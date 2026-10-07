package annina.sparkstrength.mixin.coroner;

import annina.sparkstrength.role.coroner.CoronerService;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.bartender.BartenderPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 验尸官伪装酒保时同步喝酒者发光状态。
 */
@Mixin(BartenderPlayerComponent.class)
public abstract class CoronerBartenderComponentSyncMixin {
    @Inject(method = "shouldSyncWith", at = @At("HEAD"), cancellable = true)
    private void sparkstrength$syncDrinkGlowToCoronerBartender(
            ServerPlayerEntity recipient,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (CoronerService.hasBartenderDisguise(recipient)) {
            cir.setReturnValue(true);
        }
    }
}
