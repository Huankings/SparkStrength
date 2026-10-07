package annina.sparkstrength.role.phantom;

import annina.sparkstrength.component.phantom.PhantomBackpackTargetComponent;
import annina.sparkstrength.component.phantom.PhantomBackpackUserComponent;
import annina.sparkstrength.replay.SparkStrengthReplayFormatters;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 幽灵背包隐身的服务端权威逻辑。
 *
 * <p>这条能力不修改 NoellesRoles 原幽灵按键逻辑；它只在 SparkStrength 内部维护
 * 独立冷却和目标隐身追踪，因此原本按 G 给自己隐身的 90 秒冷却不会被背包按钮影响。</p>
 */
public final class PhantomBackpackService {
    private PhantomBackpackService() {
    }

    public static void assignForRole(ServerPlayerEntity player, Role role) {
        if (!PhantomBackpackRules.isPhantom(role)) {
            PhantomBackpackUserComponent.KEY.get(player).reset();
        }
        PhantomBackpackTargetComponent.KEY.get(player).reset();
    }

    public static void clearPlayer(ServerPlayerEntity player) {
        PhantomBackpackUserComponent.KEY.get(player).reset();
        PhantomBackpackTargetComponent.KEY.get(player).reset();
    }

    public static void tryUseBackpackInvisibility(ServerPlayerEntity phantom, UUID targetUuid) {
        if (phantom == null || targetUuid == null) {
            return;
        }

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(phantom.getWorld());
        if (!gameWorld.isRole(phantom, Noellesroles.PHANTOM) || !canUseBackpackAbility(phantom)) {
            return;
        }

        PhantomBackpackUserComponent userComponent = PhantomBackpackUserComponent.KEY.get(phantom);
        if (userComponent.isOnCooldown()) {
            return;
        }

        // 只要合法幽灵点击了背包按钮，就先进入冷却，避免用非法 UUID 试探目标状态。
        userComponent.setCooldownTicks(PhantomBackpackRules.COOLDOWN_TICKS);

        ServerPlayerEntity target = phantom.getServer().getPlayerManager().getPlayer(targetUuid);
        if (!isValidTarget(phantom, target)) {
            return;
        }

        target.addStatusEffect(new StatusEffectInstance(
                StatusEffects.INVISIBILITY,
                PhantomBackpackRules.INVISIBILITY_DURATION_TICKS,
                0,
                true,
                false,
                true
        ));
        PhantomBackpackTargetComponent.KEY.get(target).startInvisibility();
        recordInvisibilityStarted(phantom, target);
    }

    private static boolean canUseBackpackAbility(ServerPlayerEntity player) {
        return GameFunctions.isPlayerPlayingAndAlive(player)
                && GameFunctions.isPlayerAliveAndSurvival(player)
                && !SwallowedPlayerComponent.isPlayerSwallowed(player);
    }

    private static boolean isValidTarget(ServerPlayerEntity user, @Nullable ServerPlayerEntity target) {
        return target != null
                && target.getServerWorld() == user.getServerWorld()
                && !target.getUuid().equals(user.getUuid())
                && GameFunctions.isPlayerPlayingAndAlive(target)
                && GameFunctions.isPlayerAliveAndSurvival(target)
                && !SwallowedPlayerComponent.isPlayerSwallowed(target);
    }

    private static void recordInvisibilityStarted(ServerPlayerEntity phantom, ServerPlayerEntity target) {
        if (!(phantom.getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }

        NbtCompound extra = new NbtCompound();
        extra.putUuid("target", target.getUuid());
        GameRecordManager.recordGlobalEvent(
                serverWorld,
                SparkStrengthReplayFormatters.PHANTOM_BACKPACK_INVISIBILITY_STARTED,
                phantom,
                extra
        );
    }
}
