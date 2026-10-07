package annina.sparkstrength.item;

import annina.sparkstrength.role.professor.ProfessorSerumService;
import annina.sparkstrength.role.professor.ProfessorSerumType;
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
import net.minecraft.world.World;

import java.util.List;

/**
 * 教授试剂物品。
 *
 * <p>物品本身只保存“这是哪一种试剂”，真正的职业、存活、目标和效果校验全部交给
 * {@link ProfessorSerumService}。这样右键投喂和背包远程投喂能共用同一套服务端逻辑。</p>
 */
public final class ProfessorSerumItem extends Item {
    private final ProfessorSerumType type;

    public ProfessorSerumItem(Settings settings, ProfessorSerumType type) {
        super(settings);
        this.type = type;
    }

    public ProfessorSerumType type() {
        return type;
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        if (user.getWorld().isClient()) {
            return ActionResult.PASS;
        }
        if (!(user instanceof ServerPlayerEntity professor)
                || !(entity instanceof ServerPlayerEntity target)) {
            return ActionResult.PASS;
        }

        return ProfessorSerumService.useHeldSerumOnTarget(professor, target, stack, type)
                ? ActionResult.CONSUME
                : ActionResult.FAIL;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world.isClient()) {
            return TypedActionResult.success(stack, true);
        }

        if (user instanceof ServerPlayerEntity serverPlayer
                && ProfessorSerumService.useHeldSerum(serverPlayer, stack, type)) {
            return TypedActionResult.success(stack);
        }
        return TypedActionResult.fail(stack);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable(getTranslationKey() + ".tooltip.line1")
                .styled(style -> style.withColor(0x808080).withItalic(false)));
        tooltip.add(Text.translatable(getTranslationKey() + ".tooltip.line2")
                .styled(style -> style.withColor(0x808080).withItalic(false)));
    }
}
