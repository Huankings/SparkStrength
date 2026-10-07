package annina.sparkstrength.client.mixin.morphling;

import annina.sparkstrength.client.role.morphling.MorphlingAppearanceClientHelper;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
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
 * 让试剂触发中的玩家真正显示成采样目标。
 *
 * <p>贴图、第一人称手臂和 renderer 内部 model 都使用同一份 SkinTextures，
 * 避免只换皮肤不换 slim/classic 模型导致贴图错位。</p>
 */
@Mixin(PlayerEntityRenderer.class)
public abstract class MorphlingPlayerAppearanceMixin
        extends LivingEntityRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {
    protected MorphlingPlayerAppearanceMixin(
            EntityRendererFactory.Context context,
            PlayerEntityModel<AbstractClientPlayerEntity> entityModel,
            float shadowRadius
    ) {
        super(context, entityModel, shadowRadius);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void sparkstrength$cacheMorphlingPlayerModels(
            EntityRendererFactory.Context context,
            boolean slim,
            CallbackInfo ci
    ) {
        MorphlingAppearanceClientHelper.initializePlayerModels(context);
    }

    @Inject(
            method = "render(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("HEAD")
    )
    private void sparkstrength$applyMorphlingPlayerModel(
            AbstractClientPlayerEntity player,
            float yaw,
            float tickDelta,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            CallbackInfo ci
    ) {
        sparkstrength$applyResolvedModel(player);
    }

    @Inject(method = "renderRightArm", at = @At("HEAD"))
    private void sparkstrength$applyMorphlingRightArmModel(
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            AbstractClientPlayerEntity player,
            CallbackInfo ci
    ) {
        sparkstrength$applyResolvedModel(player);
    }

    @Inject(method = "renderLeftArm", at = @At("HEAD"))
    private void sparkstrength$applyMorphlingLeftArmModel(
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            AbstractClientPlayerEntity player,
            CallbackInfo ci
    ) {
        sparkstrength$applyResolvedModel(player);
    }

    @Inject(
            method = "getTexture(Lnet/minecraft/client/network/AbstractClientPlayerEntity;)Lnet/minecraft/util/Identifier;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void sparkstrength$useMorphlingReagentTexture(
            AbstractClientPlayerEntity player,
            CallbackInfoReturnable<Identifier> cir
    ) {
        SkinTextures skinTextures = MorphlingAppearanceClientHelper.resolveActivePlayerSkinTextures(player);
        if (skinTextures != null) {
            cir.setReturnValue(skinTextures.texture());
        }
    }

    @WrapOperation(
            method = "renderArm",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;getSkinTextures()Lnet/minecraft/client/util/SkinTextures;"
            )
    )
    private SkinTextures sparkstrength$useMorphlingReagentArmSkin(
            AbstractClientPlayerEntity player,
            Operation<SkinTextures> original
    ) {
        SkinTextures skinTextures = MorphlingAppearanceClientHelper.resolveActivePlayerSkinTextures(player);
        return skinTextures != null ? skinTextures : original.call(player);
    }

    private void sparkstrength$applyResolvedModel(AbstractClientPlayerEntity player) {
        PlayerEntityModel<AbstractClientPlayerEntity> resolvedModel =
                MorphlingAppearanceClientHelper.getPlayerModel(
                        MorphlingAppearanceClientHelper.resolveActivePlayerSkinTextures(player)
                );
        if (resolvedModel != null) {
            this.model = resolvedModel;
        }
    }
}
