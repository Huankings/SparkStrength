package annina.sparkstrength.item;

import annina.sparkstrength.role.morphling.MorphlingService;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

/**
 * 变形遥控。
 *
 * <p>遥控器不会自己保存状态，它只扫描当前 Morphling 名下仍处于“已标记未触发”的玩家并统一启动变形。</p>
 */
public final class MorphDeviceItem extends Item {
    public MorphDeviceItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world.isClient()) {
            return TypedActionResult.success(stack, true);
        }
        if (user instanceof ServerPlayerEntity morphling) {
            return MorphlingService.useDevice(morphling, stack);
        }
        return TypedActionResult.fail(stack);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("item.sparkstrength.morph_device.tooltip")
                .styled(style -> style.withColor(0x808080).withItalic(false)));
    }
}
