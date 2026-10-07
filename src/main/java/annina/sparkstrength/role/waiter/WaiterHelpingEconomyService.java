package annina.sparkstrength.role.waiter;

import dev.doctor4t.wathe.cca.PlayerShopComponent;
import net.minecraft.server.network.ServerPlayerEntity;

/** 服务员成功帮助其他玩家完成任务时的额外金币奖励。 */
public final class WaiterHelpingEconomyService {
    public static final int HELP_REWARD = 50;

    private WaiterHelpingEconomyService() {
    }

    public static void reward(ServerPlayerEntity waiter) {
        PlayerShopComponent.KEY.get(waiter).addToBalance(HELP_REWARD);
    }
}
