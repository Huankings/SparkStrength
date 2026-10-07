package annina.sparkstrength.client.ui.coroner;

import annina.sparkstrength.client.ui.common.PlayerHeadTextureHelper;
import annina.sparkstrength.client.ui.common.PlayerNameResolver;
import annina.sparkstrength.component.coroner.CoronerPlayerComponent;
import annina.sparkstrength.network.coroner.CoronerMorphC2SPacket;
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
 * 验尸官背包里的尸体身份变形头像按钮。
 *
 * <p>点击自己头像时仍然发包给服务端，由服务端把它解释为“解除变形”。
 * 这样客户端不需要保存第二套命令语义，也防止改包绕过服务端校验。</p>
 */
public final class CoronerPlayerWidget extends ButtonWidget {
    private static final int SLOT_HIGHLIGHT = 0x90FFBF49;
    private static final int SELF_BORDER = 0xC0C7C7C7;
    private static final int CURRENT_BORDER = 0xD03AB6FF;

    private final UUID targetUuid;
    private final boolean self;
    private final @Nullable PlayerListEntry targetPlayerEntry;

    public CoronerPlayerWidget(
            int x,
            int y,
            UUID targetUuid,
            boolean self,
            @Nullable PlayerListEntry targetPlayerEntry
    ) {
        super(x, y, 16, 16, Text.empty(),
                button -> ClientPlayNetworking.send(new CoronerMorphC2SPacket(targetUuid)),
                DEFAULT_NARRATION_SUPPLIER);
        this.targetUuid = targetUuid;
        this.self = self;
        this.targetPlayerEntry = targetPlayerEntry;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        ClientPlayerEntity localPlayer = MinecraftClient.getInstance().player;
        UUID activeDisguise = localPlayer == null
                ? null
                : CoronerPlayerComponent.KEY.get(localPlayer).activeDisguiseUuid();
        boolean selected = activeDisguise != null && targetUuid.equals(activeDisguise);

        context.drawGuiTexture(
                self ? ShopEntry.Type.TOOL.getTexture() : ShopEntry.Type.POISON.getTexture(),
                getX() - 7,
                getY() - 7,
                30,
                30
        );
        PlayerSkinDrawer.draw(
                context,
                PlayerHeadTextureHelper.resolveStableSkinTextures(targetUuid, targetPlayerEntry).texture(),
                getX(),
                getY(),
                16
        );

        if (isHovered()) {
            drawSlotHighlight(context);
            Text name = Text.literal(PlayerNameResolver.playerName(targetUuid));
            TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
            context.drawTooltip(
                    textRenderer,
                    name,
                    getX() - 4 - textRenderer.getWidth(name) / 2,
                    getY() - 9
            );
        }

        if (self) {
            drawBorder(context, SELF_BORDER);
        }
        if (selected) {
            drawBorder(context, CURRENT_BORDER);
        }
    }

    private void drawBorder(DrawContext context, int color) {
        int x = getX();
        int y = getY();
        context.fill(x - 2, y - 2, x + 18, y, color);
        context.fill(x - 2, y + 16, x + 18, y + 18, color);
        context.fill(x - 2, y - 2, x, y + 18, color);
        context.fill(x + 16, y - 2, x + 18, y + 18, color);
    }

    private void drawSlotHighlight(DrawContext context) {
        int x = getX();
        int y = getY();
        context.fillGradient(RenderLayer.getGuiOverlay(), x, y, x + 16, y + 14, SLOT_HIGHLIGHT, SLOT_HIGHLIGHT, 0);
        context.fillGradient(RenderLayer.getGuiOverlay(), x, y + 14, x + 15, y + 15, SLOT_HIGHLIGHT, SLOT_HIGHLIGHT, 0);
        context.fillGradient(RenderLayer.getGuiOverlay(), x, y + 15, x + 14, y + 16, SLOT_HIGHLIGHT, SLOT_HIGHLIGHT, 0);
    }

    @Override
    public void drawMessage(DrawContext context, TextRenderer textRenderer, int color) {
        // 头像就是按钮主体，不绘制文字，避免盖住玩家皮肤。
    }
}
