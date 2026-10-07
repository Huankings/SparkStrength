package annina.sparkstrength.mixin.noellesroles;

import annina.sparkstrength.role.shadowjester.ShadowJesterShowdownService;
import org.agmas.noellesroles.Noellesroles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 在 NoellesRoles 注册自身胜利检查之前，先注册 SparkStrength 的新版影子小丑谢幕检查。
 *
 * <p>旧实现直接注入编译器生成的 {@code lambda$registerEvents$19}。
 * 该方法编号和内部调用顺序会随着 NoellesRoles 构建变化，容易造成启动时
 * {@code InvalidInjectionException}。现在只注入稳定的公开
 * {@code Noellesroles#registerEvents()} 方法，在其 {@code HEAD} 注册新版监听器，
 * 从而不再依赖任何 lambda 编号。</p>
 */
@Mixin(value = Noellesroles.class, remap = false)
public abstract class ShadowJesterWinPriorityMixin {
    @Inject(
            method = "registerEvents()V",
            at = @At("HEAD"),
            remap = false
    )
    private void sparkstrength$registerPriorityWinListener(
            CallbackInfo ci
    ) {
        ShadowJesterShowdownService.registerWinPriorityListener();
    }
}
