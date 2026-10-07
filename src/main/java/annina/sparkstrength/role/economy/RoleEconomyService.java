package annina.sparkstrength.role.economy;

import annina.sparkstrength.compat.SparkTraitsCompat;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.event.CanSeeMoney;
import dev.doctor4t.wathe.api.event.TaskComplete;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Server-side economy hooks for SparkStrength role enhancements.
 * SparkStrength 角色增强的服务端金币逻辑。
 */
public final class RoleEconomyService {
    private static boolean registered;

    private RoleEconomyService() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        CanSeeMoney.EVENT.register(RoleEconomyService::canSeeMoney);
        TaskComplete.EVENT.register((player, taskType) -> onTaskComplete(player));
    }

    public static void assignForRole(ServerPlayerEntity player, Role role) {
        if (RoleEconomyRules.shouldInitializeGoodMoney(role)) {
            PlayerShopComponent.KEY.get(player).setBalance(RoleEconomyRules.INITIAL_GOOD_ROLE_MONEY);
        }
    }

    public static void onTaskComplete(ServerPlayerEntity player) {
        Role role = GameWorldComponent.KEY.get(player.getServerWorld()).getRole(player);
        if (RoleEconomyRules.earnsTaskMoney(role) && !SparkTraitsCompat.hasImpostor(player)) {
            PlayerShopComponent.KEY.get(player).addToBalance(RoleEconomyRules.TASK_MONEY_REWARD);
        }
    }

    public static boolean isMoneyVisible(PlayerEntity player) {
        Role role = GameWorldComponent.KEY.get(player.getWorld()).getRole(player);
        return RoleEconomyRules.isGoodMoneyRole(role) || SparkTraitsCompat.hasImpostor(player);
    }

    static @Nullable CanSeeMoney.Result moneyVisibilityResult(@Nullable Role role) {
        return RoleEconomyRules.isGoodMoneyRole(role) ? CanSeeMoney.Result.ALLOW : null;
    }

    private static CanSeeMoney.Result canSeeMoney(PlayerEntity player) {
        if (player == null || !GameFunctions.isPlayerPlayingAndAlive(player)) {
            return null;
        }
        return isMoneyVisible(player) ? CanSeeMoney.Result.ALLOW : null;
    }
}
