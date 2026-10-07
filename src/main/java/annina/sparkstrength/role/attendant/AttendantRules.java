package annina.sparkstrength.role.attendant;

import dev.doctor4t.wathe.api.Role;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * Pure role predicates for Attendant starter equipment.
 * 乘务员开局装备的纯规则。
 */
public final class AttendantRules {
    public static final Identifier ATTENDANT_ID = Identifier.of("noellesroles", "attendant");

    private AttendantRules() {
    }

    public static boolean startsWithFlashlight(@Nullable Role role) {
        return role != null && ATTENDANT_ID.equals(role.identifier());
    }

    public static boolean shouldGiveStarterFlashlight(@Nullable Role role, boolean alreadyHasFlashlight) {
        return startsWithFlashlight(role) && !alreadyHasFlashlight;
    }
}
