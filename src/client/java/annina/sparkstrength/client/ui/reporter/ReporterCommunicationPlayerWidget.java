package annina.sparkstrength.client.ui.reporter;

import annina.sparkstrength.client.ui.common.PlayerHeadTextureHelper;
import annina.sparkstrength.client.ui.common.PlayerNameResolver;
import annina.sparkstrength.component.reporter.ReporterCommunicationComponent;
import annina.sparkstrength.network.reporter.ReporterCommunicationC2SPacket;
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
 * 记者背包通讯加强的玩家头像按钮。
 *
 * <p>交互复刻自改版接线员：第一次点击只记录第一名玩家，第二次点击才向服务端发包；
 * 两次选择不同玩家会尝试接线，两次选择同一玩家会尝试广播采访。客户端只做展示和发包，
 * 真正的角色、冷却、死亡、饕餮吞噬豁免全部交给服务端重判。</p>
 */
public class ReporterCommunicationPlayerWidget extends ButtonWidget {
    private static final int HIGHLIGHT_COLOR = 0x90D2B464;

    public static UUID firstChoice = null;

    private final UUID targetUuid;
    private final @Nullable PlayerListEntry targetPlayerEntry;

    public ReporterCommunicationPlayerWidget(
            int x,
            int y,
            UUID targetUuid,
            @Nullable PlayerListEntry targetPlayerEntry
    ) {
        super(x, y, 16, 16, Text.empty(), button -> {
            ClientPlayerEntity localPlayer = MinecraftClient.getInstance().player;
            if (localPlayer == null || ReporterCommunicationComponent.KEY.get(localPlayer).isOnCooldown()) {
                return;
            }

            if (firstChoice == null) {
                firstChoice = targetUuid;
                return;
            }

            ClientPlayNetworking.send(new ReporterCommunicationC2SPacket(firstChoice, targetUuid));
            firstChoice = null;
        }, DEFAULT_NARRATION_SUPPLIER);
        this.targetUuid = targetUuid;
        this.targetPlayerEntry = targetPlayerEntry;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        ClientPlayerEntity localPlayer = MinecraftClient.getInstance().player;
        int cooldownTicks = localPlayer == null
                ? 0
                : ReporterCommunicationComponent.KEY.get(localPlayer).getCooldownTicks();
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

        // 第一次选择的玩家保持高亮，方便记者确认第二次点击要接线还是广播。
        if (targetUuid.equals(firstChoice) || isHovered()) {
            drawHighlight(context);
        }

        if (isHovered()) {
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
