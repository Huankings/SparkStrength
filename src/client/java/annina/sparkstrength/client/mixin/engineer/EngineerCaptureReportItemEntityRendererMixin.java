package annina.sparkstrength.client.mixin.engineer;

import annina.sparkstrength.role.engineer.EngineerCaptureReport;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.ItemEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * 掉落在地上的捕捉报告也需要隐藏。
 *
 * <p>手持报告由 NoellesRoles 的装备包过滤隐藏；如果玩家背包已满导致报告掉落，
 * 这里继续在客户端渲染层隐藏它：拥有者本人、死亡/旁观/创造观察者可见，其他存活玩家不可见。</p>
 */
@Mixin(ItemEntityRenderer.class)
public abstract class EngineerCaptureReportItemEntityRendererMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void sparkstrength$hideCaptureReportItemEntity(
            ItemEntity entity,
            float yaw,
            float tickDelta,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            CallbackInfo ci
    ) {
        ItemStack stack = entity.getStack();
        if (!EngineerCaptureReport.isCaptureReport(stack)) {
            return;
        }

        ClientPlayerEntity viewer = MinecraftClient.getInstance().player;
        if (viewer == null) {
            return;
        }

        UUID ownerUuid = EngineerCaptureReport.ownerUuid(stack);
        if (ownerUuid != null && ownerUuid.equals(viewer.getUuid())) {
            return;
        }
        if (!GameFunctions.isPlayerPlayingAndAlive(viewer)
                || GameFunctions.isPlayerSpectatingOrCreative(viewer)) {
            return;
        }

        ci.cancel();
    }
}
