package annina.sparkstrength.client.role.morphling;

import annina.sparkstrength.component.morphling.MorphMarkPlayerComponent;
import annina.sparkstrength.role.morphling.MorphlingRules;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.event.GetInstinctHighlight;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

/**
 * 变形怪增强的客户端事件。
 */
public final class MorphlingClientHooks {
    private static boolean registered;

    private MorphlingClientHooks() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        GetInstinctHighlight.EVENT.register(MorphlingClientHooks::markedPlayerHighlight);
    }

    private static GetInstinctHighlight.HighlightResult markedPlayerHighlight(Entity entity) {
        if (!(entity instanceof PlayerEntity target)) {
            return null;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity localPlayer = client.player;
        if (localPlayer == null
                || target == localPlayer
                || !GameFunctions.isPlayerPlayingAndAlive(localPlayer)
                || !GameFunctions.isPlayerAliveAndSurvival(localPlayer)) {
            return null;
        }

        GameWorldComponent game = GameWorldComponent.KEY.get(localPlayer.getWorld());
        Role localRole = game.getRole(localPlayer);
        if (!MorphlingRules.isMorphling(localRole)) {
            return null;
        }

        MorphMarkPlayerComponent component = MorphMarkPlayerComponent.KEY.get(target);
        if (!component.hasMark() || !component.isMarkedBy(localPlayer.getUuid())) {
            return null;
        }

        if (WatheClient.isInstinctEnabled()) {
            // 按住本能键时交还给 Wathe 默认杀手本能，让阵营颜色和其它本能规则自然覆盖标记高亮。
            return null;
        }
        return GetInstinctHighlight.HighlightResult.always(
                localRole.color(),
                MorphlingRules.MARK_HIGHLIGHT_PRIORITY
        );
    }
}
