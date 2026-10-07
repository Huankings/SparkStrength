package annina.sparkstrength.client.ui.phantom;

import annina.sparkstrength.client.ui.common.PlayerHeadTextureHelper;
import annina.sparkstrength.client.ui.common.PlayerNameResolver;
import annina.sparkstrength.component.phantom.PhantomBackpackUserComponent;
import annina.sparkstrength.network.phantom.PhantomBackpackInvisibilityC2SPacket;
import dev.doctor4t.wathe.util.ShopEntry;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 幽灵背包里的玩家隐身头像按钮。
 *
 * <p>按钮本身不信任客户端判断；点击后只提交目标 UUID，
 * 服务端会重新检查幽灵身份、冷却、目标存活与饕餮吞噬状态。</p>
 */
public class PhantomBackpackPlayerWidget extends ButtonWidget {
    private static final int HIGHLIGHT_COLOR = 0x907FD8FF;

    private final UUID targetUuid;
    private final @Nullable PlayerListEntry targetPlayerEntry;

    public PhantomBackpackPlayerWidget(
            int x,
            int y,
            UUID targetUuid,
            @Nullable PlayerListEntry targetPlayerEntry
    ) {
        super(x, y, 16, 16, Text.empty(), button ->
                ClientPlayNetworking.send(new PhantomBackpackInvisibilityC2SPacket(targetUuid)),
                DEFAULT_NARRATION_SUPPLIER);
        this.targetUuid = targetUuid;
        this.targetPlayerEntry = targetPlayerEntry;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        ClientPlayerEntity localPlayer = MinecraftClient.getInstance().player;
        int cooldownTicks = localPlayer == null
                ? 0
                : PhantomBackpackUserComponent.KEY.get(localPlayer).getCooldownTicks();
        boolean onCooldown = cooldownTicks > 0;

        context.drawGuiTexture(ShopEntry.Type.TOOL.getTexture(), getX() - 7, getY() - 7, 30, 30);
        if (onCooldown) {
            context.setShaderColor(0.35f, 0.35f, 0.35f, 0.75f);
        }
        PlayerSkinDrawer.draw(
                context,
                PlayerHeadTextureHelper.resolveStableSkinTextures(targetUuid, targetPlayerEntry).texture(),
                getX(),
                getY(),
                16
        );
        context.setShaderColor(1f, 1f, 1f, 1f);

        if (onCooldown) {
            drawCooldownText(context, cooldownTicks);
        }

        if (isHovered()) {
            drawHighlight(context);
            Text name = targetPlayerEntry != null
                    ? Text.literal(targetPlayerEntry.getProfile().getName())
                    : Text.literal(PlayerNameResolver.playerName(targetUuid));
            TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
            context.drawTooltip(
                    textRenderer,
                    name,
                    getX() - 4 - textRenderer.getWidth(name) / 2,
                    getY() - 9
            );
        }
    }

    private void drawCooldownText(DrawContext context, int cooldownTicks) {
        int remainingSeconds = Math.max(1, (int) Math.ceil(cooldownTicks / 20.0));
        String timeText = remainingSeconds + "s";
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        int textX = getX() + 8 - textRenderer.getWidth(timeText) / 2;
        int textY = getY() + 4;
        context.drawText(textRenderer, timeText, textX, textY, 0xFF5555, true);
    }

    private void drawHighlight(DrawContext context) {
        int x = getX();
        int y = getY();
        context.fillGradient(RenderLayer.getGuiOverlay(), x, y, x + 16, y + 14, HIGHLIGHT_COLOR, HIGHLIGHT_COLOR, 0);
        context.fillGradient(RenderLayer.getGuiOverlay(), x, y + 14, x + 15, y + 15, HIGHLIGHT_COLOR, HIGHLIGHT_COLOR, 0);
        context.fillGradient(RenderLayer.getGuiOverlay(), x, y + 15, x + 14, y + 16, HIGHLIGHT_COLOR, HIGHLIGHT_COLOR, 0);
    }

    @Override
    public void drawMessage(DrawContext context, TextRenderer textRenderer, int color) {
        // 头像本身就是按钮内容，不绘制文字，避免遮挡玩家皮肤。
    }
}
