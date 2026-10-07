package annina.sparkstrength.role.corruptcop;

import annina.sparkstrength.component.corruptcop.CorruptCopAbilityComponent;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;

/**
 * Server authority for the Corrupt Cop ability toggle.
 * 黑警主动技能开关的服务端权威入口。
 */
public final class CorruptCopAbilityService {
    private CorruptCopAbilityService() {
    }

    public static boolean toggle(ServerPlayerEntity player) {
        if (player == null
                || !GameFunctions.isPlayerAliveAndSurvival(player)
                || SwallowedPlayerComponent.isPlayerSwallowed(player)) {
            return false;
        }

        Role role = GameWorldComponent.KEY.get(player.getServerWorld()).getRole(player);
        if (!CorruptCopRules.isCorruptCop(role)) {
            return false;
        }

        CorruptCopAbilityComponent component = CorruptCopAbilityComponent.KEY.get(player);
        component.setActive(CorruptCopRules.nextAbilityActive(true, component.isActive()));
        return true;
    }

    public static void reset(ServerPlayerEntity player) {
        if (player != null) {
            CorruptCopAbilityComponent.KEY.get(player).reset();
        }
    }
}
