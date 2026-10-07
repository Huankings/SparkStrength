package annina.sparkstrength.item;

import annina.sparkstrength.entity.CapsuleEntity;
import annina.sparkstrength.role.coroner.CoronerService;
import annina.sparkstrength.role.toxicologist.ToxicologistCapsuleRules;
import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import dev.doctor4t.wathe.index.WatheDataComponentTypes;
import dev.doctor4t.wathe.util.PoisonUtils;
import net.minecraft.component.ComponentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.ClickType;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;

import java.util.List;
import java.util.Optional;

/**
 * Throwable capsule that stores one food or drink stack and forces the hit player to consume it.
 * 可投掷胶囊，内部保存一个食物或饮品并在命中玩家时强制食用。
 */
public final class CapsuleItem extends Item {
    private static final String ROOT_KEY = "SparkStrengthCapsule";
    private static final String CONTAINED_ITEM_KEY = "ContainedItem";
    private static final String CONTAINED_NAME_KEY = "ContainedName";
    private static final String NORMAL_POISON_KEY = "NormalPoison";
    private static final String BLUE_POISON_KEY = "BluePoison";

    public CapsuleItem(Settings settings) {
        super(settings);
    }

    @Override
    public boolean onClicked(
            ItemStack stack,
            ItemStack otherStack,
            Slot slot,
            ClickType clickType,
            PlayerEntity player,
            StackReference cursorStackReference
    ) {
        if (clickType != ClickType.RIGHT || hasContents(stack) || !isFoodOrDrink(otherStack)) {
            return false;
        }
        if (!CoronerService.canUseToxicologistDisguiseItem(player)) {
            /*
             * 验尸官从毒理学家伪装商店买到的胶囊会保留在背包里，
             * 但只有当前仍处于毒理学家尸体伪装时才允许继续填装/使用。
             */
            return false;
        }

        ItemStack contents = otherStack.copyWithCount(1);
        setContents(stack, contents, player.getRegistryManager());
        if (!player.isCreative()) {
            otherStack.decrement(1);
            cursorStackReference.set(otherStack);
        }
        return true;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!CoronerService.canUseToxicologistDisguiseItem(user)) {
            return TypedActionResult.pass(stack);
        }
        if (!hasContents(stack)) {
            if (!world.isClient()) {
                user.sendMessage(Text.translatable("message.sparkstrength.capsule.empty"), true);
            }
            return TypedActionResult.fail(stack);
        }

        world.playSound(
                null,
                user.getX(),
                user.getY(),
                user.getZ(),
                SoundEvents.ENTITY_SNOWBALL_THROW,
                SoundCategory.NEUTRAL,
                0.5F,
                0.4F / (world.getRandom().nextFloat() * 0.4F + 0.8F)
        );

        if (!world.isClient()) {
            CapsuleEntity entity = new CapsuleEntity(world, user);
            ItemStack thrownStack = stack.copyWithCount(1);
            entity.setItem(thrownStack);
            entity.setVelocity(user, user.getPitch(), user.getYaw(), 0.0F, 1.5F, 1.0F);
            world.spawnEntity(entity);
        }

