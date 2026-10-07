package annina.sparkstrength.mixin.veteran;

import annina.sparkstrength.role.veteran.VeteranKnifeService;
import dev.doctor4t.wathe.util.KnifeStabPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 在 Wathe 服务端刀人包入口接管老兵。
 *
 * <p>老兵需求和原版刀人流程冲突较多：原版会扣 {@code PlayerVeteranComponent} 的 0/1/2 次、
 * 次数归零时删刀、播放刺杀音效并设置刀 CD。这里在 HEAD 分流，只有老兵会被取消原逻辑；
 * 其他角色仍然完整走 Wathe/NoellesRoles 的原有流程。</p>
 */
@Mixin(KnifeStabPayload.Receiver.class)
public abstract class VeteranKnifeStabPayloadMixin {
    @Inject(method = "receive", at = @At("HEAD"), cancellable = true)
    private void sparkstrength$veteranKnifeStab(
            KnifeStabPayload payload,
            ServerPlayNetworking.Context context,
            CallbackInfo ci
    ) {
        if (VeteranKnifeService.handleKnifeStab(payload, context)) {
            ci.cancel();
        }
    }
}
