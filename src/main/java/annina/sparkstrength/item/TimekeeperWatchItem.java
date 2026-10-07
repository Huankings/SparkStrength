package annina.sparkstrength.item;

import annina.sparkstrength.SparkStrengthItems;
import annina.sparkstrength.role.timekeeper.TimekeeperWatchMode;
import annina.sparkstrength.role.timekeeper.TimekeeperWatchService;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/**
 * SparkStrength 版濒毁怀表。
 * 这里只保留刷新物品冷却和刷新技能冷却，不包含自改版中的回溯、损坏、修复和升级机制。
 */
public final class TimekeeperWatchItem extends Item {
    public TimekeeperWatchItem(Settings settings) {
        super(settings);
    }

    public static TimekeeperWatchMode getMode(ItemStack stack) {
        return TimekeeperWatchMode.fromOrdinal(
                stack.getOrDefault(SparkStrengthItems.TIMEKEEPER_WATCH_MODE, 0)
        );
    }

    public static void setMode(ItemStack stack, TimekeeperWatchMode mode) {
        stack.set(SparkStrengthItems.TIMEKEEPER_WATCH_MODE, mode.ordinal());
    }

    @Override
    public Text getName(ItemStack stack) {
        return Text.translatable("item.sparkstrength.dying_watch");
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!world.isClient) {
            TimekeeperWatchService.tryUse(user, stack, getMode(stack));
        }
        return TypedActionResult.success(stack, world.isClient);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, java.util.List<Text> tooltip, net.minecraft.item.tooltip.TooltipType type) {
        tooltip.add(Text.translatable("item.sparkstrength.dying_watch.mode", getMode(stack).text())
                .styled(style -> style.withColor(0xADD8E6).withItalic(false)));
        tooltip.add(Text.translatable("item.sparkstrength.dying_watch.description")
                .styled(style -> style.withColor(0xADD8E6).withItalic(false)));
    }
}
