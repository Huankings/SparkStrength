package annina.sparkstrength.mixin.coroner;

import annina.sparkstrength.role.coroner.CoronerService;
import dev.doctor4t.wathe.cca.PlayerPoisonComponent;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 验尸官伪装毒理学家/毒师时，同步中毒组件，客户端才能沿用原本中毒透视颜色。
 */
@Mixin(PlayerPoisonComponent.class)
public abstract class CoronerPoisonComponentSyncMixin {
    @Inject(method = "shouldSyncWith", at = @At("HEAD"), cancellable = true)
    private void sparkstrength$syncPoisonStateToCoronerPoisonDisguises(
            ServerPlayerEntity recipient,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (CoronerService.hasToxicologistDisguise(recipient) || CoronerService.hasPoisonerDisguise(recipient)) {
            cir.setReturnValue(true);
        }
    }
}
