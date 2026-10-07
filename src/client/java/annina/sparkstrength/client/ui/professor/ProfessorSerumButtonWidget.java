package annina.sparkstrength.client.ui.professor;

import annina.sparkstrength.component.professor.ProfessorSerumUserComponent;
import annina.sparkstrength.network.professor.ProfessorRemoteFeedC2SPacket;
import annina.sparkstrength.role.professor.ProfessorSerumType;
import dev.doctor4t.wathe.util.ShopEntry;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.UUID;

/**
 * 教授背包第二页的试剂按钮。
 *
 * <p>点击按钮后只发网络请求，不在客户端预判成功/失败；背包里是否有试剂、
 * 目标是否存活和冷却怎么结算都由服务端处理。</p>
 */
public class ProfessorSerumButtonWidget extends ButtonWidget {
    private UUID targetUuid;
    private final ProfessorSerumType serumType;
    private final ItemStack iconStack;

    public ProfessorSerumButtonWidget(int x, int y, UUID targetUuid, ProfessorSerumType serumType) {
        super(x, y, 16, 16, Text.translatable("item.sparkstrength." + serumType.id()), button -> {
                    if (button instanceof ProfessorSerumButtonWidget widget) {
                        ClientPlayNetworking.send(new ProfessorRemoteFeedC2SPacket(widget.targetUuid, widget.serumType));
                    }
                },
                DEFAULT_NARRATION_SUPPLIER);
        this.targetUuid = targetUuid;
        this.serumType = serumType;
        this.iconStack = serumType.defaultStack();
    }

    public void setTargetUuid(UUID targetUuid) {
        this.targetUuid = targetUuid;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        ClientPlayerEntity localPlayer = MinecraftClient.getInstance().player;
        int cooldownTicks = localPlayer != null
                ? ProfessorSerumUserComponent.KEY.get(localPlayer).getCooldownTicks()
                : 0;
        boolean onCooldown = cooldownTicks > 0;

        context.drawGuiTexture(ShopEntry.Type.TOOL.getTexture(), getX() - 7, getY() - 7, 30, 30);
        if (onCooldown) {
            context.setShaderColor(0.35F, 0.35F, 0.35F, 0.75F);
        }
        context.drawItem(iconStack, getX(), getY());
        context.setShaderColor(1F, 1F, 1F, 1F);

        if (onCooldown) {
            int remainingSeconds = Math.max(1, (int) Math.ceil(cooldownTicks / 20.0));
            String timeText = remainingSeconds + "s";
            TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
            int textWidth = textRenderer.getWidth(timeText);
            int textX = getX() + 8 - textWidth / 2;
            int textY = getY() - 12;

            // 冷却文字放到试剂图标上方，避开 16x16 的物品纹理区域。
            // 底色只包住文字本身，让倒计时在背包背景和商店槽边框上都能看清。
            context.fill(textX - 2, textY - 1, textX + textWidth + 2, textY + 9, 0xAA000000);
            context.drawText(textRenderer, timeText, textX, textY, 0xFF5555, true);
        }

        if (isHovered()) {
            drawHighlight(context);
            context.drawTooltip(
                    MinecraftClient.getInstance().textRenderer,
                    Text.translatable("ui.sparkstrength.professor.feed_serum", getMessage()),
                    mouseX,
                    mouseY
            );
        }
    }

    private void drawHighlight(DrawContext context) {
        int color = switch (serumType) {
            case INVISIBILITY -> 0x907FD8FF;
            case DOORPASSING -> 0x904B0082;
            case SEDATIVE -> 0x90FFD84A;
            case TRUTH -> 0x90FFFFFF;
        };
        int x = getX();
        int y = getY();
        context.fillGradient(RenderLayer.getGuiOverlay(), x, y, x + 16, y + 14, color, color, 0);
        context.fillGradient(RenderLayer.getGuiOverlay(), x, y + 14, x + 15, y + 15, color, color, 0);
        context.fillGradient(RenderLayer.getGuiOverlay(), x, y + 15, x + 14, y + 16, color, color, 0);
    }

    @Override
    public void drawMessage(DrawContext context, TextRenderer textRenderer, int color) {
        // 图标就是按钮主体，文字只放在 tooltip。
    }
}
