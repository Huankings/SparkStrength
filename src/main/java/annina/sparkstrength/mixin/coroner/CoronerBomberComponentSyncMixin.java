package annina.sparkstrength.mixin.coroner;

import annina.sparkstrength.role.coroner.CoronerService;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.bomber.BomberPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 验尸官伪装炸弹客时同步携弹状态，用于保持炸弹客本能透视。
 */
@Mixin(BomberPlayerComponent.class)
public abstract class CoronerBomberComponentSyncMixin {
    @Inject(method = "shouldSyncWith", at = @At("HEAD"), cancellable = true)
    private void sparkstrength$syncBombStateToCoronerBomber(
            ServerPlayerEntity recipient,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (CoronerService.hasBomberDisguise(recipient)) {
            cir.setReturnValue(true);
        }
    }
}
