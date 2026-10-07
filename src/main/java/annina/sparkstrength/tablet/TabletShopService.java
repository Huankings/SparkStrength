package annina.sparkstrength.tablet;

import annina.sparkstrength.SparkStrengthItems;
import annina.sparkstrength.compat.SparkTraitsCompat;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.event.BuildShopEntries;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.List;

/**
 * Adds the SparkStrength tablet to eligible roles' shop.
 * 给符合条件的角色追加 SparkStrength 平板商店项。
 */
public final class TabletShopService {
    private static boolean registered;

    private TabletShopService() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        BuildShopEntries.EVENT.register(TabletShopService::buildShopEntries);
    }

    public static boolean isTabletEconomyEligible(PlayerEntity player) {
        if (player == null) {
            return false;
        }
        Role role = GameWorldComponent.KEY.get(player.getWorld()).getRole(player);
        // SparkTraits 是可选兼容：装了 impostor/叛徒词条时也能买平板；没装时自然返回 false。
        return TabletShopRules.canBuyTabletRole(role) || SparkTraitsCompat.hasImpostor(player);
    }

    private static void buildShopEntries(PlayerEntity player, BuildShopEntries.ShopContext context) {
        if (!isTabletEconomyEligible(player)) {
            return;
        }
        context.addEntry(new ShopEntry.Builder(
                TabletShopRules.TABLET_ENTRY_ID,
                tabletDisplayStack(),
                TabletShopRules.TABLET_PRICE,
                ShopEntry.Type.TOOL
        ).actualStack(SparkStrengthItems.tablet().getDefaultStack()).stock(1).build());
    }

    private static ItemStack tabletDisplayStack() {
        ItemStack stack = SparkStrengthItems.tablet().getDefaultStack();
        stack.set(DataComponentTypes.ITEM_NAME, Text.translatable("shop.sparkstrength.tablet"));
        stack.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                Text.translatable("shop.sparkstrength.tablet.description")
                        .styled(style -> style.withColor(0x808080).withItalic(false))
        )));
        return stack;
    }
}
