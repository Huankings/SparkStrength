package annina.sparkstrength.client.mixin.coroner;

import annina.sparkstrength.client.ui.common.PlayerPageLayout;
import annina.sparkstrength.client.ui.common.PlayerPageSwitchWidget;
import annina.sparkstrength.client.ui.common.PlayerSelectionPageState;
import annina.sparkstrength.client.ui.coroner.CoronerPlayerWidget;
import annina.sparkstrength.component.coroner.CoronerPlayerComponent;
import annina.sparkstrength.role.coroner.CoronerRules;
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
 * 给 Wathe 受限背包界面追加验尸官“尸体身份变形”头像栏。
 *
 * <p>和召集者保持同一交互模型：自己头像永远存在，刚开局只能点自己；
 * 每用采尸袋收走一具尸体，就会多一个尸体原主头像按钮。</p>
 */
@Mixin(LimitedInventoryScreen.class)
public abstract class CoronerInventoryScreenMixin extends LimitedHandledScreen<PlayerScreenHandler> {
    @Shadow @Final public ClientPlayerEntity player;

    @Unique private final List<CoronerPlayerWidget> sparkstrength$coronerPlayerWidgets = new ArrayList<>();
    @Unique private PlayerPageSwitchWidget sparkstrength$coronerPreviousPageWidget;
    @Unique private PlayerPageSwitchWidget sparkstrength$coronerNextPageWidget;
    @Unique private int sparkstrength$coronerCurrentPage;

    public CoronerInventoryScreenMixin(PlayerScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Inject(method = "init", at = @At("HEAD"))
    private void sparkstrength$coronerAddMorphButtons(CallbackInfo ci) {
        if (player == null || player.getWorld() == null) {
            return;
        }

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        if (!CoronerRules.isCoroner(gameWorld.getRole(player))) {
            return;
        }

        sparkstrength$coronerAddPlayerSelectionUI();
    }

    @Unique
    private void sparkstrength$coronerAddPlayerSelectionUI() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.player.networkHandler == null) {
            return;
        }

        List<UUID> targetUuids = sparkstrength$coronerCollectTargets(player);
        int y = PlayerPageLayout.getPlayerRowY(this.height);
        sparkstrength$coronerPlayerWidgets.clear();
        sparkstrength$coronerCurrentPage = PlayerSelectionPageState.getCoronerPage();

        for (UUID targetUuid : targetUuids) {
            PlayerListEntry playerListEntry = client.player.networkHandler.getPlayerListEntry(targetUuid);
            CoronerPlayerWidget child = new CoronerPlayerWidget(
                    0,
                    y,
                    targetUuid,
                    targetUuid.equals(player.getUuid()),
                    playerListEntry
            );
            sparkstrength$coronerPlayerWidgets.add(child);
            addDrawableChild(child);
        }

        sparkstrength$coronerPreviousPageWidget = addDrawableChild(new PlayerPageSwitchWidget(
                0,
                y,
                Items.PURPLE_DYE.getDefaultStack(),
                Text.translatable("ui.sparkstrength.pagination.previous"),
                button -> {
                    sparkstrength$coronerCurrentPage--;
                    sparkstrength$coronerRefreshPage();
                }
        ));
        sparkstrength$coronerNextPageWidget = addDrawableChild(new PlayerPageSwitchWidget(
                0,
                y,
                Items.LIME_DYE.getDefaultStack(),
                Text.translatable("ui.sparkstrength.pagination.next"),
                button -> {
                    sparkstrength$coronerCurrentPage++;
                    sparkstrength$coronerRefreshPage();
                }
        ));

        sparkstrength$coronerRefreshPage();
    }

    @Unique
    private List<UUID> sparkstrength$coronerCollectTargets(ClientPlayerEntity player) {
        List<UUID> targetUuids = new ArrayList<>();
        targetUuids.add(player.getUuid());
        targetUuids.addAll(CoronerPlayerComponent.KEY.get(player).unlockedBodyUuids());
        return targetUuids;
    }

    @Unique
    private void sparkstrength$coronerRefreshPage() {
        int totalPages = PlayerPageLayout.getTotalPageCount(sparkstrength$coronerPlayerWidgets.size());
        if (sparkstrength$coronerCurrentPage < 0) {
            sparkstrength$coronerCurrentPage = 0;
        }
        if (sparkstrength$coronerCurrentPage >= totalPages) {
            sparkstrength$coronerCurrentPage = totalPages - 1;
        }
        PlayerSelectionPageState.setCoronerPage(sparkstrength$coronerCurrentPage);

        int startIndex = sparkstrength$coronerCurrentPage * PlayerPageLayout.PLAYERS_PER_PAGE;
        int endIndex = Math.min(startIndex + PlayerPageLayout.PLAYERS_PER_PAGE, sparkstrength$coronerPlayerWidgets.size());
        int visibleCount = endIndex - startIndex;
        int y = PlayerPageLayout.getPlayerRowY(this.height);
        boolean showPrevious = sparkstrength$coronerCurrentPage > 0;
        boolean showNext = sparkstrength$coronerCurrentPage < totalPages - 1;
        int groupStartX = PlayerPageLayout.getCenteredGroupStartX(this.width, visibleCount, showPrevious, showNext);
        int playerStartX = groupStartX + (showPrevious ? PlayerPageLayout.SLOT_APART : 0);

        for (int i = 0; i < sparkstrength$coronerPlayerWidgets.size(); i++) {
            CoronerPlayerWidget widget = sparkstrength$coronerPlayerWidgets.get(i);
            boolean visible = i >= startIndex && i < endIndex;
            widget.visible = visible;
            widget.active = visible;
            if (visible) {
                int visibleIndex = i - startIndex;
                widget.setX(playerStartX + visibleIndex * PlayerPageLayout.SLOT_APART);
                widget.setY(y);
            }
        }

        if (sparkstrength$coronerPreviousPageWidget != null) {
            sparkstrength$coronerPreviousPageWidget.visible = showPrevious;
            sparkstrength$coronerPreviousPageWidget.active = showPrevious;
            sparkstrength$coronerPreviousPageWidget.setX(groupStartX);
            sparkstrength$coronerPreviousPageWidget.setY(y);
        }
        if (sparkstrength$coronerNextPageWidget != null) {
            sparkstrength$coronerNextPageWidget.visible = showNext;
            sparkstrength$coronerNextPageWidget.active = showNext;
            sparkstrength$coronerNextPageWidget.setX(playerStartX + visibleCount * PlayerPageLayout.SLOT_APART);
            sparkstrength$coronerNextPageWidget.setY(y);
        }
    }
}
