package annina.sparkstrength.client.renderer;

import annina.sparkstrength.SparkStrengthItems;
import annina.sparkstrength.entity.CaptureDeviceEntity;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

import java.util.UUID;

/**
 * 捕捉装置客户端渲染。
 *
 * <p>可见性在这里处理：放置者本人能看到自己的装置，死亡/旁观/创造观察者也能看到；
 * 其它仍在局内存活的玩家完全不渲染，避免装置暴露位置。</p>
 */
public final class CaptureDeviceEntityRenderer extends EntityRenderer<CaptureDeviceEntity> {
    private final ItemRenderer itemRenderer;

    public CaptureDeviceEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public Identifier getTexture(CaptureDeviceEntity entity) {
        return PlayerScreenHandler.BLOCK_ATLAS_TEXTURE;
    }

    @Override
    public void render(
            CaptureDeviceEntity entity,
            float yaw,
            float tickDelta,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light
    ) {
        if (!shouldRenderForViewer(entity)) {
            return;
        }

        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-entity.getYaw()));
        matrices.translate(0.0F, entity.isCeilingMounted() ? -0.02F : 0.02F, 0.0F);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(entity.isCeilingMounted() ? -90.0F : 90.0F));
        matrices.scale(0.4F, 0.4F, 0.4F);
        itemRenderer.renderItem(
                SparkStrengthItems.captureDevice().getDefaultStack(),
                ModelTransformationMode.FIXED,
                light,
                OverlayTexture.DEFAULT_UV,
                matrices,
                vertexConsumers,
                entity.getWorld(),
                entity.getId()
        );
        matrices.pop();
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    private static boolean shouldRenderForViewer(CaptureDeviceEntity entity) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return false;
        }

        UUID ownerUuid = entity.getOwnerUuid();
        if (ownerUuid != null && ownerUuid.equals(client.player.getUuid())) {
            return true;
        }

        return !GameFunctions.isPlayerPlayingAndAlive(client.player)
                || GameFunctions.isPlayerSpectatingOrCreative(client.player);
    }
}
