package annina.sparkstrength.client.role.waiter;

import annina.sparkstrength.component.waiter.WaiterTaskRevealComponent;
import annina.sparkstrength.role.coroner.CoronerService;
import dev.doctor4t.wathe.api.event.GetInstinctHighlight;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.Noellesroles;

/** 服务员及验尸官服务员伪装的任务完成透视客户端高亮。 */
public final class WaiterTaskRevealClientHooks {
    private static boolean registered;

    private WaiterTaskRevealClientHooks() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        GetInstinctHighlight.EVENT.register(WaiterTaskRevealClientHooks::highlight);
    }

    private static GetInstinctHighlight.HighlightResult highlight(Entity entity) {
        ClientPlayerEntity viewer = MinecraftClient.getInstance().player;
        if (viewer == null || !(entity instanceof PlayerEntity target) || viewer == target
                || !GameFunctions.isPlayerPlayingAndAlive(viewer)
                || !GameFunctions.isPlayerPlayingAndAlive(target)) {
            return null;
        }

        GameWorldComponent game = GameWorldComponent.KEY.get(viewer.getWorld());
        boolean waiter = game.isRole(viewer, Noellesroles.WAITER)
                || CoronerService.hasWaiterDisguise(viewer);
        if (!waiter || !WaiterTaskRevealComponent.KEY.get(target).isRevealed()) {
            return null;
        }

        // always 表示无需按本能键，并且不检查视线，因此可以隔墙透视。
        return GetInstinctHighlight.HighlightResult.always(Noellesroles.WAITER.color(), 70);
    }
}
