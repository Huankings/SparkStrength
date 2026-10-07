package annina.sparkstrength.client.mixin.engineer;

import annina.sparkstrength.component.engineer.EngineerStunnedPlayerComponent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 定身期间禁用所有按键状态。
 *
 * <p>移动、跳跃、蹲下、攻击/使用对应的 key binding，以及打开背包等界面入口，
 * 最终都会查询 {@link KeyBinding#isPressed()} 或 {@link KeyBinding#wasPressed()}。</p>
 */
@Mixin(KeyBinding.class)
public abstract class EngineerStunnedKeyBindingMixin {
    @Inject(method = "wasPressed", at = @At("HEAD"), cancellable = true)
    private void sparkstrength$blockWasPressedWhenStunned(CallbackInfoReturnable<Boolean> cir) {
        if (isLocalPlayerStunned()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isPressed", at = @At("HEAD"), cancellable = true)
    private void sparkstrength$blockIsPressedWhenStunned(CallbackInfoReturnable<Boolean> cir) {
        if (isLocalPlayerStunned()) {
            cir.setReturnValue(false);
        }
    }

    private static boolean isLocalPlayerStunned() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.player != null && EngineerStunnedPlayerComponent.KEY.get(client.player).isStunned();
    }
}
