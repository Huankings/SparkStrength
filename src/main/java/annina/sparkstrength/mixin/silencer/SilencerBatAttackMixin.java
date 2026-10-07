package annina.sparkstrength.mixin.silencer;

import annina.sparkstrength.role.silencer.SilencerQuietService;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * 静语者使用 Wathe 球棒击杀时不播放 bat hit 音效。
 *
 * <p>Wathe 自己通过 {@code @WrapMethod(PlayerEntity#attack)} 实现球棒秒杀和声音。
 * 这里用更高优先级提前处理静语者/验尸官静语者伪装的球棒命中，只删除声音，
 * 其它角色仍交回 Wathe 原逻辑。</p>
 */
@Mixin(value = PlayerEntity.class, priority = 1200)
public abstract class SilencerBatAttackMixin {
    @Shadow public abstract float getAttackCooldownProgress(float baseTime);

    @Shadow public abstract void resetLastAttackedTicks();

    @WrapMethod(method = "attack")
    private void sparkstrength$silencerQuietBatHit(Entity target, Operation<Void> original) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        if (SilencerQuietService.shouldSilenceWatheBat(self)
                && self instanceof ServerPlayerEntity serverPlayer
                && target instanceof ServerPlayerEntity playerTarget
                && getAttackCooldownProgress(0.5F) >= 1.0F) {
            GameFunctions.killPlayer(playerTarget, true, serverPlayer, GameConstants.DeathReasons.BAT);
            resetLastAttackedTicks();
            return;
        }

        original.call(target);
    }
}
