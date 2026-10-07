package annina.sparkstrength.client.tablet;

import annina.sparkstrength.client.screen.tablet.TabletClientState;
import annina.sparkstrength.network.tablet.TabletSnapshot;
import annina.sparkstrength.tablet.TabletAccess;
import annina.sparkstrength.tablet.TabletRules;
import annina.sparkstrength.tablet.TabletShopRules;
import dev.doctor4t.wathe.api.event.GetInstinctHighlight;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.UUID;

/**
 * Tablet highlights use Wathe's outline event without requiring the instinct key.
 * 平板高亮使用 Wathe 描边事件，但不要求按下本能键。
 */
public final class TabletClientHighlights {
    private static final int TABLET_PRIORITY = 70;
    private static final int SUSPECT_PRIORITY = 80;

    private TabletClientHighlights() {
    }

    public static void register() {
        GetInstinctHighlight.EVENT.register(TabletClientHighlights::highlight);
    }

    private static GetInstinctHighlight.HighlightResult highlight(Entity target) {
        ClientPlayerEntity viewer = MinecraftClient.getInstance().player;
        if (viewer == null || !(target instanceof PlayerEntity targetPlayer) || targetPlayer == viewer) {
            return null;
        }
        if (!TabletAccess.hasTabletInHotbar(viewer)) {
            return null;
        }
        if (!GameFunctions.isPlayerPlayingAndAlive(targetPlayer)) {
            return null;
        }

        TabletSnapshot snapshot = TabletClientState.snapshot();
        UUID targetUuid = targetPlayer.getUuid();
        boolean targetHasTablet = snapshot.connections().stream().anyMatch(row -> row.uuid().equals(targetUuid));
        boolean targetSuspect = snapshot.suspects().stream().anyMatch(row -> row.uuid().equals(targetUuid));

        // Periodic suspect reveals are intentional through-wall tablet ESP.
        // 嫌疑人的周期透视不受墙体视线阻挡。
        if (targetSuspect && isSuspectRevealWindow(viewer)) {
            return GetInstinctHighlight.HighlightResult.always(
                    TabletShopRules.SUSPECT_HIGHLIGHT_COLOR,
                    SUSPECT_PRIORITY
            );
        }
        if (!viewer.canSee(targetPlayer)) {
            return null;
        }
        if (targetSuspect && targetHasTablet) {
            return GetInstinctHighlight.HighlightResult.always(
                    TabletShopRules.SUSPECT_HIGHLIGHT_COLOR,
                    SUSPECT_PRIORITY
            );
        }
        if (targetHasTablet) {
            return GetInstinctHighlight.HighlightResult.always(
                    TabletShopRules.TABLET_HIGHLIGHT_COLOR,
                    TABLET_PRIORITY
            );
        }
        return null;
    }

    private static boolean isSuspectRevealWindow(PlayerEntity viewer) {
        int tick = viewer.age % TabletRules.SUSPECT_REVEAL_INTERVAL_TICKS;
        return tick < TabletRules.SUSPECT_REVEAL_TICKS;
    }
}
