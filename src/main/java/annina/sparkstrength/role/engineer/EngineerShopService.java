package annina.sparkstrength.role.engineer;

import annina.sparkstrength.SparkStrengthItems;
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
 * 工程师专属商店。
 *
 * <p>工程师原本的维修工具由 NoellesRoles 职业逻辑发放，不从这里移除。
 * 这里仅重建商店条目，保证工程师商店只出售本增强要求的捕捉装置和电力恢复系统。</p>
 */
public final class EngineerShopService {
    private static boolean registered;

    private EngineerShopService() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        BuildShopEntries.EVENT.register(EngineerShopService::buildShopEntries);
    }

    private static void buildShopEntries(PlayerEntity player, BuildShopEntries.ShopContext context) {
        Role role = GameWorldComponent.KEY.get(player.getWorld()).getRole(player);
        if (!EngineerRules.isEngineer(role)) {
            return;
        }

        context.clearEntries();
        context.addEntry(new ShopEntry.Builder(
                EngineerRules.CAPTURE_DEVICE_ENTRY_ID,
                captureDeviceDisplayStack(),
                EngineerRules.CAPTURE_DEVICE_PRICE,
                ShopEntry.Type.TOOL
        ).actualStack(SparkStrengthItems.captureDevice().getDefaultStack()).build());
        context.addEntry(new ShopEntry.Builder(
                EngineerRules.POWER_RESTORATION_ENTRY_ID,
                powerRestorationDisplayStack(),
                EngineerRules.POWER_RESTORATION_PRICE,
                ShopEntry.Type.TOOL
        ).onBuy(EngineerPowerRestorationService::tryRestorePower).build());
    }

    private static ItemStack captureDeviceDisplayStack() {
        ItemStack stack = SparkStrengthItems.captureDevice().getDefaultStack();
        stack.set(DataComponentTypes.ITEM_NAME, Text.translatable("shop.sparkstrength.engineer.capture_device"));
        stack.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                Text.translatable("shop.sparkstrength.engineer.capture_device.description")
                        .styled(style -> style.withColor(0x808080).withItalic(false))
        )));
        return stack;
    }

    private static ItemStack powerRestorationDisplayStack() {
        ItemStack stack = SparkStrengthItems.powerRestoration().getDefaultStack();
        stack.set(DataComponentTypes.ITEM_NAME, Text.translatable("shop.sparkstrength.engineer.power_restoration"));
        stack.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                Text.translatable("shop.sparkstrength.engineer.power_restoration.description")
                        .styled(style -> style.withColor(0x808080).withItalic(false))
        )));
        return stack;
    }
}
