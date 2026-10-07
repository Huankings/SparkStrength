package annina.sparkstrength.role.veteran;

import annina.sparkstrength.network.veteran.SyncVeteranBlackoutS2CPacket;
import annina.sparkstrength.role.coroner.CoronerService;
import dev.doctor4t.wathe.api.event.BlackoutEffect;
import dev.doctor4t.wathe.cca.WorldBlackoutComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 把 Wathe 服务端停电状态同步给客户端老兵高亮。
 *
 * <p>同步策略很轻：状态变化时立即发包；停电期间每秒补发一次 true，
 * 让中途重连或刚切入世界的客户端也能尽快拿到正确状态。</p>
 */
public final class VeteranBlackoutService {
    private static final int ACTIVE_RESYNC_INTERVAL_TICKS = 20;
    private static final int CORONER_BLACKOUT_NIGHT_VISION_REFRESH_TICKS = 40;
    private static final int BLACKOUT_EFFECT_DURATION_TOLERANCE_TICKS = 5;
    private static final Map<RegistryKey<World>, Boolean> LAST_BLACKOUT_STATE = new HashMap<>();
    private static final Map<UUID, RememberedCoronerVeteranNightVision> CORONER_VETERAN_NIGHT_VISION = new HashMap<>();
    private static boolean registered;

    private VeteranBlackoutService() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        BlackoutEffect.BEFORE.register(VeteranBlackoutService::beforeBlackoutEffect);
    }

    public static void tick(ServerWorld world) {
        boolean active = WorldBlackoutComponent.KEY.get(world).isBlackoutActive();
        RegistryKey<World> worldKey = world.getRegistryKey();
        boolean previous = LAST_BLACKOUT_STATE.getOrDefault(worldKey, false);
        boolean changed = previous != active;
        LAST_BLACKOUT_STATE.put(worldKey, active);

        if (active) {
            refreshCoronerVeteranBlackoutSight(world);
        } else {
            clearCoronerVeteranBlackoutSight(world);
        }

        if (changed || (active && world.getTime() % ACTIVE_RESYNC_INTERVAL_TICKS == 0)) {
            syncToWorld(world, active);
        }
    }

    public static void clear(ServerWorld world) {
        LAST_BLACKOUT_STATE.put(world.getRegistryKey(), false);
        clearCoronerVeteranBlackoutSight(world);
        syncToWorld(world, false);
    }

    private static BlackoutEffect.BlackoutResult beforeBlackoutEffect(ServerPlayerEntity player, int durationTicks) {
        if (!shouldGiveCoronerVeteranBlackoutSight(player)) {
            return null;
        }

        /*
         * Wathe 只会把真实杀手和真实老兵放进夜视分支；验尸官真实职业仍会走“即将失明”事件。
         * 当验尸官当前借用老兵尸体身份时，这里取消本次 blackout 失明并补发同款夜视，
         * 让他在停电期间表现得像真正的老兵。切走伪装后，下面记录的夜视会被 tick 清掉。
         */
        removeCurrentBlackoutBlindness(player, durationTicks);
        applyCoronerVeteranNightVision(player, durationTicks);
        return BlackoutEffect.BlackoutResult.cancel();
    }

    private static void refreshCoronerVeteranBlackoutSight(ServerWorld world) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (shouldGiveCoronerVeteranBlackoutSight(player)) {
                /*
                 * 兜底刷新：如果金酒、手电筒等更早的监听器先取消了失明，Wathe 不会继续调用这里的
                 * BlackoutEffect 监听器；因此在世界 tick 里补一个短夜视，保证老兵伪装仍能看清停电。
                 * 已经由 beforeBlackoutEffect 发过更长停电夜视时，apply 方法会保留那份更精确的时长记录。
                 */
                applyCoronerVeteranNightVision(player, CORONER_BLACKOUT_NIGHT_VISION_REFRESH_TICKS);
            } else {
                clearRememberedCoronerVeteranNightVision(player);
            }
        }
    }

    private static boolean shouldGiveCoronerVeteranBlackoutSight(ServerPlayerEntity player) {
        return GameFunctions.isPlayerPlayingAndAlive(player)
                && !GameFunctions.isPlayerSpectatingOrCreative(player)
                && CoronerService.hasVeteranDisguise(player);
    }

    private static void applyCoronerVeteranNightVision(ServerPlayerEntity player, int durationTicks) {
        StatusEffectInstance currentNightVision = player.getStatusEffect(StatusEffects.NIGHT_VISION);
        if (currentNightVision != null
                && currentNightVision.getDuration() > durationTicks + BLACKOUT_EFFECT_DURATION_TOLERANCE_TICKS) {
            /*
             * 玩家可能有金酒、鲛人等更长的夜视来源；短时兜底刷新不应该把那类效果认成验尸官停电夜视，
             * 否则切走伪装时可能误删其它职业或物品给的效果。
             */
            return;
        }

        player.addStatusEffect(new StatusEffectInstance(
                StatusEffects.NIGHT_VISION,
                durationTicks,
                0,
                false,
                false,
                true
        ));
        CORONER_VETERAN_NIGHT_VISION.put(
                player.getUuid(),
                new RememberedCoronerVeteranNightVision(durationTicks, player.getServerWorld().getTime())
        );
    }

    private static void removeCurrentBlackoutBlindness(ServerPlayerEntity player, int durationTicks) {
        StatusEffectInstance blindness = player.getStatusEffect(StatusEffects.BLINDNESS);
        if (blindness != null && matchesDuration(blindness.getDuration(), durationTicks)) {
            player.removeStatusEffect(StatusEffects.BLINDNESS);
        }
    }

    private static void clearCoronerVeteranBlackoutSight(ServerWorld world) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            clearRememberedCoronerVeteranNightVision(player);
        }
    }

    private static void clearRememberedCoronerVeteranNightVision(ServerPlayerEntity player) {
        RememberedCoronerVeteranNightVision remembered = CORONER_VETERAN_NIGHT_VISION.remove(player.getUuid());
        if (remembered == null) {
            return;
        }

        StatusEffectInstance nightVision = player.getStatusEffect(StatusEffects.NIGHT_VISION);
        if (nightVision != null && matchesRememberedNightVision(nightVision.getDuration(), remembered, player.getServerWorld().getTime())) {
            player.removeStatusEffect(StatusEffects.NIGHT_VISION);
        }
    }

    private static boolean matchesRememberedNightVision(
            int currentDurationTicks,
            RememberedCoronerVeteranNightVision remembered,
            long worldTime
    ) {
        long elapsedTicks = Math.max(0, worldTime - remembered.worldTime());
        int expectedDurationTicks = Math.max(0, remembered.durationTicks() - (int) Math.min(Integer.MAX_VALUE, elapsedTicks));
        return matchesDuration(currentDurationTicks, expectedDurationTicks);
    }

    private static boolean matchesDuration(int currentDurationTicks, int expectedDurationTicks) {
        return Math.abs(currentDurationTicks - expectedDurationTicks) <= BLACKOUT_EFFECT_DURATION_TOLERANCE_TICKS;
    }

    private static void syncToWorld(ServerWorld world, boolean active) {
        SyncVeteranBlackoutS2CPacket packet = new SyncVeteranBlackoutS2CPacket(active);
        for (ServerPlayerEntity player : world.getPlayers()) {
            ServerPlayNetworking.send(player, packet);
        }
    }

    private record RememberedCoronerVeteranNightVision(int durationTicks, long worldTime) {
    }
}
