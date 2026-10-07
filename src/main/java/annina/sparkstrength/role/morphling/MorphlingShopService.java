package annina.sparkstrength.role.morphling;

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
 * 变形怪专属商店改写。
 *
 * <p>Wathe 的默认杀手商店会先构建好刀、枪、手雷、疯魔、毒药等条目；这里只在玩家确实是
 * Morphling 时移除毒药瓶和蝎子，并把变形试剂插在手雷与疯魔之间，避免影响其它杀手职业。</p>
 */
public final class MorphlingShopService {
    private static boolean registered;

    private MorphlingShopService() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        BuildShopEntries.EVENT.register(MorphlingShopService::buildShopEntries);
    }

    private static void buildShopEntries(PlayerEntity player, BuildShopEntries.ShopContext context) {
        Role role = GameWorldComponent.KEY.get(player.getWorld()).getRole(player);
        if (!MorphlingRules.isMorphling(role)) {
            return;
        }

        context.getEntries().removeIf(entry -> "poison_vial".equals(entry.id()) || "scorpion".equals(entry.id()));

        int insertIndex = findEntryIndex(context, "psycho_mode");
        if (insertIndex < 0) {
            int grenadeIndex = findEntryIndex(context, "grenade");
            insertIndex = grenadeIndex >= 0 ? grenadeIndex + 1 : context.size();
        }
        context.addEntry(insertIndex, new ShopEntry.Builder(
                MorphlingRules.MORPH_REAGENT_ENTRY_ID,
                reagentDisplayStack(),
                MorphlingRules.MORPH_REAGENT_PRICE,
                ShopEntry.Type.POISON
        ).actualStack(new ItemStack(SparkStrengthItems.morphReagent())).build());
    }

    private static int findEntryIndex(BuildShopEntries.ShopContext context, String id) {
        for (int i = 0; i < context.size(); i++) {
            if (id.equals(context.getEntry(i).id())) {
                return i;
            }
        }
        return -1;
    }

    private static ItemStack reagentDisplayStack() {
        ItemStack stack = new ItemStack(SparkStrengthItems.morphReagent());
        stack.set(DataComponentTypes.ITEM_NAME, Text.translatable("shop.sparkstrength.morphling.morph_reagent"));
        stack.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                Text.translatable("shop.sparkstrength.morphling.morph_reagent.description")
                        .styled(style -> style.withColor(0x808080).withItalic(false))
        )));
        return stack;
    }
}
