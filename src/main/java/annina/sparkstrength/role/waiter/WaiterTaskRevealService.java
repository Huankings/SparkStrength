package annina.sparkstrength.role.waiter;

import annina.sparkstrength.component.waiter.WaiterTaskRevealComponent;
import dev.doctor4t.wathe.api.event.TaskComplete;
import net.minecraft.server.network.ServerPlayerEntity;

/** 服务员“任务完成后透视目标”能力的服务端事件注册。 */
public final class WaiterTaskRevealService {
    private static boolean registered;

    private WaiterTaskRevealService() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        TaskComplete.EVENT.register(WaiterTaskRevealService::onTaskComplete);
    }

    private static void onTaskComplete(ServerPlayerEntity player, dev.doctor4t.wathe.cca.PlayerMoodComponent.Task taskType) {
        // 任何玩家完成任务都会记录一次；客户端只给服务员及其服务员伪装显示。
        WaiterTaskRevealComponent.KEY.get(player).startReveal();
    }

    public static void reset(ServerPlayerEntity player) {
        WaiterTaskRevealComponent.KEY.get(player).reset();
    }
}
