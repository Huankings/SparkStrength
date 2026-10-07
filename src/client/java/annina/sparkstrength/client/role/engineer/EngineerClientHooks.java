package annina.sparkstrength.client.role.engineer;

import annina.sparkstrength.SparkStrengthEntities;
import annina.sparkstrength.client.renderer.CaptureDeviceEntityRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

/**
 * 工程师增强的客户端注册入口。
 */
public final class EngineerClientHooks {
    private EngineerClientHooks() {
    }

    public static void register() {
        EntityRendererRegistry.register(SparkStrengthEntities.captureDevice(), CaptureDeviceEntityRenderer::new);
    }
}
