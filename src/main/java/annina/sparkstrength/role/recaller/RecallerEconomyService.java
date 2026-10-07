package annina.sparkstrength.role.recaller;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

/**
 * 回溯者的定时被动金币收入。
 *
 * <p>这套逻辑参考 Wathe 杀手阵营的被动收入：在服务端世界 tick 中按固定间隔发钱。
 * 与完成任务奖励不同，它不依赖玩家完成任务，只要求玩家当前仍是存活的回溯者。</p>
 */
public final class RecallerEconomyService {
    private RecallerEconomyService() {
    }

    public static void tick(ServerWorld world) {
        long worldTime = world.getTime();
        if (RecallerShopRules.PASSIVE_INCOME_INTERVAL_TICKS <= 0
                || worldTime % RecallerShopRules.PASSIVE_INCOME_INTERVAL_TICKS != 0) {
            return;
        }

        GameWorldComponent game = GameWorldComponent.KEY.get(world);
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (!GameFunctions.isPlayerPlayingAndAlive(player)) {
                continue;
            }

            Role role = game.getRole(player);
            if (!RecallerShopRules.isRecaller(role)) {
                continue;
            }

            PlayerShopComponent shop = PlayerShopComponent.KEY.get(player);
            int amountToAdd = RecallerShopRules.passiveIncomeToAdd(shop.getBalance());
            if (amountToAdd > 0) {
                shop.addToBalance(amountToAdd);
            }
        }
    }
}
