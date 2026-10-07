package annina.sparkstrength.role.engineer;

import annina.sparkstrength.SparkStrengthEntities;
import annina.sparkstrength.component.engineer.EngineerStunnedPlayerComponent;
import annina.sparkstrength.entity.CaptureDeviceEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

/**
 * Round-state cleanup entrypoint for capture devices.
 * 捕捉装置的局状态清理入口。
 */
public final class EngineerCaptureDeviceService {
    private EngineerCaptureDeviceService() {
    }

    public static void clearPlayer(ServerPlayerEntity player) {
        EngineerStunnedPlayerComponent.KEY.get(player).clear();
    }

    public static void clearRoundState(ServerWorld world) {
        for (CaptureDeviceEntity entity : world.getEntitiesByType(
                SparkStrengthEntities.captureDevice(),
                captureDevice -> true
        )) {
            entity.discard();
        }
        for (ServerPlayerEntity player : world.getPlayers()) {
            clearPlayer(player);
        }
    }
}
