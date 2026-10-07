package annina.sparkstrength.role.morphling;

import annina.sparkstrength.SparkStrengthItems;
import annina.sparkstrength.component.morphling.MorphBodyDisguiseWorldComponent;
import annina.sparkstrength.component.morphling.MorphMarkPlayerComponent;
import annina.sparkstrength.replay.SparkStrengthReplayFormatters;
import com.mojang.authlib.GameProfile;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.event.ShouldPunishGunShooter;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.agmas.noellesroles.morphling.MorphlingPlayerComponent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 变形怪增强的服务端主逻辑。
 *
 * <p>物品右键、商店、开局发放、死亡奖励、枪惩罚豁免和语音插件都只调用这里的公共方法。
 * 这样所有职业校验和“只奖励一份”的边界条件集中维护，避免后续改某个入口时漏掉另一条入口。</p>
 */
public final class MorphlingService {
    private static final String ROOT_KEY = "SparkStrengthMorphReagent";
    private static final String SAMPLE_UUID_KEY = "SampleUuid";
    private static final String SAMPLE_NAME_KEY = "SampleName";
    private static final Set<UUID> WAITING_FOR_REAGENT_RELEASE = new HashSet<>();
    private static boolean registered;

    private MorphlingService() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        ShouldPunishGunShooter.EVENT.register(MorphlingService::shouldPunishGunShooter);
    }

    public static void assignForRole(ServerPlayerEntity player, Role role) {
        if (!MorphlingRules.isMorphling(role) || hasMorphDevice(player)) {
            return;
        }
        player.giveItemStack(new ItemStack(SparkStrengthItems.morphDevice()));
    }

    public static void reset(ServerPlayerEntity player) {
        MorphMarkPlayerComponent.KEY.get(player).clear();
        clearReagentReleaseGate(player);
    }

    public static void clearReagentReleaseGate(ServerPlayerEntity player) {
        WAITING_FOR_REAGENT_RELEASE.remove(player.getUuid());
    }

    public static TypedActionResult<ItemStack> useReagent(
            ServerPlayerEntity morphling,
            ItemStack stack,
            @Nullable Entity explicitTarget
    ) {
        if (!canUseMorphlingItem(morphling)) {
            return TypedActionResult.fail(stack);
        }

        if (WAITING_FOR_REAGENT_RELEASE.contains(morphling.getUuid())) {
            /*
             * 采样成功后，同一次右键按住期间客户端/服务端都可能继续收到 use/useOnEntity。
             * 这些包不能进入“已有采样 -> 标记”的下一阶段，必须等 onStoppedUsing 证明玩家松开鼠标后才放行。
             */
            return TypedActionResult.success(stack);
        }

        Optional<SampleData> sample = getSample(stack);
        if (sample.isEmpty()) {
            SampleData sampled = sampleTarget(morphling, explicitTarget);
            setSample(stack, sampled);
            WAITING_FOR_REAGENT_RELEASE.add(morphling.getUuid());
            sendMorphlingMessage(morphling, "message.sparkstrength.morphling.sampled", sampled.name());
            recordSample(morphling, sampled);
            return TypedActionResult.success(stack);
        }

        ServerPlayerEntity markTarget = findMarkTargetOrSelf(morphling, explicitTarget);
        if (sample.get().uuid().equals(markTarget.getUuid())) {
            sendMorphlingMessage(morphling, "message.sparkstrength.morphling.same_target");
            return TypedActionResult.fail(stack);
        }

        MorphMarkPlayerComponent.KEY.get(markTarget).setPending(
                morphling,
                sample.get().uuid(),
                sample.get().name(),
                markTarget.getGameProfile().getName()
        );
        sendMorphlingMessage(morphling, "message.sparkstrength.morphling.marked", markTarget.getGameProfile().getName());
        recordMark(morphling, markTarget, sample.get());

        if (!morphling.isCreative()) {
            stack.decrement(1);
        }
        return TypedActionResult.success(stack);
    }

    public static TypedActionResult<ItemStack> useDevice(ServerPlayerEntity morphling, ItemStack stack) {
        if (!canUseMorphlingItem(morphling)) {
            return TypedActionResult.fail(stack);
        }

        int activated = 0;
        for (ServerPlayerEntity target : morphling.getServer().getPlayerManager().getPlayerList()) {
            MorphMarkPlayerComponent component = MorphMarkPlayerComponent.KEY.get(target);
            if (!component.isPending() || !component.isMarkedBy(morphling.getUuid())) {
                continue;
            }
            if (!GameFunctions.isPlayerPlayingAndAlive(target)
                    || !GameFunctions.isPlayerAliveAndSurvival(target)
                    || target.getUuid().equals(component.sampleUuid())) {
                component.clear();
                continue;
            }
            if (component.activate()) {
                activated++;
                recordTrigger(target, component);
            }
        }

        if (activated <= 0) {
            sendMorphlingMessage(morphling, "message.sparkstrength.morphling.no_marks");
            return TypedActionResult.fail(stack);
        }

        sendMorphlingMessage(morphling, "message.sparkstrength.morphling.triggered");
        return TypedActionResult.success(stack);
    }

    public static void afterKill(
            ServerPlayerEntity victim,
            @Nullable ServerPlayerEntity killer,
            Identifier deathReason
    ) {
        recordBodyDisguiseIfNeeded(victim);
        rewardSelfMorphKill(victim, killer);
        rewardMarkedPlayerEvent(victim, killer);
        MorphMarkPlayerComponent.KEY.get(victim).clear();
    }

    public static List<ServerPlayerEntity> findActivePlayersDisguisedAs(ServerPlayerEntity samplePlayer) {
        List<ServerPlayerEntity> result = new ArrayList<>();
        if (!GameFunctions.isPlayerPlayingAndAlive(samplePlayer)
                || !GameFunctions.isPlayerAliveAndSurvival(samplePlayer)) {
            /*
             * 语音伪装只允许“仍然存活的采样玩家”驱动。
             * 如果采样来源是尸体或采样玩家之后死亡，旁观/死亡玩家说话不能再借活着的伪装者传进局内，
             * 否则会绕过局内信息隔离，把死亡玩家知道的内容泄露给存活玩家。
             */
            return result;
        }
        MinecraftServer server = samplePlayer.getServer();
        for (ServerPlayerEntity possibleDisguised : server.getPlayerManager().getPlayerList()) {
            MorphMarkPlayerComponent component = MorphMarkPlayerComponent.KEY.get(possibleDisguised);
            if (component.isActive()
                    && samplePlayer.getUuid().equals(component.sampleUuid())
                    && GameFunctions.isPlayerPlayingAndAlive(possibleDisguised)
                    && GameFunctions.isPlayerAliveAndSurvival(possibleDisguised)) {
                result.add(possibleDisguised);
            }
        }
        return result;
    }

    public static boolean hasSample(ItemStack stack) {
        return getSample(stack).isPresent();
    }

    public static Optional<UUID> sampleUuid(ItemStack stack) {
        return getSample(stack).map(SampleData::uuid);
    }

    public static String sampleNameForTooltip(ItemStack stack) {
        return getSample(stack).map(SampleData::name).orElse("");
    }

    private static boolean canUseMorphlingItem(ServerPlayerEntity player) {
        GameWorldComponent game = GameWorldComponent.KEY.get(player.getWorld());
        return MorphlingRules.isMorphling(game.getRole(player))
                && GameFunctions.isPlayerPlayingAndAlive(player)
                && GameFunctions.isPlayerAliveAndSurvival(player);
    }

    private static boolean hasMorphDevice(ServerPlayerEntity player) {
        for (int slot = 0; slot < player.getInventory().size(); slot++) {
            if (player.getInventory().getStack(slot).isOf(SparkStrengthItems.morphDevice())) {
                return true;
            }
        }
        return false;
    }

    private static SampleData sampleTarget(ServerPlayerEntity morphling, @Nullable Entity explicitTarget) {
        Entity target = explicitTarget != null ? explicitTarget : findLookedAtSampleTarget(morphling);
        if (target instanceof ServerPlayerEntity playerTarget && isValidLivingTarget(morphling, playerTarget)) {
            return new SampleData(playerTarget.getUuid(), playerTarget.getGameProfile().getName());
        }
        if (target instanceof PlayerBodyEntity body) {
            UUID bodyOwner = body.getPlayerUuid();
            return new SampleData(bodyOwner, resolvePlayerName(morphling.getServer(), bodyOwner));
        }
        return new SampleData(morphling.getUuid(), morphling.getGameProfile().getName());
    }

    private static ServerPlayerEntity findMarkTargetOrSelf(ServerPlayerEntity morphling, @Nullable Entity explicitTarget) {
        Entity target = explicitTarget != null ? explicitTarget : findLookedAtMarkTarget(morphling);
        if (target instanceof ServerPlayerEntity playerTarget && isValidLivingTarget(morphling, playerTarget)) {
            return playerTarget;
        }
        return morphling;
    }

    private static @Nullable Entity findLookedAtSampleTarget(ServerPlayerEntity morphling) {
        HitResult hitResult = ProjectileUtil.getCollision(
                morphling,
                entity -> (entity instanceof ServerPlayerEntity target && isValidLivingTarget(morphling, target))
                        || entity instanceof PlayerBodyEntity,
                MorphlingRules.REAGENT_TARGET_RANGE
        );
        return hitResult instanceof EntityHitResult entityHitResult ? entityHitResult.getEntity() : null;
    }

    private static @Nullable Entity findLookedAtMarkTarget(ServerPlayerEntity morphling) {
        HitResult hitResult = ProjectileUtil.getCollision(
                morphling,
                entity -> entity instanceof ServerPlayerEntity target && isValidLivingTarget(morphling, target),
                MorphlingRules.REAGENT_TARGET_RANGE
        );
        return hitResult instanceof EntityHitResult entityHitResult ? entityHitResult.getEntity() : null;
    }

    private static boolean isValidLivingTarget(ServerPlayerEntity morphling, ServerPlayerEntity target) {
        return target != morphling
                && GameFunctions.isPlayerPlayingAndAlive(target)
                && GameFunctions.isPlayerAliveAndSurvival(target);
    }

    private static void rewardSelfMorphKill(ServerPlayerEntity victim, @Nullable ServerPlayerEntity killer) {
        if (killer == null || killer.getUuid().equals(victim.getUuid())) {
            return;
        }

        GameWorldComponent game = GameWorldComponent.KEY.get(killer.getWorld());
        if (!MorphlingRules.isMorphling(game.getRole(killer))) {
            return;
        }

        UUID currentDisguise = currentSelfDisguise(killer);
        if (currentDisguise == null) {
            return;
        }

        int reward = victim.getUuid().equals(currentDisguise)
                ? MorphlingRules.SELF_MORPH_TARGET_KILL_REWARD
                : MorphlingRules.SELF_MORPH_KILL_REWARD;
        PlayerShopComponent.KEY.get(killer).addToBalance(reward);
    }

    private static @Nullable UUID currentSelfDisguise(ServerPlayerEntity morphling) {
        MorphlingPlayerComponent originalMorph = MorphlingPlayerComponent.KEY.get(morphling);
        if (originalMorph.getMorphTicks() > 0 && originalMorph.disguise != null) {
            return originalMorph.disguise;
        }

        MorphMarkPlayerComponent reagentMorph = MorphMarkPlayerComponent.KEY.get(morphling);
        if (reagentMorph.isActive()) {
            return reagentMorph.sampleUuid();
        }
        return null;
    }

    private static void rewardMarkedPlayerEvent(ServerPlayerEntity victim, @Nullable ServerPlayerEntity killer) {
        if (killer != null && !killer.getUuid().equals(victim.getUuid())) {
            rewardMarkerForActivePlayer(killer);
        }
        rewardMarkerForActivePlayer(victim);
    }

    private static void rewardMarkerForActivePlayer(ServerPlayerEntity activePlayer) {
        MorphMarkPlayerComponent component = MorphMarkPlayerComponent.KEY.get(activePlayer);
        UUID markerUuid = component.markerUuid();
        if (!component.isActive() || markerUuid == null || markerUuid.equals(activePlayer.getUuid())) {
            // 试剂作用在 Morphling 自己身上时，只走“自我变形击杀奖励”，不再触发 +50 的旁路奖励。
            return;
        }

        ServerPlayerEntity marker = activePlayer.getServer().getPlayerManager().getPlayer(markerUuid);
        if (marker != null && GameFunctions.isPlayerPlayingAndAlive(marker)) {
            PlayerShopComponent.KEY.get(marker).addToBalance(MorphlingRules.OTHER_MARK_EVENT_REWARD);
        }
    }

    private static void recordBodyDisguiseIfNeeded(ServerPlayerEntity victim) {
        MorphMarkPlayerComponent component = MorphMarkPlayerComponent.KEY.get(victim);
        if (!component.isActive() || component.sampleUuid() == null) {
            return;
        }
        MorphBodyDisguiseWorldComponent.KEY.get(victim.getServerWorld())
                .recordBodyDisguise(victim.getUuid(), component.sampleUuid(), component.sampleName());
    }

    private static @Nullable ShouldPunishGunShooter.PunishResult shouldPunishGunShooter(
            PlayerEntity shooter,
            PlayerEntity victim
    ) {
        MorphMarkPlayerComponent component = MorphMarkPlayerComponent.KEY.get(victim);
        if (!component.isActive()) {
            return null;
        }

        GameWorldComponent game = GameWorldComponent.KEY.get(victim.getWorld());
        Role victimRole = game.getRole(victim);
        if (victimRole != null && victimRole.isInnocent()) {
            return ShouldPunishGunShooter.PunishResult.cancel();
        }
        return null;
    }

    private static Optional<SampleData> getSample(ItemStack stack) {
        NbtCompound root = root(stack);
        if (!root.containsUuid(SAMPLE_UUID_KEY)) {
            return Optional.empty();
        }
        String name = root.getString(SAMPLE_NAME_KEY);
        if (name == null || name.isBlank()) {
            name = root.getUuid(SAMPLE_UUID_KEY).toString().substring(0, 8);
        }
        return Optional.of(new SampleData(root.getUuid(SAMPLE_UUID_KEY), name));
    }

    private static void setSample(ItemStack stack, SampleData sample) {
        NbtCompound root = new NbtCompound();
        root.putUuid(SAMPLE_UUID_KEY, sample.uuid());
        root.putString(SAMPLE_NAME_KEY, sample.name());

        NbtCompound customData = new NbtCompound();
        customData.put(ROOT_KEY, root);
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(customData));
    }

    private static NbtCompound root(ItemStack stack) {
        NbtComponent component = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        NbtCompound data = component.copyNbt();
        return data.contains(ROOT_KEY, NbtElement.COMPOUND_TYPE) ? data.getCompound(ROOT_KEY) : new NbtCompound();
    }

    private static void sendMorphlingMessage(ServerPlayerEntity morphling, String key, Object... args) {
        Role role = GameWorldComponent.KEY.get(morphling.getWorld()).getRole(morphling);
        int color = role != null ? role.color() : 0xAA023D;
        morphling.sendMessage(Text.translatable(key, args).withColor(color), true);
    }

    private static String resolvePlayerName(MinecraftServer server, UUID uuid) {
        ServerPlayerEntity online = server.getPlayerManager().getPlayer(uuid);
        if (online != null) {
            return online.getGameProfile().getName();
        }
        return server.getUserCache()
                .getByUuid(uuid)
                .map(GameProfile::getName)
                .orElse(uuid.toString().substring(0, 8));
    }

    private static void recordSample(ServerPlayerEntity morphling, SampleData sample) {
        if (!(morphling.getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }
        NbtCompound extra = new NbtCompound();
        extra.putUuid("sample", sample.uuid());
        extra.putString("sample_name", sample.name());
        GameRecordManager.recordGlobalEvent(
                serverWorld,
                SparkStrengthReplayFormatters.MORPH_REAGENT_SAMPLED,
                morphling,
                extra
        );
    }

    private static void recordMark(ServerPlayerEntity morphling, ServerPlayerEntity target, SampleData sample) {
        if (!(morphling.getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }
        NbtCompound extra = new NbtCompound();
        extra.putUuid("sample", sample.uuid());
        extra.putString("sample_name", sample.name());
        extra.putUuid("target", target.getUuid());
        GameRecordManager.recordGlobalEvent(
                serverWorld,
                SparkStrengthReplayFormatters.MORPH_REAGENT_MARKED,
                morphling,
                extra
        );
    }

    private static void recordTrigger(ServerPlayerEntity target, MorphMarkPlayerComponent component) {
        if (!(target.getWorld() instanceof ServerWorld serverWorld) || component.sampleUuid() == null) {
            return;
        }
        NbtCompound extra = new NbtCompound();
        extra.putUuid("sample", component.sampleUuid());
        extra.putString("sample_name", component.sampleName());
        GameRecordManager.recordGlobalEvent(
                serverWorld,
                SparkStrengthReplayFormatters.MORPH_MARK_TRIGGERED,
                target,
                extra
        );
    }

    public record SampleData(UUID uuid, String name) {
    }
}
