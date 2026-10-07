package annina.sparkstrength.tablet;

import annina.sparkstrength.role.corruptcop.CorruptCopRules;
import dev.doctor4t.wathe.api.Role;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * Shop and highlight rules for the SparkStrength tablet item.
 * SparkStrength 平板物品的商店与高亮规则。
 */
public final class TabletShopRules {
    public static final Identifier VIGILANTE_ID = Identifier.of("wathe", "vigilante");
    public static final String TABLET_ENTRY_ID = "sparkstrength_tablet";
    public static final int TABLET_PRICE = 150;
    public static final int TABLET_HIGHLIGHT_COLOR = 0x1B8AE5;
    public static final int SUSPECT_HIGHLIGHT_COLOR = 0xFF8C00;

    private TabletShopRules() {
    }

    public static boolean canBuyTabletRole(@Nullable Role role) {
        return isVigilante(role) || CorruptCopRules.isCorruptCop(role);
    }

    public static boolean isVigilante(@Nullable Role role) {
        return role != null && VIGILANTE_ID.equals(role.identifier());
    }
}
