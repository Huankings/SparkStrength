package annina.sparkstrength.client.mixin.corruptcop;

import annina.sparkstrength.client.role.corruptcop.CorruptCopClientHooks;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.doctor4t.wathe.client.WatheClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Composes Corrupt Cop into Wathe's version-pinned lightmap gate without granting killer authority.
 * 将黑警组合进 Wathe 版本锁定的亮度入口，但不授予杀手权限。
 */
@Mixin(value = WatheClient.class, remap = false)
public abstract class WatheClientInstinctLightMixin {
    @WrapOperation(
            method = "lambda$onInitializeClient$15",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/doctor4t/wathe/client/WatheClient;isInstinctEnabledAndIsKiller()Z"
            )
    )
    private static boolean sparkstrength$allowCorruptCopInstinctLight(Operation<Boolean> original) {
        boolean originalResult = original.call();
        return originalResult || CorruptCopClientHooks.usesKillerStyleInstinctLight();
    }
}
