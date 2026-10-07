package annina.sparkstrength.tablet;

import annina.sparkstrength.SparkStrengthItems;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.List;

/**
 * Centralizes tablet possession and meeting eligibility checks.
 * 集中处理平板持有和会议资格判断，避免各处规则漂移。
 */
public final class TabletAccess {
    private TabletAccess() {
    }

    public static boolean hasTabletInHotbar(PlayerEntity player) {
        if (player == null) {
            return false;
        }
        for (int slot = TabletRules.HOTBAR_START_SLOT; slot <= TabletRules.HOTBAR_END_SLOT; slot++) {
            ItemStack stack = player.getInventory().getStack(slot);
            if (stack.isOf(SparkStrengthItems.tablet())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isInGame(PlayerEntity player) {
        return player != null && GameWorldComponent.KEY.get(player.getWorld()).hasAnyRole(player.getUuid());
    }

    public static boolean isInGame(ServerWorld world, java.util.UUID uuid) {
        return GameWorldComponent.KEY.get(world).hasAnyRole(uuid);
    }

    public static boolean isAliveTabletParticipant(ServerPlayerEntity player) {
        return player != null && hasTabletInHotbar(player) && GameFunctions.isPlayerPlayingAndAlive(player);
    }

    public static List<ServerPlayerEntity> tabletHolders(ServerWorld world) {
        return world.getPlayers().stream()
                .filter(TabletAccess::hasTabletInHotbar)
                .toList();
    }

    public static List<ServerPlayerEntity> aliveTabletParticipants(ServerWorld world) {
        return world.getPlayers().stream()
                .filter(TabletAccess::isAliveTabletParticipant)
                .toList();
    }
}
