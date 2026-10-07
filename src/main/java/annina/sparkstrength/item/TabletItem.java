package annina.sparkstrength.item;

import annina.sparkstrength.tablet.TabletStateService;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/**
 * Opens the tablet network UI; all privileged actions are validated server-side.
 * 打开平板网络界面；所有关键操作都由服务端重新校验。
 */
public final class TabletItem extends Item {
    public TabletItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world.isClient()) {
            return TypedActionResult.success(stack, true);
        }
        if (user instanceof ServerPlayerEntity serverPlayer) {
            TabletStateService.openTablet(serverPlayer);
        }
        return TypedActionResult.success(stack);
    }
}
