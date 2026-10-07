package annina.sparkstrength.client.mixin.professor;

import annina.sparkstrength.client.ui.common.PlayerPageLayout;
import annina.sparkstrength.client.ui.common.PlayerPageSwitchWidget;
import annina.sparkstrength.client.ui.common.PlayerSelectionPageState;
import annina.sparkstrength.client.ui.professor.ProfessorRemoteFeedPlayerWidget;
import annina.sparkstrength.client.ui.professor.ProfessorSerumButtonWidget;
import annina.sparkstrength.role.professor.ProfessorSerumRules;
import annina.sparkstrength.role.professor.ProfessorSerumType;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedHandledScreen;
import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedInventoryScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Items;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 给教授背包添加“远程投喂”两级按钮。
 *
 * <p>第一页显示所有在线玩家头像；点击头像后切到第二页，显示四种试剂按钮。
 * 这套 UI 只负责采集选择，最终是否成功由服务器再次检查，避免客户端靠改包绕过限制。</p>
 */
@Mixin(LimitedInventoryScreen.class)
public abstract class ProfessorInventoryScreenMixin extends LimitedHandledScreen<PlayerScreenHandler> {
    @Shadow @Final public ClientPlayerEntity player;

    @Unique private final List<ProfessorRemoteFeedPlayerWidget> sparkstrength$professorPlayerWidgets = new ArrayList<>();
    @Unique private final List<ProfessorSerumButtonWidget> sparkstrength$professorSerumWidgets = new ArrayList<>();
    @Unique private PlayerPageSwitchWidget sparkstrength$professorPreviousPageWidget;
    @Unique private PlayerPageSwitchWidget sparkstrength$professorNextPageWidget;
    @Unique private PlayerPageSwitchWidget sparkstrength$professorBackWidget;
    @Unique private int sparkstrength$professorCurrentPage;
    @Unique private UUID sparkstrength$professorSelectedTarget;
    @Unique private boolean sparkstrength$professorSelectingSerum;

    public ProfessorInventoryScreenMixin(PlayerScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Inject(method = "init", at = @At("HEAD"))
    private void sparkstrength$professorAddRemoteFeedButtons(CallbackInfo ci) {
        if (player == null || player.getWorld() == null) {
            return;
        }

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        if (!ProfessorSerumRules.isProfessor(gameWorld.getRole(player))) {
            return;
        }

        sparkstrength$professorAddPlayerSelectionUI();
    }

    @Unique
    private void sparkstrength$professorAddPlayerSelectionUI() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.player.networkHandler == null) {
            return;
        }

        List<UUID> playerUuids = new ArrayList<>(client.player.networkHandler.getPlayerUuids());
        int y = PlayerPageLayout.getPlayerRowY(this.height);
        sparkstrength$professorPlayerWidgets.clear();
        sparkstrength$professorSerumWidgets.clear();
        sparkstrength$professorCurrentPage = PlayerSelectionPageState.getProfessorPage();
        sparkstrength$professorSelectingSerum = false;
        sparkstrength$professorSelectedTarget = null;

        for (UUID targetUuid : playerUuids) {
            PlayerListEntry playerListEntry = client.player.networkHandler.getPlayerListEntry(targetUuid);
            ProfessorRemoteFeedPlayerWidget child = new ProfessorRemoteFeedPlayerWidget(
                    (LimitedInventoryScreen) (Object) this,
                    0,
                    y,
                    targetUuid,
                    playerListEntry,
                    button -> sparkstrength$professorOpenSerumSelection(targetUuid)
            );
            sparkstrength$professorPlayerWidgets.add(child);
            addDrawableChild(child);
        }

        sparkstrength$professorPreviousPageWidget = addDrawableChild(new PlayerPageSwitchWidget(
                0,
                y,
                Items.PURPLE_DYE.getDefaultStack(),
                Text.translatable("ui.sparkstrength.pagination.previous"),
                button -> {
                    sparkstrength$professorCurrentPage--;
                    sparkstrength$professorRefreshPage();
                }
        ));
        sparkstrength$professorNextPageWidget = addDrawableChild(new PlayerPageSwitchWidget(
                0,
                y,
                Items.LIME_DYE.getDefaultStack(),
                Text.translatable("ui.sparkstrength.pagination.next"),
                button -> {
                    sparkstrength$professorCurrentPage++;
                    sparkstrength$professorRefreshPage();
                }
        ));
        sparkstrength$professorBackWidget = addDrawableChild(new PlayerPageSwitchWidget(
                0,
                y,
                Items.ARROW.getDefaultStack(),
                Text.translatable("ui.sparkstrength.professor.back_to_players"),
                button -> sparkstrength$professorOpenPlayerSelection()
        ));

        int serumStartX = this.width / 2 - (ProfessorSerumType.BACKPACK_ORDER.size() * PlayerPageLayout.SLOT_APART) / 2
                + PlayerPageLayout.SLOT_X_OFFSET;
        for (int i = 0; i < ProfessorSerumType.BACKPACK_ORDER.size(); i++) {
            ProfessorSerumButtonWidget widget = new ProfessorSerumButtonWidget(
                    serumStartX + i * PlayerPageLayout.SLOT_APART,
                    y,
                    player.getUuid(),
                    ProfessorSerumType.BACKPACK_ORDER.get(i)
            );
            sparkstrength$professorSerumWidgets.add(widget);
            addDrawableChild(widget);
        }

