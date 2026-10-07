package annina.sparkstrength.role.coroner;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

/**
 * 验尸官的定时被动金币收入。
 *
 * <p>用户明确要求“照搬回溯者”，所以这里的间隔、单次金额和余额上限都直接使用
 * {@link CoronerRules} 中与回溯者一致的数值。</p>
 */
public final class CoronerEconomyService {
    private CoronerEconomyService() {
    }

    public static void tick(ServerWorld world) {
        long worldTime = world.getTime();
        if (CoronerRules.PASSIVE_INCOME_INTERVAL_TICKS <= 0
                || worldTime % CoronerRules.PASSIVE_INCOME_INTERVAL_TICKS != 0) {
            return;
        }

        GameWorldComponent game = GameWorldComponent.KEY.get(world);
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (!GameFunctions.isPlayerPlayingAndAlive(player)) {
                continue;
            }

            Role role = game.getRole(player);
            if (!CoronerRules.isCoroner(role)) {
                continue;
            }

            PlayerShopComponent shop = PlayerShopComponent.KEY.get(player);
            int amountToAdd = CoronerRules.passiveIncomeToAdd(shop.getBalance());
            if (amountToAdd > 0) {
                shop.addToBalance(amountToAdd);
            }
        }
    }
}
