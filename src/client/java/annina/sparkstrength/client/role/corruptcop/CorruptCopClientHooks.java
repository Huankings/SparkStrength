package annina.sparkstrength.client.role.corruptcop;

import annina.sparkstrength.role.corruptcop.CorruptCopRules;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.event.GetInstinctHighlight;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Client-side instinct presentation hooks for Corrupt Cop.
 * 黑警本能视觉表现的客户端挂钩。
 */
public final class CorruptCopClientHooks {
    private CorruptCopClientHooks() {
    }

    public static void register() {
        GetInstinctHighlight.EVENT.register(CorruptCopClientHooks::highlight);
    }

    public static boolean usesKillerStyleInstinctLight() {
        ClientPlayerEntity viewer = MinecraftClient.getInstance().player;
        if (viewer == null) {
            return false;
        }

        Role role = GameWorldComponent.KEY.get(viewer.getWorld()).getRole(viewer);
        return CorruptCopRules.usesKillerStyleInstinctLight(
                role,
                WatheClient.isInstinctEnabled(),
                GameFunctions.isPlayerPlayingAndAlive(viewer)
        );
    }

    private static GetInstinctHighlight.HighlightResult highlight(Entity target) {
        ClientPlayerEntity viewer = MinecraftClient.getInstance().player;
        if (viewer == null || !(target instanceof PlayerEntity targetPlayer)) {
            return null;
        }

        GameWorldComponent gameComponent = GameWorldComponent.KEY.get(viewer.getWorld());
        Role role = gameComponent.getRole(viewer);
        return CorruptCopRules.instinctHighlight(
                role,
                GameFunctions.isPlayerPlayingAndAlive(viewer),
                GameFunctions.isPlayerSpectatingOrCreative(viewer),
                viewer.getUuid().equals(targetPlayer.getUuid()),
                GameFunctions.isPlayerPlayingAndAlive(targetPlayer),
                GameFunctions.isPlayerSpectatingOrCreative(targetPlayer),
                targetPlayer.isInvisible()
        );
    }
}
