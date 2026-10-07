package annina.sparkstrength.client.mixin.morphling;

import annina.sparkstrength.client.role.morphling.MorphlingAppearanceClientHelper;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.doctor4t.wathe.client.gui.RoleNameRenderer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(RoleNameRenderer.class)
public abstract class MorphlingRoleNameRendererMixin {
    @WrapOperation(
            method = "renderHud",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/PlayerEntity;getDisplayName()Lnet/minecraft/text/Text;"
            )
    )
    private static Text sparkstrength$useMorphlingReagentDisplayName(
            PlayerEntity player,
            Operation<Text> original
    ) {
        Text displayName = MorphlingAppearanceClientHelper.resolveActiveDisplayName(player);
        return displayName != null ? displayName : original.call(player);
    }
}
