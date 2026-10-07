package annina.sparkstrength.mixin.coroner;

import annina.sparkstrength.role.coroner.CoronerService;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.professor.IronManPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 验尸官伪装教授时同步铁人药剂状态，用于保持原教授蓝色透视效果。
 */
@Mixin(IronManPlayerComponent.class)
public abstract class CoronerIronManComponentSyncMixin {
    @Inject(method = "shouldSyncWith", at = @At("HEAD"), cancellable = true)
    private void sparkstrength$syncIronManToCoronerProfessor(
            ServerPlayerEntity recipient,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (CoronerService.hasProfessorDisguise(recipient)) {
            cir.setReturnValue(true);
        }
    }
}
