package annina.sparkstrength.client.mixin.coroner;

import annina.sparkstrength.role.coroner.CoronerService;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.client.gui.TimeRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 验尸官伪装为计时员时，复用 Wathe 原本的游戏时间 HUD。
 *
 * <p>Wathe 的时间显示入口只检查真实职业的 {@link Role#canSeeTime()}。
 * 验尸官真实职业没有这个权限，所以这里仅在客户端显示判断处补一层：
 * 当前本地玩家处于“计时员尸体身份”伪装时，把可见时间视为 true。
 * 这样不会复制倒计时渲染逻辑，也能继续保留 Wathe 原本的颜色、滚动数字和旁观者显示规则。</p>
 */
@Mixin(value = TimeRenderer.class, remap = false)
public abstract class CoronerTimeRendererMixin {
    @WrapOperation(
            method = "renderHud",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/doctor4t/wathe/api/Role;canSeeTime()Z"
            )
    )
    private static boolean sparkstrength$allowCoronerTimekeeperDisguise(
            Role role,
            Operation<Boolean> original
    ) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null && CoronerService.hasTimekeeperDisguise(player)) {
            return true;
        }
        return original.call(role);
    }
}
