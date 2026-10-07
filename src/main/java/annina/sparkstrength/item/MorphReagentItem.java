package annina.sparkstrength.item;

import annina.sparkstrength.role.morphling.MorphlingService;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;

import java.util.List;

/**
 * 变形试剂。
 *
 * <p>试剂自身只负责保存“是否已经采样”的物品数据；所有职业校验、目标解析、标记消耗和回放
 * 都交给 {@link MorphlingService}，这样准星右键和直接右键实体不会出现两套不同规则。</p>
 */
public final class MorphReagentItem extends Item {
    public MorphReagentItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        if (user.getWorld().isClient()) {
            if (!MorphlingService.hasSample(stack)) {
                /*
                 * 客户端也进入“正在使用物品”状态，这样松开右键时会正常发出停止使用包。
                 * 服务端借这个释放信号清掉采样 gate，防止一次点按先采样又立刻标记。
                 */
                user.setCurrentHand(hand);
                return ActionResult.CONSUME;
            }
            return ActionResult.PASS;
        }
        if (!(user instanceof ServerPlayerEntity morphling)) {
            return ActionResult.PASS;
        }
        boolean hadSample = MorphlingService.hasSample(stack);
        ActionResult result = MorphlingService.useReagent(morphling, stack, entity).getResult();
        startWaitingForReleaseAfterSampling(stack, user, hand, hadSample);
        return result;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world.isClient()) {
            if (!MorphlingService.hasSample(stack)) {
                user.setCurrentHand(hand);
                return TypedActionResult.consume(stack);
            }
            return TypedActionResult.success(stack, true);
        }
        if (user instanceof ServerPlayerEntity morphling) {
            boolean hadSample = MorphlingService.hasSample(stack);
            TypedActionResult<ItemStack> result = MorphlingService.useReagent(morphling, stack, null);
            startWaitingForReleaseAfterSampling(stack, user, hand, hadSample);
            return result;
        }
        return TypedActionResult.fail(stack);
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if (!world.isClient() && user instanceof ServerPlayerEntity morphling) {
            MorphlingService.clearReagentReleaseGate(morphling);
        }
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.NONE;
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return 72000;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        String sampleName = MorphlingService.sampleNameForTooltip(stack);
        tooltip.add(Text.translatable(
                "item.sparkstrength.morph_reagent.tooltip.sample",
                sampleName.isBlank() ? Text.translatable("item.sparkstrength.morph_reagent.tooltip.none") : Text.literal(sampleName)
        ).styled(style -> style.withColor(0x808080).withItalic(false)));
        for (int i = 1; i <= 4; i++) {
            tooltip.add(Text.translatable("item.sparkstrength.morph_reagent.tooltip.line" + i)
                    .styled(style -> style.withColor(0x808080).withItalic(false)));
        }
    }

    private static void startWaitingForReleaseAfterSampling(
            ItemStack stack,
            PlayerEntity user,
            Hand hand,
            boolean hadSample
    ) {
        if (!hadSample && MorphlingService.hasSample(stack)) {
            user.setCurrentHand(hand);
        }
    }
}
