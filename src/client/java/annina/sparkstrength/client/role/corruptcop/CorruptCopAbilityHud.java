package annina.sparkstrength.client.role.corruptcop;

import annina.sparkstrength.component.corruptcop.CorruptCopAbilityComponent;
import annina.sparkstrength.role.corruptcop.CorruptCopRules;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import org.agmas.noellesroles.client.NoellesrolesClient;

/**
 * Renders the Corrupt Cop's owner-only ability state in the NoellesRoles HUD style.
 * 以 NoellesRoles 的 HUD 风格渲染黑警仅本人可见的主动技能状态。
 */
public final class CorruptCopAbilityHud {
    private CorruptCopAbilityHud() {
    }

    public static void render(TextRenderer renderer, ClientPlayerEntity player, DrawContext context) {
        if (!GameFunctions.isPlayerPlayingAndAlive(player)) {
            return;
        }
        Role role = GameWorldComponent.KEY.get(player.getWorld()).getRole(player);
        if (!CorruptCopRules.isCorruptCop(role)) {
            return;
        }

        CorruptCopAbilityComponent ability = CorruptCopAbilityComponent.KEY.get(player);
        String key = ability.isActive()
                ? "tip.sparkstrength.corrupt_cop.active"
                : "tip.sparkstrength.corrupt_cop.inactive";
        Text text = Text.translatable(key, NoellesrolesClient.abilityBind.getBoundKeyLocalizedText());
        int y = context.getScaledWindowHeight() - renderer.fontHeight;
        drawRightAligned(renderer, context, text, y);

        int remainingSeconds = CorruptCopMusicController.remainingResumeSeconds();
        if (!ability.isActive() && remainingSeconds > 0) {
            Text resumeText = Text.translatable(
                    "tip.sparkstrength.corrupt_cop.music_resume_remaining",
                    remainingSeconds
            );
            drawRightAligned(renderer, context, resumeText, y - renderer.fontHeight);
        }
    }

    private static void drawRightAligned(TextRenderer renderer, DrawContext context, Text text, int y) {
        int x = context.getScaledWindowWidth() - renderer.getWidth(text);
        context.drawTextWithShadow(renderer, text, x, y, CorruptCopRules.ABILITY_COLOR);
    }
}
