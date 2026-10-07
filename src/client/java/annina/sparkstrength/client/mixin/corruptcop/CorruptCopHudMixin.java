package annina.sparkstrength.client.mixin.corruptcop;

import annina.sparkstrength.client.role.corruptcop.CorruptCopAbilityHud;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds the Corrupt Cop ability toggle text to the in-game HUD.
 * 将黑警主动技能的开关文字加入游戏内 HUD。
 */
@Mixin(InGameHud.class)
public abstract class CorruptCopHudMixin {
    @Shadow
    @Final
    private MinecraftClient client;

    @Inject(method = "renderMainHud", at = @At("TAIL"))
    private void sparkstrength$renderCorruptCopHud(
            DrawContext context,
            RenderTickCounter tickCounter,
            CallbackInfo ci
    ) {
        ClientPlayerEntity player = client.player;
        if (player == null) {
            return;
        }
        TextRenderer renderer = client.textRenderer;
        CorruptCopAbilityHud.render(renderer, player, context);
    }
}
