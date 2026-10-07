package annina.sparkstrength.client.role.economy;

import annina.sparkstrength.role.economy.RoleEconomyService;
import dev.doctor4t.wathe.api.event.CanSeeMoney;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Client-side money visibility hook.
 * 客户端金币显示权限挂钩。
 */
public final class RoleEconomyClientHooks {
    private RoleEconomyClientHooks() {
    }

    public static void register() {
        CanSeeMoney.EVENT.register(RoleEconomyClientHooks::canSeeMoney);
    }

    private static CanSeeMoney.Result canSeeMoney(PlayerEntity player) {
        if (player == null || !GameFunctions.isPlayerPlayingAndAlive(player)) {
            return null;
        }
        return RoleEconomyService.isMoneyVisible(player) ? CanSeeMoney.Result.ALLOW : null;
    }
}
