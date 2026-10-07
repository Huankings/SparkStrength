package annina.sparkstrength.client.mixin.reporter;

import annina.sparkstrength.client.ui.common.PlayerPageLayout;
import annina.sparkstrength.client.ui.common.PlayerPageSwitchWidget;
import annina.sparkstrength.client.ui.common.PlayerSelectionPageState;
import annina.sparkstrength.client.ui.reporter.ReporterCommunicationPlayerWidget;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedHandledScreen;
import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedInventoryScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Items;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.Text;
import org.agmas.noellesroles.Noellesroles;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 给 Wathe 受限背包界面追加记者“接线 / 广播采访”头像栏。
 *
 * <p>这里不改 NoellesRolesspark 的记者本体，所有按钮和页码状态都放在 SparkStrength 内；
 * 服务端收到两次点击后再判断接线或广播，这样旧的 G 键标记透视逻辑和冷却完全独立。</p>
 */
@Mixin(LimitedInventoryScreen.class)
public abstract class ReporterInventoryScreenMixin extends LimitedHandledScreen<PlayerScreenHandler> {
    @Shadow @Final public ClientPlayerEntity player;

    @Unique private final List<ReporterCommunicationPlayerWidget> sparkstrength$reporterPlayerWidgets = new ArrayList<>();
    @Unique private PlayerPageSwitchWidget sparkstrength$reporterPreviousPageWidget;
    @Unique private PlayerPageSwitchWidget sparkstrength$reporterNextPageWidget;
    @Unique private int sparkstrength$reporterCurrentPage;
    @Unique private boolean sparkstrength$reporterUiActive;

    public ReporterInventoryScreenMixin(PlayerScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Inject(method = "init", at = @At("HEAD"))
    private void sparkstrength$reporterAddCommunicationButtons(CallbackInfo ci) {
        sparkstrength$reporterUiActive = false;
        ReporterCommunicationPlayerWidget.firstChoice = null;
        if (player == null || player.getWorld() == null) {
            return;
        }

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        if (!gameWorld.isRole(player, Noellesroles.REPORTER)) {
            return;
        }

        sparkstrength$reporterAddPlayerSelectionUI();
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void sparkstrength$reporterRenderSelectionHint(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!sparkstrength$reporterUiActive) {
            return;
        }

        Text text = ReporterCommunicationPlayerWidget.firstChoice == null
                ? Text.translatable("ui.sparkstrength.reporter.first_player_selection")
                : Text.translatable("ui.sparkstrength.reporter.second_player_selection");
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        int x = this.width / 2 - textRenderer.getWidth(text) / 2;
        int y = (this.height - 32) / 2 + 40;
        context.drawTextWithShadow(textRenderer, text, x, y, Noellesroles.REPORTER.color());
    }

    @Unique
    private void sparkstrength$reporterAddPlayerSelectionUI() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.player.networkHandler == null) {
            return;
        }

        /*
         * 复刻接线员的选择范围：使用在线网络列表并额外确保自己在列表中。
         * 目标是否死亡、是否被饕餮吞噬不在客户端过滤，避免 UI 泄露隐藏状态；
         * 服务端会在发包后统一决定成功、失败冷却和回放。
         */
        Set<UUID> targetUuids = new LinkedHashSet<>(client.player.networkHandler.getPlayerUuids());
        targetUuids.add(client.player.getUuid());

        int y = PlayerPageLayout.getPlayerRowY(this.height);
        sparkstrength$reporterPlayerWidgets.clear();
        sparkstrength$reporterCurrentPage = PlayerSelectionPageState.getReporterPage();
        sparkstrength$reporterUiActive = true;

        for (UUID targetUuid : targetUuids) {
            PlayerListEntry playerListEntry = client.player.networkHandler.getPlayerListEntry(targetUuid);
            ReporterCommunicationPlayerWidget child = new ReporterCommunicationPlayerWidget(
                    0,
                    y,
                    targetUuid,
                    playerListEntry
            );
            sparkstrength$reporterPlayerWidgets.add(child);
            addDrawableChild(child);
        }

        sparkstrength$reporterPreviousPageWidget = addDrawableChild(new PlayerPageSwitchWidget(
                0,
                y,
                Items.PURPLE_DYE.getDefaultStack(),
                Text.translatable("ui.sparkstrength.pagination.previous"),
                button -> {
                    sparkstrength$reporterCurrentPage--;
                    sparkstrength$reporterRefreshPage();
                }
        ));
        sparkstrength$reporterNextPageWidget = addDrawableChild(new PlayerPageSwitchWidget(
                0,
                y,
                Items.LIME_DYE.getDefaultStack(),
                Text.translatable("ui.sparkstrength.pagination.next"),
                button -> {
                    sparkstrength$reporterCurrentPage++;
                    sparkstrength$reporterRefreshPage();
                }
        ));

        sparkstrength$reporterRefreshPage();
    }

    @Unique
    private void sparkstrength$reporterRefreshPage() {
        int totalPages = PlayerPageLayout.getTotalPageCount(sparkstrength$reporterPlayerWidgets.size());
        if (sparkstrength$reporterCurrentPage < 0) {
            sparkstrength$reporterCurrentPage = 0;
        }
        if (sparkstrength$reporterCurrentPage >= totalPages) {
            sparkstrength$reporterCurrentPage = totalPages - 1;
        }
        PlayerSelectionPageState.setReporterPage(sparkstrength$reporterCurrentPage);

        int startIndex = sparkstrength$reporterCurrentPage * PlayerPageLayout.PLAYERS_PER_PAGE;
        int endIndex = Math.min(startIndex + PlayerPageLayout.PLAYERS_PER_PAGE, sparkstrength$reporterPlayerWidgets.size());
        int visibleCount = endIndex - startIndex;
        int y = PlayerPageLayout.getPlayerRowY(this.height);
        boolean showPrevious = sparkstrength$reporterCurrentPage > 0;
        boolean showNext = sparkstrength$reporterCurrentPage < totalPages - 1;
        int groupStartX = PlayerPageLayout.getCenteredGroupStartX(this.width, visibleCount, showPrevious, showNext);
        int playerStartX = groupStartX + (showPrevious ? PlayerPageLayout.SLOT_APART : 0);

        for (int i = 0; i < sparkstrength$reporterPlayerWidgets.size(); i++) {
            ReporterCommunicationPlayerWidget widget = sparkstrength$reporterPlayerWidgets.get(i);
            boolean visible = i >= startIndex && i < endIndex;
            widget.visible = visible;
            widget.active = visible;
            if (visible) {
                int visibleIndex = i - startIndex;
                widget.setX(playerStartX + visibleIndex * PlayerPageLayout.SLOT_APART);
                widget.setY(y);
            }
        }

        if (sparkstrength$reporterPreviousPageWidget != null) {
            sparkstrength$reporterPreviousPageWidget.visible = showPrevious;
            sparkstrength$reporterPreviousPageWidget.active = showPrevious;
            sparkstrength$reporterPreviousPageWidget.setX(groupStartX);
            sparkstrength$reporterPreviousPageWidget.setY(y);
        }
        if (sparkstrength$reporterNextPageWidget != null) {
            sparkstrength$reporterNextPageWidget.visible = showNext;
            sparkstrength$reporterNextPageWidget.active = showNext;
            sparkstrength$reporterNextPageWidget.setX(playerStartX + visibleCount * PlayerPageLayout.SLOT_APART);
            sparkstrength$reporterNextPageWidget.setY(y);
        }
    }
}
