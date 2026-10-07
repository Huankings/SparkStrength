package annina.sparkstrength.client.ui.common;

import dev.doctor4t.wathe.client.WatheClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.util.SkinTextures;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 背包头像使用的稳定皮肤解析工具。
 *
 * <p>这里只读 Tab 列表、Wathe 玩家缓存和默认皮肤，不读取世界内玩家实体当前皮肤。
 * 世界实体可能已经被验尸官/变形怪改写外观，头像栏如果读取实时实体会把伪装状态泄露回 UI。</p>
 */
public final class PlayerHeadTextureHelper {
    private PlayerHeadTextureHelper() {
    }

    public static SkinTextures resolveStableSkinTextures(UUID targetUuid, @Nullable PlayerListEntry preferredEntry) {
        if (preferredEntry != null) {
            return preferredEntry.getSkinTextures();
        }

        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity localPlayer = client.player;
        if (localPlayer != null && localPlayer.networkHandler != null) {
            PlayerListEntry playerListEntry = localPlayer.networkHandler.getPlayerListEntry(targetUuid);
            if (playerListEntry != null) {
                return playerListEntry.getSkinTextures();
            }
        }

        if (WatheClient.PLAYER_ENTRIES_CACHE != null) {
            PlayerListEntry cachedEntry = WatheClient.PLAYER_ENTRIES_CACHE.get(targetUuid);
            if (cachedEntry != null) {
                return cachedEntry.getSkinTextures();
            }
        }

        return DefaultSkinHelper.getSkinTextures(targetUuid);
    }
}
