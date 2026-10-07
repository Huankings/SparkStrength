package annina.sparkstrength.client.screen.criminologist;

import annina.sparkstrength.client.ui.common.PlayerNameResolver;
import com.mojang.authlib.GameProfile;
import annina.sparkstrength.network.criminologist.SelectCriminologistTargetC2SPacket;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.util.ShopEntry;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.text.Text;

import java.util.UUID;

final class CriminologistTargetWidget extends ButtonWidget {
    static final int WIDGET_SIZE = 16;
    private static final int SLOT_SIZE = 30;
    private static final int SLOT_OFFSET = 7;
    private final UUID victimUuid;
    private final UUID targetUuid;

    CriminologistTargetWidget(int x, int y, UUID victimUuid, UUID targetUuid) {
        super(
                x,
                y,
                WIDGET_SIZE,
                WIDGET_SIZE,
                Text.literal(PlayerNameResolver.playerName(targetUuid)),
                button -> {
                    ClientPlayNetworking.send(new SelectCriminologistTargetC2SPacket(victimUuid, targetUuid));
                    MinecraftClient.getInstance().setScreen(null);
                },
                DEFAULT_NARRATION_SUPPLIER
        );
        this.victimUuid = victimUuid;
        this.targetUuid = targetUuid;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        context.drawGuiTexture(
                ShopEntry.Type.TOOL.getTexture(),
                getX() - SLOT_OFFSET,
                getY() - SLOT_OFFSET,
                SLOT_SIZE,
                SLOT_SIZE
        );
        PlayerSkinDrawer.draw(context, skin(), getX(), getY(), WIDGET_SIZE);
        if (isHovered()) {
            drawSlotHighlight(context, getX(), getY(), 0);
            context.drawTooltip(
                    MinecraftClient.getInstance().textRenderer,
                    Text.literal(PlayerNameResolver.playerName(targetUuid)),
                    mouseX,
                    mouseY
            );
        }
    }

    private SkinTextures skin() {
        PlayerListEntry entry = WatheClient.PLAYER_ENTRIES_CACHE.get(targetUuid);
        if (entry != null) {
            return entry.getSkinTextures();
        }
        return DefaultSkinHelper.getSkinTextures(new GameProfile(targetUuid, "Unknown"));
    }

    private static void drawSlotHighlight(DrawContext context, int x, int y, int z) {
        int color = -1862287543;
        context.fillGradient(RenderLayer.getGuiOverlay(), x, y, x + 16, y + 14, color, color, z);
        context.fillGradient(RenderLayer.getGuiOverlay(), x, y + 14, x + 15, y + 15, color, color, z);
        context.fillGradient(RenderLayer.getGuiOverlay(), x, y + 15, x + 14, y + 16, color, color, z);
    }
}