        if (!user.isCreative()) {
            stack.decrement(1);
        }
        return TypedActionResult.success(stack, world.isClient());
    }

    @Override
    public Text getName(ItemStack stack) {
        Optional<CapsuleContents> contents = getContentsSummary(stack);
        if (contents.isEmpty()) {
            return super.getName(stack);
        }

        return filledNameText(contents.get());
    }

    @Override
    public void appendTooltip(
            ItemStack stack,
            TooltipContext context,
            List<Text> tooltip,
            TooltipType type
    ) {
        Optional<CapsuleContents> contents = getContentsSummary(stack);
        contents.ifPresent(value -> tooltip.add(
                tooltipText(value)
                        .styled(style -> style.withColor(0x808080).withItalic(false))
        ));
    }

    public static boolean hasContents(ItemStack capsuleStack) {
        return root(capsuleStack).contains(CONTAINED_ITEM_KEY, NbtElement.COMPOUND_TYPE);
    }

    public static ItemStack getContents(ItemStack capsuleStack, RegistryWrapper.WrapperLookup registryLookup) {
        NbtCompound root = root(capsuleStack);
        if (!root.contains(CONTAINED_ITEM_KEY, NbtElement.COMPOUND_TYPE)) {
            return ItemStack.EMPTY;
        }
        return ItemStack.fromNbtOrEmpty(registryLookup, root.getCompound(CONTAINED_ITEM_KEY));
    }

    public static void forceConsume(ServerPlayerEntity target, ItemStack contents) {
        if (contents.isEmpty() || !isFoodOrDrink(contents)) {
            return;
        }

        boolean drink = isDrink(contents);
        PlayerMoodComponent mood = PlayerMoodComponent.KEY.get(target);
        if (drink) {
            mood.drinkCocktail();
            target.getWorld().playSound(
                    null,
                    target.getBlockPos(),
                    SoundEvents.ENTITY_GENERIC_DRINK,
                    SoundCategory.PLAYERS,
                    1.0F,
                    1.0F
            );
        } else {
            mood.eatFood();
            target.getWorld().playSound(
                    null,
                    target.getBlockPos(),
                    SoundEvents.ENTITY_GENERIC_EAT,
                    SoundCategory.PLAYERS,
                    1.0F,
                    1.0F
            );
        }

        PoisonUtils.applyFoodPoison(target, contents);
    }

    public static boolean isFoodOrDrink(ItemStack stack) {
        return !stack.isEmpty() && (isDrink(stack) || stack.get(DataComponentTypes.FOOD) != null);
    }

    public static boolean isDrink(ItemStack stack) {
        return !stack.isEmpty() && stack.getUseAction() == UseAction.DRINK;
    }

    private static void setContents(
            ItemStack capsuleStack,
            ItemStack contents,
            RegistryWrapper.WrapperLookup registryLookup
    ) {
        NbtCompound root = new NbtCompound();
        NbtElement encoded = contents.encode(registryLookup);
        if (encoded instanceof NbtCompound encodedCompound) {
            root.put(CONTAINED_ITEM_KEY, encodedCompound);
        }
        root.putString(CONTAINED_NAME_KEY, contents.getName().getString());
        root.putBoolean(NORMAL_POISON_KEY, hasNormalPoison(contents));
        root.putBoolean(BLUE_POISON_KEY, hasBluePoison(contents));

        NbtCompound customData = new NbtCompound();
        customData.put(ROOT_KEY, root);
        capsuleStack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(customData));
        clearDisplayOnlyComponents(capsuleStack);
    }

    private static Optional<CapsuleContents> getContentsSummary(ItemStack capsuleStack) {
        NbtCompound root = root(capsuleStack);
        if (!root.contains(CONTAINED_ITEM_KEY, NbtElement.COMPOUND_TYPE)) {
            return Optional.empty();
        }
        return Optional.of(new CapsuleContents(
                root.getString(CONTAINED_NAME_KEY),
                root.getBoolean(NORMAL_POISON_KEY),
                root.getBoolean(BLUE_POISON_KEY)
        ));
    }

    private static NbtCompound root(ItemStack capsuleStack) {
        NbtComponent component = capsuleStack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        NbtCompound data = component.copyNbt();
        return data.contains(ROOT_KEY, NbtElement.COMPOUND_TYPE) ? data.getCompound(ROOT_KEY) : new NbtCompound();
    }

    private static boolean hasNormalPoison(ItemStack stack) {
        return stack.contains(WatheDataComponentTypes.POISONER);
    }

    private static boolean hasBluePoison(ItemStack stack) {
        if (!Registries.DATA_COMPONENT_TYPE.containsId(ToxicologistCapsuleRules.BLUE_POISON_COMPONENT_ID)) {
            return false;
        }
        ComponentType<?> componentType = Registries.DATA_COMPONENT_TYPE.get(
                ToxicologistCapsuleRules.BLUE_POISON_COMPONENT_ID
        );
        return componentType != null && stack.contains(componentType);
    }

    static MutableText filledNameText(String name, boolean normalPoisoned, boolean bluePoisoned) {
        return Text.translatable(
                "item.sparkstrength.capsule.filled",
                contentDisplayText(name, normalPoisoned, bluePoisoned)
        );
    }

    private static MutableText filledNameText(CapsuleContents contents) {
        return filledNameText(contents.name(), contents.normalPoisoned(), contents.bluePoisoned());
    }

    static MutableText tooltipText(String name, boolean normalPoisoned, boolean bluePoisoned) {
        return Text.translatable(
                "item.sparkstrength.capsule.tooltip",
                contentDisplayText(name, normalPoisoned, bluePoisoned)
        );
    }

    private static MutableText tooltipText(CapsuleContents contents) {
        return tooltipText(contents.name(), contents.normalPoisoned(), contents.bluePoisoned());
    }

    static MutableText contentDisplayText(String name, boolean normalPoisoned, boolean bluePoisoned) {
        MutableText contentText = Text.literal(name);
        if (!normalPoisoned && !bluePoisoned) {
            return contentText;
        }

        int color = ToxicologistCapsuleRules.poisonNameColor(normalPoisoned, bluePoisoned);
        return Text.translatable("item.sparkstrength.capsule.poisoned_content", contentText)
                .styled(style -> style.withColor(color).withItalic(false));
    }

    static void clearDisplayOnlyComponents(ItemStack capsuleStack) {
        // Drop shop-only display data after filling so the dynamic capsule name can show poison state.
        // 装入内容后移除商店展示组件，让胶囊名称能动态显示有毒状态。
        capsuleStack.remove(DataComponentTypes.ITEM_NAME);
        capsuleStack.remove(DataComponentTypes.LORE);
    }

    private record CapsuleContents(String name, boolean normalPoisoned, boolean bluePoisoned) {
    }
}
