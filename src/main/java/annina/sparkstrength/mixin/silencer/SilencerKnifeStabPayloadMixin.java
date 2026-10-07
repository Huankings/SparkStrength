package annina.sparkstrength.mixin.silencer;

import annina.sparkstrength.role.silencer.SilencerKnifeService;
import dev.doctor4t.wathe.util.KnifeStabPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 在 Wathe 服务端刀人包入口接管静语者刺杀音效。
 */
@Mixin(KnifeStabPayload.Receiver.class)
public abstract class SilencerKnifeStabPayloadMixin {
    @Inject(method = "receive", at = @At("HEAD"), cancellable = true)
    private void sparkstrength$silencerQuietKnifeStab(
            KnifeStabPayload payload,
            ServerPlayNetworking.Context context,
            CallbackInfo ci
    ) {
        if (SilencerKnifeService.handleQuietKnifeStab(payload, context)) {
            ci.cancel();
        }
    }
}
