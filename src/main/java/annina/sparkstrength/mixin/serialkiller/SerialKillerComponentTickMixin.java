package annina.sparkstrength.mixin.serialkiller;

import annina.sparkstrength.role.serialkiller.SerialKillerCooldownService;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.serialkiller.SerialKillerPlayerComponent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 在 NoellesRoles 连环杀手组件写入刀 CD 后再次执行 SparkStrength 清零。 */
@Mixin(value = SerialKillerPlayerComponent.class, remap = false)
public abstract class SerialKillerComponentTickMixin {
    @Shadow @Final private PlayerEntity player;

    // RETURN 会覆盖 serverTick 内部的提前 return，确保延迟刀 CD 写入后仍能清理。
    @Inject(method = "serverTick", at = @At("RETURN"), remap = false)
    private void sparkstrength$clearCooldownAfterNoellesTick(CallbackInfo ci) {
        SerialKillerCooldownService.applyComponentTickReset(player);
    }
}
