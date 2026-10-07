package annina.sparkstrength.client.role.detective;

import annina.sparkstrength.component.detective.CriminologistPlayerComponent;
import annina.sparkstrength.role.detective.CriminologistRules;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.event.GetInstinctHighlight;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Client-side highlight hook for the Detective criminologist tracking reveal.
 * 侦探犯罪学家追踪显形的客户端高亮挂钩。
 */
public final class CriminologistClientHooks {
    private CriminologistClientHooks() {
    }

    public static void register() {
        GetInstinctHighlight.EVENT.register(CriminologistClientHooks::highlight);
    }

    private static GetInstinctHighlight.HighlightResult highlight(Entity target) {
        ClientPlayerEntity viewer = MinecraftClient.getInstance().player;
        if (viewer == null || !(target instanceof PlayerEntity targetPlayer)) {
            return null;
        }
        if (!GameFunctions.isPlayerPlayingAndAlive(viewer)) {
            return null;
        }

        GameWorldComponent gameComponent = GameWorldComponent.KEY.get(viewer.getWorld());
        Role role = gameComponent.getRole(viewer);
        if (!CriminologistRules.isDetective(role)) {
            return null;
        }

        CriminologistPlayerComponent component = CriminologistPlayerComponent.KEY.get(viewer);
        if (component.isCriminologistRevealing(targetPlayer.getUuid())) {
            return GetInstinctHighlight.HighlightResult.always(CriminologistRules.HIGHLIGHT_COLOR);
        }
        return null;
    }
}
