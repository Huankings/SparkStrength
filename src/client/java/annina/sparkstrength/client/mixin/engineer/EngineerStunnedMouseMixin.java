package annina.sparkstrength.client.mixin.engineer;

import annina.sparkstrength.component.engineer.EngineerStunnedPlayerComponent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 定身期间禁用鼠标输入。
 *
 * <p>按钮和滚轮负责阻止左/右键、切换物品栏；光标和 updateMouse 拦截负责冻结视角，
 * 避免玩家被捕捉后还能靠鼠标观察周围。</p>
 */
@Mixin(Mouse.class)
public abstract class EngineerStunnedMouseMixin {
    @Inject(method = "onMouseButton", at = @At("HEAD"), cancellable = true)
    private void sparkstrength$blockMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {
        if (isLocalPlayerStunned()) {
            ci.cancel();
        }
    }

    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void sparkstrength$blockMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (isLocalPlayerStunned()) {
            ci.cancel();
        }
    }

    @Inject(method = "onCursorPos", at = @At("HEAD"), cancellable = true)
    private void sparkstrength$blockCursorMovement(long window, double x, double y, CallbackInfo ci) {
        if (isLocalPlayerStunned()) {
            ci.cancel();
        }
    }

    @Inject(method = "updateMouse", at = @At("HEAD"), cancellable = true)
    private void sparkstrength$blockMouseUpdate(double timeDelta, CallbackInfo ci) {
        if (isLocalPlayerStunned()) {
            ci.cancel();
        }
    }

    private static boolean isLocalPlayerStunned() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.player != null && EngineerStunnedPlayerComponent.KEY.get(client.player).isStunned();
    }
}
