package annina.sparkstrength.role.veteran;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.event.BuildShopEntries;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.List;

/**
 * 老兵专属商店。
 *
 * <p>Wathe 的商店权限来自“构建出的条目是否为空”，所以老兵要能打开商店，
 * 只需要在 {@link BuildShopEntries} 中给他构建条目。这里清空默认条目，
 * 确保老兵商店只卖加强方案指定的匕首。</p>
 */
public final class VeteranShopService {
    private static boolean registered;

    private VeteranShopService() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        BuildShopEntries.EVENT.register(VeteranShopService::buildShopEntries);
    }

    private static void buildShopEntries(PlayerEntity player, BuildShopEntries.ShopContext context) {
        Role role = GameWorldComponent.KEY.get(player.getWorld()).getRole(player);
        if (!VeteranRules.isVeteran(role)) {
            return;
        }

        context.clearEntries();
        context.addEntry(new ShopEntry.Builder(
                VeteranRules.KNIFE_ENTRY_ID,
                knifeDisplayStack(),
                VeteranRules.KNIFE_PRICE,
                ShopEntry.Type.WEAPON
        ).onBuy(VeteranShopService::buyKnife).build());
    }

    private static boolean buyKnife(PlayerEntity buyer) {
        ItemStack stack = WatheItems.KNIFE.getDefaultStack();
        boolean inserted = ShopEntry.insertStackInFreeSlot(buyer, stack);
        if (!inserted) {
            return false;
        }

        if (buyer instanceof ServerPlayerEntity serverPlayer) {
            // 只有真正把匕首放进背包后才加次数，避免快捷栏满时金币扣了但刀次数凭空增加。
            VeteranKnifeService.addPurchasedKnife(serverPlayer);
        }
        return true;
    }

    private static ItemStack knifeDisplayStack() {
        ItemStack stack = WatheItems.KNIFE.getDefaultStack();
        stack.set(DataComponentTypes.ITEM_NAME, Text.translatable("shop.sparkstrength.veteran_knife"));
        stack.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                Text.translatable("shop.sparkstrength.veteran_knife.description")
                        .styled(style -> style.withColor(0x808080).withItalic(false))
        )));
        return stack;
    }
}
