package annina.sparkstrength.role.recaller;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.event.BuildShopEntries;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;

/**
 * 给 NoellesRoles 回溯者追加专属商店。
 *
 * <p>回溯者出售的是原版物品，所以直接使用原版物品栈作为展示和实际购买物。
 * 这里没有调用 {@code stock(...)}，代表末影珍珠和紫颂果都不限量购买。</p>
 */
public final class RecallerShopService {
    private static boolean registered;

    private RecallerShopService() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        BuildShopEntries.EVENT.register(RecallerShopService::buildShopEntries);
    }

    private static void buildShopEntries(PlayerEntity player, BuildShopEntries.ShopContext context) {
        Role role = GameWorldComponent.KEY.get(player.getWorld()).getRole(player);
        if (!RecallerShopRules.canUseRecallerShop(role)) {
            return;
        }

        // 末影珍珠：用于让回溯者拥有额外位移手段，价格集中在 RecallerShopRules 中方便后续调整。
        context.addEntry(new ShopEntry.Builder(
                RecallerShopRules.ENDER_PEARL_ENTRY_ID,
                Items.ENDER_PEARL.getDefaultStack(),
                RecallerShopRules.ENDER_PEARL_PRICE,
                ShopEntry.Type.TOOL
        ).actualStack(Items.ENDER_PEARL.getDefaultStack()).build());

        // 紫颂果：低价、不限量的风险位移补给，同样不设置库存上限。
        context.addEntry(new ShopEntry.Builder(
                RecallerShopRules.CHORUS_FRUIT_ENTRY_ID,
                Items.CHORUS_FRUIT.getDefaultStack(),
                RecallerShopRules.CHORUS_FRUIT_PRICE,
                ShopEntry.Type.TOOL
        ).actualStack(Items.CHORUS_FRUIT.getDefaultStack()).build());
    }
}
