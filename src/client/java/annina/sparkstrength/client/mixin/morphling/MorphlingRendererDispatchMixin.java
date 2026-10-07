package annina.sparkstrength.client.mixin.morphling;

import annina.sparkstrength.client.role.morphling.MorphlingAppearanceClientHelper;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@SuppressWarnings("unchecked")
@Mixin(EntityRenderDispatcher.class)
public abstract class MorphlingRendererDispatchMixin {
    @Shadow
    private Map<SkinTextures.Model, EntityRenderer<? extends PlayerEntity>> modelRenderers;

    @Inject(method = "getRenderer", at = @At("HEAD"), cancellable = true)
    private <T extends Entity> void sparkstrength$selectMorphlingReagentRenderer(
            T entity,
            CallbackInfoReturnable<EntityRenderer<? super T>> cir
    ) {
        if (!(entity instanceof AbstractClientPlayerEntity player)) {
            return;
        }

        SkinTextures skinTextures = MorphlingAppearanceClientHelper.resolveActivePlayerSkinTextures(player);
        if (skinTextures == null) {
            return;
        }

        EntityRenderer<? extends PlayerEntity> renderer = modelRenderers.get(skinTextures.model());
        if (renderer != null) {
            cir.setReturnValue((EntityRenderer<? super T>) renderer);
        }
    }
}
