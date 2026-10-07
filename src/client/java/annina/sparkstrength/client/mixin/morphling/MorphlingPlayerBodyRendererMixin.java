package annina.sparkstrength.client.mixin.morphling;

import annina.sparkstrength.client.role.morphling.MorphlingAppearanceClientHelper;
import dev.doctor4t.wathe.client.render.entity.PlayerBodyEntityRenderer;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 让试剂变形期间死亡生成的 PlayerBodyEntity 按采样目标显示。
 *
 * <p>服务端只记录“尸体 owner -> 伪装目标 UUID”，这里按本地观察者决定最终外观：
 * 普通存活玩家看到采样目标，存活杀手阵营玩家按住本能键时看到尸体原主。</p>
 */
@Mixin(PlayerBodyEntityRenderer.class)
public abstract class MorphlingPlayerBodyRendererMixin
        extends LivingEntityRenderer<PlayerBodyEntity, PlayerEntityModel<PlayerBodyEntity>> {
    protected MorphlingPlayerBodyRendererMixin(
            EntityRendererFactory.Context context,
            PlayerEntityModel<PlayerBodyEntity> entityModel,
            float shadowRadius
    ) {
        super(context, entityModel, shadowRadius);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void sparkstrength$cacheMorphlingBodyModels(
            EntityRendererFactory.Context context,
            boolean slim,
            CallbackInfo ci
    ) {
        MorphlingAppearanceClientHelper.initializeBodyModels(context);
    }

    @Inject(
            method = "render(Ldev/doctor4t/wathe/entity/PlayerBodyEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("HEAD")
    )
    private void sparkstrength$applyMorphlingBodyModel(
            PlayerBodyEntity body,
            float yaw,
            float tickDelta,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            CallbackInfo ci
    ) {
        SkinTextures skinTextures = MorphlingAppearanceClientHelper.resolveBodySkinTextures(body);
        PlayerEntityModel<PlayerBodyEntity> resolvedModel = MorphlingAppearanceClientHelper.getBodyModel(skinTextures);
        if (resolvedModel != null) {
            this.model = resolvedModel;
        }
    }

    @Inject(method = "getTexture", at = @At("HEAD"), cancellable = true)
    private void sparkstrength$useMorphlingBodyTexture(
            PlayerBodyEntity body,
            CallbackInfoReturnable<Identifier> cir
    ) {
        cir.setReturnValue(MorphlingAppearanceClientHelper.resolveBodyTexture(
                body,
                PlayerBodyEntityRenderer.DEFAULT_TEXTURE
        ));
    }
}
