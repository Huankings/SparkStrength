package annina.sparkstrength.role.coroner;

import annina.sparkstrength.SparkStrengthItems;
import annina.sparkstrength.compat.SparkWitchCompat;
import annina.sparkstrength.role.engineer.EngineerPowerRestorationService;
import annina.sparkstrength.role.engineer.EngineerRules;
import annina.sparkstrength.role.professor.ProfessorSerumRules;
import annina.sparkstrength.role.professor.ProfessorSerumShopService;
import annina.sparkstrength.role.professor.ProfessorSerumType;
import annina.sparkstrength.role.recaller.RecallerShopRules;
import annina.sparkstrength.role.toxicologist.ToxicologistAntidoteService;
import annina.sparkstrength.role.toxicologist.ToxicologistCapsuleRules;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.event.BuildShopEntries;
import dev.doctor4t.wathe.cca.GameTimeComponent;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.item.IngredientItem;

import java.util.List;

/**
 * 验尸官专属商店条目。
 *
 * <p>这里只追加采尸袋，不清空原有条目；如果之后 NoellesRoles 或其它增强继续给验尸官加商品，
 * 不会被这里覆盖。</p>
 */
public final class CoronerShopService {
    private static boolean registered;

    private CoronerShopService() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        BuildShopEntries.EVENT.register(CoronerShopService::buildShopEntries);
    }

    private static void buildShopEntries(PlayerEntity player, BuildShopEntries.ShopContext context) {
        Role role = GameWorldComponent.KEY.get(player.getWorld()).getRole(player);
        if (!CoronerRules.canUseCoronerShop(role)) {
            return;
        }

        context.addEntry(0, new ShopEntry.Builder(
                CoronerRules.BODY_BAG_ENTRY_ID,
                bodyBagDisplayStack(),
                CoronerRules.BODY_BAG_PRICE,
                ShopEntry.Type.TOOL
        ).actualStack(SparkStrengthItems.coronerBodyBag().getDefaultStack()).build());

        Role disguiseRole = CoronerService.activeDisguiseRoleForRules(player);
        if (disguiseRole == null) {
            return;
        }
        /*
         * 验尸官伪装商店统一在采尸袋之后追加。
         * 这里不调用 NoellesRoles 原商店 handler，避免酒保/工程师这类 handler 的 clearEntries()
         * 把验尸官自己的采尸袋清掉；也避免真实职业判断导致验尸官打不开对应商店。
         */
        if (CoronerRules.isToxicologist(disguiseRole)) {
            addToxicologistShop(context);
        } else if (CoronerRules.isProfessor(disguiseRole)) {
            addProfessorShop(context);
        } else if (CoronerRules.isReporter(disguiseRole)) {
            addReporterShop(context);
        } else if (CoronerRules.isTimekeeper(disguiseRole)) {
            addTimekeeperShop(context);
        } else if (CoronerRules.isRecaller(disguiseRole)) {
            addRecallerShop(context);
        } else if (CoronerRules.isBartender(disguiseRole)) {
            addBartenderShop(context);
        } else if (CoronerRules.isEngineer(disguiseRole)) {
            rebuildEngineerShop(context);
        } else if (CoronerRules.isBomber(disguiseRole)) {
            addBomberShop(context);
        } else if (CoronerRules.isSparkWitchKidnapper(disguiseRole)) {
            addSparkWitchKidnapperShop(player, context);
        }
    }

    private static ItemStack bodyBagDisplayStack() {
        ItemStack stack = SparkStrengthItems.coronerBodyBag().getDefaultStack();
        stack.set(DataComponentTypes.ITEM_NAME, Text.translatable("shop.sparkstrength.coroner_body_bag"));
        stack.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                Text.translatable("shop.sparkstrength.coroner_body_bag.description")
                        .styled(style -> style.withColor(0x808080).withItalic(false))
        )));
        return stack;
    }

    private static void addToxicologistShop(BuildShopEntries.ShopContext context) {
        int offset = 0;
        offset = insertAfterBodyBag(context, offset, new ShopEntry.Builder(
                ToxicologistCapsuleRules.CAPSULE_ENTRY_ID,
                capsuleDisplayStack(),
                ToxicologistCapsuleRules.CAPSULE_PRICE,
                ShopEntry.Type.TOOL
        ).actualStack(SparkStrengthItems.capsule().getDefaultStack()).build());
        insertAfterBodyBag(context, offset, new ShopEntry.Builder(
                ToxicologistCapsuleRules.REFRESH_ANTIDOTE_COOLDOWN_ENTRY_ID,
                refreshAntidoteCooldownDisplayStack(),
                ToxicologistCapsuleRules.REFRESH_ANTIDOTE_COOLDOWN_PRICE,
                ShopEntry.Type.TOOL
        ).onBuy(ToxicologistAntidoteService::refreshAntidoteCooldown).build());
    }

    private static void addProfessorShop(BuildShopEntries.ShopContext context) {
        int offset = 0;
        for (ProfessorSerumType type : ProfessorSerumType.BACKPACK_ORDER) {
            offset = insertAfterBodyBag(context, offset, new ShopEntry.Builder(
                    ProfessorSerumRules.shopEntryId(type),
                    professorSerumDisplayStack(type),
                    ProfessorSerumRules.price(type),
                    ShopEntry.Type.TOOL
            ).actualStack(type.defaultStack()).build());
        }
        insertAfterBodyBag(context, offset, new ShopEntry.Builder(
                ProfessorSerumRules.REFRESH_COOLDOWN_ENTRY_ID,
                professorRefreshDisplayStack(),
                ProfessorSerumRules.REFRESH_COOLDOWN_PRICE,
                ShopEntry.Type.TOOL
        ).onBuy(ProfessorSerumShopService::refreshCooldown).build());
    }

    private static void addReporterShop(BuildShopEntries.ShopContext context) {
        insertAfterBodyBag(context, 0, new ShopEntry.Builder("note", new ItemStack(WatheItems.NOTE, 4), 50,
                ShopEntry.Type.TOOL).build());
    }

    private static void addTimekeeperShop(BuildShopEntries.ShopContext context) {
        ItemStack reduceTimeItem = ModItems.TIMEKEEPER_REDUCE_TIME.getDefaultStack();
        reduceTimeItem.set(DataComponentTypes.CUSTOM_NAME, Text.translatable("item.noellesroles.timekeeper_reduce_time"));
        insertAfterBodyBag(context, 0, new ShopEntry.Builder("timekeeper_reduce_time", reduceTimeItem, 100, ShopEntry.Type.TOOL)
                .onBuy(buyer -> {
                    GameTimeComponent.KEY.get(buyer.getWorld()).addTime(-900);
                    return true;
                }).build());
    }

    private static void addRecallerShop(BuildShopEntries.ShopContext context) {
        int offset = 0;
        offset = insertAfterBodyBag(context, offset, new ShopEntry.Builder(
                RecallerShopRules.ENDER_PEARL_ENTRY_ID,
                Items.ENDER_PEARL.getDefaultStack(),
                RecallerShopRules.ENDER_PEARL_PRICE,
                ShopEntry.Type.TOOL
        ).actualStack(Items.ENDER_PEARL.getDefaultStack()).build());
        insertAfterBodyBag(context, offset, new ShopEntry.Builder(
                RecallerShopRules.CHORUS_FRUIT_ENTRY_ID,
                Items.CHORUS_FRUIT.getDefaultStack(),
                RecallerShopRules.CHORUS_FRUIT_PRICE,
                ShopEntry.Type.TOOL
        ).actualStack(Items.CHORUS_FRUIT.getDefaultStack()).build());
    }

    private static void addBartenderShop(BuildShopEntries.ShopContext context) {
        context.clearEntries();
        context.addEntry(new ShopEntry.Builder(
                CoronerRules.BODY_BAG_ENTRY_ID,
                bodyBagDisplayStack(),
                CoronerRules.BODY_BAG_PRICE,
                ShopEntry.Type.TOOL
        ).actualStack(SparkStrengthItems.coronerBodyBag().getDefaultStack()).build());
        context.addEntry(new ShopEntry(ModItems.BASE_SPIRIT.getDefaultStack(), 50, ShopEntry.Type.POISON));
        addIngredientEntry(context, ModItems.RUM);
        addIngredientEntry(context, ModItems.GIN);
        addIngredientEntry(context, ModItems.VODKA);
        addIngredientEntry(context, ModItems.TEQUILA);
        addIngredientEntry(context, ModItems.WHISKEY);
        addIngredientEntry(context, ModItems.ICE_CUBE);
        addIngredientEntry(context, ModItems.SPECIAL_LIQUEUR);
        addIngredientEntry(context, ModItems.SPECIAL_SPICE);
    }

    private static void rebuildEngineerShop(BuildShopEntries.ShopContext context) {
        context.clearEntries();
        context.addEntry(new ShopEntry.Builder(
                CoronerRules.BODY_BAG_ENTRY_ID,
                bodyBagDisplayStack(),
                CoronerRules.BODY_BAG_PRICE,
                ShopEntry.Type.TOOL
        ).actualStack(SparkStrengthItems.coronerBodyBag().getDefaultStack()).build());
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

    private static void addBomberShop(BuildShopEntries.ShopContext context) {
        context.getEntries().removeIf(entry ->
                entry.stack().isOf(WatheItems.KNIFE)
                        || entry.stack().isOf(WatheItems.GRENADE));
        insertAfterBodyBag(context, 0, new ShopEntry.Builder(
                "coroner_bomber_timed_bomb",
                ModItems.TIMED_BOMB.getDefaultStack(),
                100,
                ShopEntry.Type.WEAPON
        ).actualStack(ModItems.TIMED_BOMB.getDefaultStack()).build());
        /*
         * 验尸官伪装为炸弹客时，商店还要继承炸弹客原本的手雷条目。
         * 这里按 NoellesRoles 炸弹客商店的平衡值重新放回 300 金币，
         * 避免基础商店里若已存在同名手雷时出现重复或价格不一致。
         */
        insertAfterBodyBag(context, 1, new ShopEntry.Builder(
                "grenade",
                WatheItems.GRENADE.getDefaultStack(),
                300,
                ShopEntry.Type.WEAPON
        ).actualStack(WatheItems.GRENADE.getDefaultStack()).build());
    }

    private static void addSparkWitchKidnapperShop(PlayerEntity player, BuildShopEntries.ShopContext context) {
        ShopEntry entry = SparkWitchCompat.createKidnapperDrugShopEntry(player);
        if (entry == null) {
            return;
        }
        /*
         * SparkWitch 自己拥有迷药的物品、贴图、价格与描述。
         * 这里仅在验尸官采尸袋之后追加该条目，不直接引用 SparkWitch 类，保持软兼容。
         */
        insertAfterBodyBag(context, 0, entry);
    }

    private static int insertAfterBodyBag(BuildShopEntries.ShopContext context, int offset, ShopEntry entry) {
        context.addEntry(Math.min(1 + offset, context.size()), entry);
        return offset + 1;
    }

    private static void addIngredientEntry(BuildShopEntries.ShopContext context, Item item) {
        if (item instanceof IngredientItem ingredientItem) {
            context.addEntry(new ShopEntry(item.getDefaultStack(), ingredientItem.getShopPrice(), ShopEntry.Type.POISON));
        }
    }

    private static ItemStack capsuleDisplayStack() {
        ItemStack stack = SparkStrengthItems.capsule().getDefaultStack();
        stack.set(DataComponentTypes.ITEM_NAME, Text.translatable("shop.sparkstrength.capsule"));
        stack.set(DataComponentTypes.LORE, grayLore("shop.sparkstrength.capsule.description"));
        return stack;
    }

    private static ItemStack refreshAntidoteCooldownDisplayStack() {
        ItemStack stack = Items.CLOCK.getDefaultStack();
        stack.set(DataComponentTypes.ITEM_NAME,
                Text.translatable("shop.sparkstrength.toxicologist.refresh_antidote_cooldown"));
        stack.set(DataComponentTypes.LORE,
                grayLore("shop.sparkstrength.toxicologist.refresh_antidote_cooldown.description"));
        return stack;
    }

    private static ItemStack professorSerumDisplayStack(ProfessorSerumType type) {
        ItemStack stack = type.defaultStack();
        stack.set(DataComponentTypes.ITEM_NAME, Text.translatable("shop.sparkstrength.professor." + type.id()));
        stack.set(DataComponentTypes.LORE, grayLore("shop.sparkstrength.professor." + type.id() + ".description"));
        return stack;
    }

    private static ItemStack professorRefreshDisplayStack() {
        ItemStack stack = Items.CLOCK.getDefaultStack();
        stack.set(DataComponentTypes.ITEM_NAME, Text.translatable("shop.sparkstrength.professor.refresh_cooldown"));
        stack.set(DataComponentTypes.LORE, grayLore("shop.sparkstrength.professor.refresh_cooldown.description"));
        return stack;
    }

    private static ItemStack captureDeviceDisplayStack() {
        ItemStack stack = SparkStrengthItems.captureDevice().getDefaultStack();
        stack.set(DataComponentTypes.ITEM_NAME, Text.translatable("shop.sparkstrength.engineer.capture_device"));
        stack.set(DataComponentTypes.LORE, grayLore("shop.sparkstrength.engineer.capture_device.description"));
        return stack;
    }

    private static ItemStack powerRestorationDisplayStack() {
        ItemStack stack = SparkStrengthItems.powerRestoration().getDefaultStack();
        stack.set(DataComponentTypes.ITEM_NAME, Text.translatable("shop.sparkstrength.engineer.power_restoration"));
        stack.set(DataComponentTypes.LORE, grayLore("shop.sparkstrength.engineer.power_restoration.description"));
        return stack;
    }

    private static LoreComponent grayLore(String translationKey) {
        return new LoreComponent(List.of(Text.translatable(translationKey)
                .styled(style -> style.withColor(0x808080).withItalic(false))));
    }
}
