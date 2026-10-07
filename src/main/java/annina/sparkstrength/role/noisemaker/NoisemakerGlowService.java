package annina.sparkstrength.role.noisemaker;

import annina.sparkstrength.component.noisemaker.NoisemakerGlowTargetComponent;
import annina.sparkstrength.component.noisemaker.NoisemakerGlowUserComponent;
import annina.sparkstrength.replay.SparkStrengthReplayFormatters;
import annina.sparkstrength.role.coroner.CoronerService;
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
 * 大嗓门点亮能力的服务端逻辑。
 *
 * <p>所有关键判定都集中在这里，网络包、击杀事件和组件 tick 不各写一套规则，
 * 后续调数值或调整判定时更不容易漏改。</p>
 */
public final class NoisemakerGlowService {
    private NoisemakerGlowService() {
    }

    /**
     * 背包头像按钮点亮目标。
     *
     * <p>顺序特意设计为：先确认使用者本人合法且冷却可用，然后立刻消耗 80 秒冷却，
     * 再检查目标是否仍然存活。这样点击死亡/旁观/离线玩家也会进入冷却，
     * 防止大嗓门通过“点头像是否吃冷却”反向判断目标是否存活。
     * 回放只会在目标确实存活并被点亮时记录。</p>
     */
    public static void tryUseBackpackGlow(ServerPlayerEntity user, UUID targetUuid) {
        if (targetUuid == null) {
            return;
        }

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(user.getWorld());
        if (!gameWorld.isRole(user, Noellesroles.NOISEMAKER)) {
            return;
        }
        if (!GameFunctions.isPlayerPlayingAndAlive(user) || SwallowedPlayerComponent.isPlayerSwallowed(user)) {
            return;
        }

        NoisemakerGlowUserComponent userComponent = NoisemakerGlowUserComponent.KEY.get(user);
        if (userComponent.isOnCooldown()) {
            return;
        }

        // 关键惩罚点：只要请求来自合法且可用的大嗓门，就先消耗冷却。
        userComponent.setCooldownTicks(NoisemakerGlowConstants.COOLDOWN_TICKS);

        ServerPlayerEntity target = user.getServer().getPlayerManager().getPlayer(targetUuid);
        if (!isValidGlowTarget(target)) {
            return;
        }

        target.addStatusEffect(new StatusEffectInstance(
                StatusEffects.GLOWING,
                NoisemakerGlowConstants.GLOW_DURATION_TICKS,
                0,
                false,
                false,
                true
        ));
        NoisemakerGlowTargetComponent.KEY.get(target).startGlow();

        if (user.getWorld() instanceof ServerWorld serverWorld) {
            NbtCompound extra = new NbtCompound();
            extra.putUuid("target", target.getUuid());
            GameRecordManager.recordGlobalEvent(
                    serverWorld,
                    SparkStrengthReplayFormatters.NOISEMAKER_GLOW_STARTED,
                    user,
                    extra
            );
        }
    }

    /**
     * 大嗓门被击杀时，让直接杀死他的玩家发光 15 秒。
     *
     * <p>这是单纯的被动反制效果，按照你的确认不写入回放，也不进入目标组件，
     * 避免后续出现“没有开始事件却有结束事件”的回放词条。</p>
     */
    public static void glowKillerWhenNoisemakerDies(ServerPlayerEntity victim, @Nullable ServerPlayerEntity killer) {
        if (killer == null || killer == victim) {
            return;
        }

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(victim.getWorld());
        if (!gameWorld.isRole(victim, Noellesroles.NOISEMAKER) && !CoronerService.hasNoisemakerDisguise(victim)) {
            return;
        }
        if (!GameFunctions.isPlayerPlayingAndAlive(killer)) {
            return;
        }

        killer.addStatusEffect(new StatusEffectInstance(
                StatusEffects.GLOWING,
                NoisemakerGlowConstants.KILLER_GLOW_TICKS,
                0,
                false,
                false,
                true
        ));
    }

    private static boolean isValidGlowTarget(@Nullable ServerPlayerEntity target) {
        return target != null
                && GameFunctions.isPlayerPlayingAndAlive(target)
                && GameFunctions.isPlayerAliveAndSurvival(target)
                && !SwallowedPlayerComponent.isPlayerSwallowed(target);
    }
}
