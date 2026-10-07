package annina.sparkstrength.client.ui.professor;

import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedInventoryScreen;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 教授背包第一页的玩家头像按钮。
 *
 * <p>按钮只负责选择“准备投喂谁”，不会直接发包；真正投喂要在第二页选择试剂后才提交。
 * 这样服务端能收到完整的目标 UUID + 试剂类型，并在同一个入口里统一校验。</p>
 */
public class ProfessorRemoteFeedPlayerWidget extends ButtonWidget {
    private final UUID targetUuid;
    @Nullable
    private final PlayerListEntry targetPlayerEntry;

    public ProfessorRemoteFeedPlayerWidget(
            LimitedInventoryScreen screen,
            int x,
            int y,
            UUID targetUuid,
            @Nullable PlayerListEntry targetPlayerEntry,
            PressAction onPress
    ) {
        super(x, y, 16, 16, Text.empty(), onPress, DEFAULT_NARRATION_SUPPLIER);
        this.targetUuid = targetUuid;
        this.targetPlayerEntry = targetPlayerEntry;
    }

    public UUID targetUuid() {
        return targetUuid;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        context.drawGuiTexture(ShopEntry.Type.TOOL.getTexture(), getX() - 7, getY() - 7, 30, 30);
        PlayerSkinDrawer.draw(context, resolveSkinTextures().texture(), getX(), getY(), 16);

        if (isHovered()) {
            drawHighlight(context);
            Text name = targetPlayerEntry != null
                    ? Text.literal(targetPlayerEntry.getProfile().getName())
                    : Text.translatable("ui.sparkstrength.professor.unknown_player");
            TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
            context.drawTooltip(
                    textRenderer,
                    name,
                    getX() - 4 - textRenderer.getWidth(name) / 2,
                    getY() - 9
            );
        }
    }

    private SkinTextures resolveSkinTextures() {
        if (targetPlayerEntry != null) {
            return targetPlayerEntry.getSkinTextures();
        }

        ClientPlayerEntity localPlayer = MinecraftClient.getInstance().player;
        if (localPlayer != null && localPlayer.networkHandler != null) {
            PlayerListEntry entry = localPlayer.networkHandler.getPlayerListEntry(targetUuid);
            if (entry != null) {
                return entry.getSkinTextures();
            }
        }

        return DefaultSkinHelper.getSkinTextures(targetUuid);
    }

    private void drawHighlight(DrawContext context) {
        int color = 0x907FD8FF;
        int x = getX();
        int y = getY();
        context.fillGradient(RenderLayer.getGuiOverlay(), x, y, x + 16, y + 14, color, color, 0);
        context.fillGradient(RenderLayer.getGuiOverlay(), x, y + 14, x + 15, y + 15, color, color, 0);
        context.fillGradient(RenderLayer.getGuiOverlay(), x, y + 15, x + 14, y + 16, color, color, 0);
    }

    @Override
    public void drawMessage(DrawContext context, TextRenderer textRenderer, int color) {
        // 头像就是按钮内容，不额外绘制文字，避免盖住皮肤。
    }
}