        sparkstrength$professorRefreshPage();
    }

    @Unique
    private void sparkstrength$professorOpenSerumSelection(UUID targetUuid) {
        sparkstrength$professorSelectedTarget = targetUuid;
        sparkstrength$professorSelectingSerum = true;
        sparkstrength$professorRefreshPage();
    }

    @Unique
    private void sparkstrength$professorOpenPlayerSelection() {
        sparkstrength$professorSelectedTarget = null;
        sparkstrength$professorSelectingSerum = false;
        sparkstrength$professorRefreshPage();
    }

    @Unique
    private void sparkstrength$professorRefreshPage() {
        if (sparkstrength$professorSelectingSerum) {
            sparkstrength$professorRefreshSerumSelection();
            return;
        }

        int totalPages = PlayerPageLayout.getTotalPageCount(sparkstrength$professorPlayerWidgets.size());
        if (sparkstrength$professorCurrentPage < 0) {
            sparkstrength$professorCurrentPage = 0;
        }
        if (sparkstrength$professorCurrentPage >= totalPages) {
            sparkstrength$professorCurrentPage = totalPages - 1;
        }
        PlayerSelectionPageState.setProfessorPage(sparkstrength$professorCurrentPage);

        int startIndex = sparkstrength$professorCurrentPage * PlayerPageLayout.PLAYERS_PER_PAGE;
        int endIndex = Math.min(startIndex + PlayerPageLayout.PLAYERS_PER_PAGE, sparkstrength$professorPlayerWidgets.size());
        int visibleCount = endIndex - startIndex;
        int y = PlayerPageLayout.getPlayerRowY(this.height);
        boolean showPrevious = sparkstrength$professorCurrentPage > 0;
        boolean showNext = sparkstrength$professorCurrentPage < totalPages - 1;
        int groupStartX = PlayerPageLayout.getCenteredGroupStartX(this.width, visibleCount, showPrevious, showNext);
        int playerStartX = groupStartX + (showPrevious ? PlayerPageLayout.SLOT_APART : 0);

        for (int i = 0; i < sparkstrength$professorPlayerWidgets.size(); i++) {
            ProfessorRemoteFeedPlayerWidget widget = sparkstrength$professorPlayerWidgets.get(i);
            boolean visible = i >= startIndex && i < endIndex;
            widget.visible = visible;
            widget.active = visible;
            if (visible) {
                int visibleIndex = i - startIndex;
                widget.setX(playerStartX + visibleIndex * PlayerPageLayout.SLOT_APART);
                widget.setY(y);
            }
        }

        for (ProfessorSerumButtonWidget widget : sparkstrength$professorSerumWidgets) {
            widget.visible = false;
            widget.active = false;
        }
        if (sparkstrength$professorBackWidget != null) {
            sparkstrength$professorBackWidget.visible = false;
            sparkstrength$professorBackWidget.active = false;
        }
        if (sparkstrength$professorPreviousPageWidget != null) {
            sparkstrength$professorPreviousPageWidget.visible = showPrevious;
            sparkstrength$professorPreviousPageWidget.active = showPrevious;
            sparkstrength$professorPreviousPageWidget.setX(groupStartX);
            sparkstrength$professorPreviousPageWidget.setY(y);
        }
        if (sparkstrength$professorNextPageWidget != null) {
            sparkstrength$professorNextPageWidget.visible = showNext;
            sparkstrength$professorNextPageWidget.active = showNext;
            sparkstrength$professorNextPageWidget.setX(playerStartX + visibleCount * PlayerPageLayout.SLOT_APART);
            sparkstrength$professorNextPageWidget.setY(y);
        }
    }

    @Unique
    private void sparkstrength$professorRefreshSerumSelection() {
        int y = PlayerPageLayout.getPlayerRowY(this.height);
        int totalSlots = ProfessorSerumType.BACKPACK_ORDER.size() + 1;
        int groupStartX = this.width / 2 - totalSlots * PlayerPageLayout.SLOT_APART / 2
                + PlayerPageLayout.SLOT_X_OFFSET;
        UUID targetUuid = sparkstrength$professorSelectedTarget;

        for (ProfessorRemoteFeedPlayerWidget widget : sparkstrength$professorPlayerWidgets) {
            widget.visible = false;
            widget.active = false;
        }
        if (sparkstrength$professorPreviousPageWidget != null) {
            sparkstrength$professorPreviousPageWidget.visible = false;
            sparkstrength$professorPreviousPageWidget.active = false;
        }
        if (sparkstrength$professorNextPageWidget != null) {
            sparkstrength$professorNextPageWidget.visible = false;
            sparkstrength$professorNextPageWidget.active = false;
        }
        if (sparkstrength$professorBackWidget != null) {
            sparkstrength$professorBackWidget.visible = true;
            sparkstrength$professorBackWidget.active = true;
            sparkstrength$professorBackWidget.setX(groupStartX);
            sparkstrength$professorBackWidget.setY(y);
        }

        for (int i = 0; i < sparkstrength$professorSerumWidgets.size(); i++) {
            ProfessorSerumButtonWidget widget = sparkstrength$professorSerumWidgets.get(i);
            if (targetUuid == null) {
                widget.visible = false;
                widget.active = false;
                continue;
            }

            widget.setTargetUuid(targetUuid);
            widget.visible = true;
            widget.active = true;
            widget.setX(groupStartX + (i + 1) * PlayerPageLayout.SLOT_APART);
            widget.setY(y);
        }
    }
}
