package annina.sparkstrength.client.ui.common;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.WatheClient;
import net.minecraft.client.MinecraftClient;

import java.util.UUID;

/**
 * Resolves cached player names for client-only screens and HUD overlays.
 * 给客户端界面和 HUD 统一解析玩家名称。
 */
public final class PlayerNameResolver {
    private PlayerNameResolver() {
    }

    public static String playerName(UUID uuid) {
        var entry = WatheClient.PLAYER_ENTRIES_CACHE.get(uuid);
        if (entry != null && entry.getDisplayName() != null) {
            return entry.getDisplayName().getString();
        }
        if (entry != null) {
            return entry.getProfile().getName();
        }

        var client = MinecraftClient.getInstance();
        if (client.player != null) {
            var profile = GameWorldComponent.KEY.get(client.player.getWorld()).getGameProfiles().get(uuid);
            if (profile != null) {
                return profile.getName();
            }
        }
        return uuid.toString();
    }
}
