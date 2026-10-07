package annina.sparkstrength.client.mixin.phantom;

import annina.sparkstrength.client.ui.common.PlayerPageLayout;
import annina.sparkstrength.client.ui.common.PlayerPageSwitchWidget;
import annina.sparkstrength.client.ui.common.PlayerSelectionPageState;
import annina.sparkstrength.client.ui.phantom.PhantomBackpackPlayerWidget;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedHandledScreen;
import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedInventoryScreen;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Items;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.Text;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;
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
 * 给 Wathe 受限背包界面追加幽灵“让其他玩家隐身”头像栏。
 *
 * <p>客户端列表先过滤自己、死亡/旁观、被饕餮吞噬的玩家，保证界面符合需求；
 * 服务端仍会在收包后完整重判一遍，避免客户端状态延迟或改包绕过规则。</p>
 */
@Mixin(LimitedInventoryScreen.class)
public abstract class PhantomBackpackInventoryScreenMixin extends LimitedHandledScreen<PlayerScreenHandler> {
    @Shadow @Final public ClientPlayerEntity player;

    @Unique private final List<PhantomBackpackPlayerWidget> sparkstrength$phantomPlayerWidgets = new ArrayList<>();
    @Unique private PlayerPageSwitchWidget sparkstrength$phantomPreviousPageWidget;
    @Unique private PlayerPageSwitchWidget sparkstrength$phantomNextPageWidget;
    @Unique private int sparkstrength$phantomCurrentPage;

    public PhantomBackpackInventoryScreenMixin(PlayerScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Inject(method = "init", at = @At("HEAD"))
    private void sparkstrength$phantomAddInvisibilityButtons(CallbackInfo ci) {
        if (player == null || player.getWorld() == null) {
            return;
        }

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        if (!gameWorld.isRole(player, Noellesroles.PHANTOM)) {
            return;
        }

        sparkstrength$phantomAddPlayerSelectionUI();
    }

    @Unique
    private void sparkstrength$phantomAddPlayerSelectionUI() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.player.networkHandler == null) {
            return;
        }

        List<UUID> targetUuids = sparkstrength$phantomCollectTargets(client.player);
        int y = PlayerPageLayout.getPlayerRowY(this.height);
        sparkstrength$phantomPlayerWidgets.clear();
        sparkstrength$phantomCurrentPage = PlayerSelectionPageState.getPhantomPage();

        for (UUID targetUuid : targetUuids) {
            PlayerListEntry playerListEntry = client.player.networkHandler.getPlayerListEntry(targetUuid);
            PhantomBackpackPlayerWidget child = new PhantomBackpackPlayerWidget(
                    0,
                    y,
                    targetUuid,
                    playerListEntry
            );
            sparkstrength$phantomPlayerWidgets.add(child);
            addDrawableChild(child);
        }

        sparkstrength$phantomPreviousPageWidget = addDrawableChild(new PlayerPageSwitchWidget(
                0,
                y,
                Items.PURPLE_DYE.getDefaultStack(),
                Text.translatable("ui.sparkstrength.pagination.previous"),
                button -> {
                    sparkstrength$phantomCurrentPage--;
                    sparkstrength$phantomRefreshPage();
                }
        ));
        sparkstrength$phantomNextPageWidget = addDrawableChild(new PlayerPageSwitchWidget(
                0,
                y,
                Items.LIME_DYE.getDefaultStack(),
                Text.translatable("ui.sparkstrength.pagination.next"),
                button -> {
                    sparkstrength$phantomCurrentPage++;
                    sparkstrength$phantomRefreshPage();
                }
        ));

        sparkstrength$phantomRefreshPage();
    }

    @Unique
    private List<UUID> sparkstrength$phantomCollectTargets(ClientPlayerEntity localPlayer) {
        MinecraftClient client = MinecraftClient.getInstance();
        List<UUID> targetUuids = new ArrayList<>();
        for (UUID targetUuid : localPlayer.networkHandler.getPlayerUuids()) {
            if (targetUuid.equals(localPlayer.getUuid())) {
                continue;
            }

            PlayerEntity targetPlayer = client.world == null ? null : client.world.getPlayerByUuid(targetUuid);
            if (targetPlayer == null
                    || !GameFunctions.isPlayerPlayingAndAlive(targetPlayer)
                    || !GameFunctions.isPlayerAliveAndSurvival(targetPlayer)
                    || SwallowedPlayerComponent.isPlayerSwallowed(targetPlayer)) {
                continue;
            }

            targetUuids.add(targetUuid);
        }
        return targetUuids;
    }

    @Unique
    private void sparkstrength$phantomRefreshPage() {
        int totalPages = PlayerPageLayout.getTotalPageCount(sparkstrength$phantomPlayerWidgets.size());
        if (sparkstrength$phantomCurrentPage < 0) {
            sparkstrength$phantomCurrentPage = 0;
        }
        if (sparkstrength$phantomCurrentPage >= totalPages) {
            sparkstrength$phantomCurrentPage = totalPages - 1;
        }
        PlayerSelectionPageState.setPhantomPage(sparkstrength$phantomCurrentPage);

        int startIndex = sparkstrength$phantomCurrentPage * PlayerPageLayout.PLAYERS_PER_PAGE;
        int endIndex = Math.min(startIndex + PlayerPageLayout.PLAYERS_PER_PAGE, sparkstrength$phantomPlayerWidgets.size());
        int visibleCount = endIndex - startIndex;
        int y = PlayerPageLayout.getPlayerRowY(this.height);
        boolean showPrevious = sparkstrength$phantomCurrentPage > 0;
        boolean showNext = sparkstrength$phantomCurrentPage < totalPages - 1;
        int groupStartX = PlayerPageLayout.getCenteredGroupStartX(this.width, visibleCount, showPrevious, showNext);
        int playerStartX = groupStartX + (showPrevious ? PlayerPageLayout.SLOT_APART : 0);

        for (int i = 0; i < sparkstrength$phantomPlayerWidgets.size(); i++) {
            PhantomBackpackPlayerWidget widget = sparkstrength$phantomPlayerWidgets.get(i);
            boolean visible = i >= startIndex && i < endIndex;
            widget.visible = visible;
            widget.active = visible;
            if (visible) {
                int visibleIndex = i - startIndex;
                widget.setX(playerStartX + visibleIndex * PlayerPageLayout.SLOT_APART);
                widget.setY(y);
            }
        }

        if (sparkstrength$phantomPreviousPageWidget != null) {
            sparkstrength$phantomPreviousPageWidget.visible = showPrevious;
            sparkstrength$phantomPreviousPageWidget.active = showPrevious;
            sparkstrength$phantomPreviousPageWidget.setX(groupStartX);
            sparkstrength$phantomPreviousPageWidget.setY(y);
        }
        if (sparkstrength$phantomNextPageWidget != null) {
            sparkstrength$phantomNextPageWidget.visible = showNext;
            sparkstrength$phantomNextPageWidget.active = showNext;
            sparkstrength$phantomNextPageWidget.setX(playerStartX + visibleCount * PlayerPageLayout.SLOT_APART);
            sparkstrength$phantomNextPageWidget.setY(y);
        }
    }
}
