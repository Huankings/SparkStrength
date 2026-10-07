package annina.sparkstrength.client.role.detective;

import annina.sparkstrength.client.ui.common.PlayerNameResolver;
import com.mojang.authlib.GameProfile;
import annina.sparkstrength.component.detective.CriminologistPlayerComponent;
import annina.sparkstrength.role.detective.CriminologistRules;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.text.Text;

import java.util.UUID;

/**
 * Renders the Detective's second active skill line above the bottom-right ability row.
 * 在右下角第一主动技能上方显示侦探第二主动技能状态。
 */
public final class CriminologistHudRenderer {
    private static final int RIGHT_PADDING = 5;
    private static final int BOTTOM_PADDING = 5;
    private static final int ROW_GAP = 4;
    private static final int AVATAR_SIZE = 12;

    private CriminologistHudRenderer() {
    }

    public static void render(DrawContext context, ClientPlayerEntity player) {
        if (!GameFunctions.isPlayerPlayingAndAlive(player)) {
            return;
        }
        Role role = GameWorldComponent.KEY.get(player.getWorld()).getRole(player);
        if (!CriminologistRules.isDetective(role)) {
            return;
        }

        CriminologistPlayerComponent component = CriminologistPlayerComponent.KEY.get(player);
        TextRenderer renderer = MinecraftClient.getInstance().textRenderer;
        UUID targetUuid = component.getCriminologistTrackingTargetUuid();
        if (targetUuid != null) {
            Text line = Text.translatable(
                    "hud.sparkstrength.criminologist.tracking",
                    PlayerNameResolver.playerName(targetUuid)
            );
            int lineWidth = renderer.getWidth(line);
            int y = context.getScaledWindowHeight() - BOTTOM_PADDING - (renderer.fontHeight * 2) - ROW_GAP;
            int x = context.getScaledWindowWidth() - RIGHT_PADDING - lineWidth - AVATAR_SIZE - ROW_GAP;
            PlayerSkinDrawer.draw(context, skin(targetUuid), x, y - 2, AVATAR_SIZE);
            context.drawTextWithShadow(renderer, line, x + AVATAR_SIZE + ROW_GAP, y, 0xFF3030);
            return;
        }

        Text line = stateText(component);
        int x = context.getScaledWindowWidth() - RIGHT_PADDING - renderer.getWidth(line);
        int y = context.getScaledWindowHeight() - BOTTOM_PADDING - (renderer.fontHeight * 2) - ROW_GAP;
        context.drawTextWithShadow(renderer, line, x, y, 0xFF3030);
    }

    private static Text stateText(CriminologistPlayerComponent component) {
        if (component.getCriminologistCooldownTicks() > 0) {
            return Text.translatable(
                    "hud.sparkstrength.criminologist.cooldown",
                    seconds(component.getCriminologistCooldownTicks())
            );
        }
        return Text.translatable("hud.sparkstrength.criminologist.ready");
    }

    private static SkinTextures skin(UUID uuid) {
        PlayerListEntry entry = WatheClient.PLAYER_ENTRIES_CACHE.get(uuid);
        if (entry != null) {
            return entry.getSkinTextures();
        }
        return DefaultSkinHelper.getSkinTextures(new GameProfile(uuid, "Unknown"));
    }

    private static int seconds(int ticks) {
        return (int) Math.ceil(ticks / 20.0);
    }
}
