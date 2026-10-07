package annina.sparkstrength.client.mixin.detective;

import annina.sparkstrength.client.role.detective.CriminologistHudRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds SparkStrength's Detective second-skill line to the main HUD render pass.
 * 在主 HUD 渲染流程中追加 SparkStrength 侦探第二技能提示。
 */
@Mixin(InGameHud.class)
public abstract class CriminologistHudMixin {
    @Inject(method = "renderMainHud", at = @At("TAIL"))
    private void sparkstrength$renderCriminologistHud(
            DrawContext context,
            RenderTickCounter tickCounter,
            CallbackInfo ci
    ) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null) {
            CriminologistHudRenderer.render(context, player);
        }
    }
}
