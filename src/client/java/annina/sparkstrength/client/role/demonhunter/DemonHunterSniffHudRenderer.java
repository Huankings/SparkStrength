package annina.sparkstrength.client.role.demonhunter;

import annina.sparkstrength.component.demonhunter.DemonHunterSniffPlayerComponent;
import annina.sparkstrength.role.demonhunter.DemonHunterSniffRules;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.client.NoellesrolesClient;

/**
 * 在右下角绘制猎魔人嗅探技能状态。
 *
 * <p>坐标沿用 SparkStrength 侦探第二技能的右下角行位：放在 Wathe/NoellesRoles
 * 原能力提示上方一行。猎魔人和侦探不会同时是同一个角色，所以不会互相覆盖。</p>
 */
public final class DemonHunterSniffHudRenderer {
    private static final int RIGHT_PADDING = 5;
    private static final int BOTTOM_PADDING = 5;
    private static final int ROW_GAP = 4;

    private DemonHunterSniffHudRenderer() {
    }

    public static void render(DrawContext context, ClientPlayerEntity player) {
        if (!GameFunctions.isPlayerPlayingAndAlive(player)) {
            return;
        }
        Role role = GameWorldComponent.KEY.get(player.getWorld()).getRole(player);
        if (!DemonHunterSniffRules.isDemonHunter(role)) {
            return;
        }

        DemonHunterSniffPlayerComponent component = DemonHunterSniffPlayerComponent.KEY.get(player);
        Text line = stateText(component);
        TextRenderer renderer = MinecraftClient.getInstance().textRenderer;
        int x = context.getScaledWindowWidth() - RIGHT_PADDING - renderer.getWidth(line);
        int y = context.getScaledWindowHeight() - BOTTOM_PADDING - (renderer.fontHeight * 2) - ROW_GAP;
        context.drawTextWithShadow(renderer, line, x, y, Noellesroles.DEMON_HUNTER.color());
    }

    private static Text stateText(DemonHunterSniffPlayerComponent component) {
        if (component.getSniffCooldownTicks() > 0) {
            return Text.translatable(
                    "hud.sparkstrength.demon_hunter_sniff.cooldown",
                    seconds(component.getSniffCooldownTicks())
            );
        }
        return Text.translatable(
                "hud.sparkstrength.demon_hunter_sniff.ready",
                abilityKeyText()
        );
    }

    private static Text abilityKeyText() {
        KeyBinding abilityBind = NoellesrolesClient.abilityBind;
        return abilityBind != null ? abilityBind.getBoundKeyLocalizedText() : Text.literal("G");
    }

    private static int seconds(int ticks) {
        return (int) Math.ceil(ticks / 20.0);
    }
}
