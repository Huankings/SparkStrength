package annina.sparkstrength.client.mixin.demonhunter;

import annina.sparkstrength.client.role.demonhunter.DemonHunterSniffHudRenderer;
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
 * 把猎魔人嗅探技能 HUD 追加到主 HUD 渲染流程。
 */
@Mixin(InGameHud.class)
public abstract class DemonHunterSniffHudMixin {
    @Inject(method = "renderMainHud", at = @At("TAIL"))
    private void sparkstrength$renderDemonHunterSniffHud(
            DrawContext context,
            RenderTickCounter tickCounter,
            CallbackInfo ci
    ) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null) {
            DemonHunterSniffHudRenderer.render(context, player);
        }
    }
}
