package annina.sparkstrength.item;

import annina.sparkstrength.role.coroner.CoronerService;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

import java.util.List;

/**
 * 验尸官采尸袋。
 *
 * <p>交互行为与 Wathe 裹尸袋保持一致：对准玩家尸体右键即可收走尸体。
 * 区别是这里没有任何使用冷却，并且服务端会给验尸官解锁该尸体原主的变形能力。</p>
 */
public final class CoronerBodyBagItem extends Item {
    public CoronerBodyBagItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        if (!(entity instanceof PlayerBodyEntity body)) {
            return ActionResult.PASS;
        }

        if (user.getWorld().isClient()) {
            return ActionResult.SUCCESS;
        }
        if (!(user instanceof ServerPlayerEntity serverPlayer)
                || !(serverPlayer.getWorld() instanceof ServerWorld serverWorld)
                || !CoronerService.collectBodyWithBag(serverPlayer, body)) {
            return ActionResult.PASS;
        }

        CoronerService.playBodyBagSound(serverWorld, body);
        body.discard();
        if (!serverPlayer.isCreative()) {
            stack.decrement(1);
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public void appendTooltip(
            ItemStack stack,
            TooltipContext context,
            List<Text> tooltip,
            TooltipType type
    ) {
        tooltip.add(Text.translatable("item.sparkstrength.coroner_body_bag.tooltip")
                .styled(style -> style.withColor(0x808080).withItalic(false)));
    }
}
