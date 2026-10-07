package annina.sparkstrength.role.attendant;

import annina.sparkstrength.SparkStrengthItems;
import dev.doctor4t.wathe.api.Role;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Gives Attendants their SparkStrength starter flashlight.
 * 给乘务员发放 SparkStrength 开局手电筒。
 */
public final class AttendantFlashlightService {
    private AttendantFlashlightService() {
    }

    public static void assignForRole(ServerPlayerEntity player, Role role) {
        if (AttendantRules.shouldGiveStarterFlashlight(role, hasFlashlight(player))) {
            player.giveItemStack(new ItemStack(SparkStrengthItems.flashlight()));
        }
    }

    private static boolean hasFlashlight(ServerPlayerEntity player) {
        // 角色分配事件在某些流程里可能重复触发；先检查背包，避免刷出多个开局手电。
        for (int slot = 0; slot < player.getInventory().size(); slot++) {
            if (player.getInventory().getStack(slot).isOf(SparkStrengthItems.flashlight())) {
                return true;
            }
        }
        return false;
    }
}
