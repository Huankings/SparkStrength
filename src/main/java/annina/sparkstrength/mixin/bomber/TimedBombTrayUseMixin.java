package annina.sparkstrength.mixin.bomber;

import annina.sparkstrength.role.bomber.BomberTrapService;
import annina.sparkstrength.role.bomber.TimedBombTrapHolder;
import annina.sparkstrength.role.coroner.CoronerService;
import dev.doctor4t.wathe.block.FoodPlatterBlock;
import dev.doctor4t.wathe.block_entity.BeveragePlateBlockEntity;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.index.WatheDataComponentTypes;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.bomber.BomberPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 定时炸弹托盘增强。
 *
 * <p>这个 mixin 优先级高于 NoellesRoles/Strength 的服务员双拿取 mixin。
 * 如果托盘里有炸弹，就由这里统一完成拿取，避免服务员或验尸官伪装服务员提前拿走物品却漏触发炸弹。</p>
 */
@Mixin(value = FoodPlatterBlock.class, priority = 1300)
public abstract class TimedBombTrayUseMixin {
    @Inject(method = "onUse", at = @At("HEAD"), cancellable = true)
    private void sparkstrength$handleTimedBombTray(
            BlockState state,
            World world,
            BlockPos pos,
            PlayerEntity player,
            BlockHitResult hit,
            CallbackInfoReturnable<ActionResult> cir
    ) {
        if (world.isClient || !(world.getBlockEntity(pos) instanceof BeveragePlateBlockEntity plate)) {
            return;
        }

        TimedBombTrapHolder trap = (TimedBombTrapHolder) plate;
        ItemStack handStack = player.getStackInHand(Hand.MAIN_HAND);

        if (handStack.isOf(ModItems.TIMED_BOMB)) {
            this.sparkstrength$tryPlantTrayBomb(plate, trap, handStack, player, pos, cir);
            return;
        }

        if (!handStack.isEmpty() || !trap.sparkstrength$hasTimedBombTrap()) {
            return;
        }

        /*
         * 已经携带定时炸弹的人不会消耗这个陷阱。
         * 这里直接放行给 Wathe/服务员原逻辑，让他仍然可以正常拿取托盘物品。
         */
        if (BomberPlayerComponent.KEY.get(player).hasBomb()) {
            return;
        }

        List<ItemStack> platter = plate.getStoredItems();
        if (platter.isEmpty()) {
            return;
        }

        if (!this.sparkstrength$canTakeFromBombedTray(player, platter)) {
            cir.setReturnValue(ActionResult.PASS);
            return;
        }

        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }

        UUID bomberUuid = trap.sparkstrength$getTimedBombTrapOwner();
        if (!BomberTrapService.tryAttachTimedBomb(serverPlayer, bomberUuid)) {
            return;
        }

        ItemStack takenStack = platter.get(world.getRandom().nextInt(platter.size())).copy();
        takenStack.setCount(1);
        takenStack.set(DataComponentTypes.MAX_STACK_SIZE, 1);

        String poisoner = plate.getPoisoner();
        GameRecordManager.recordPlatterTake(serverPlayer, Registries.ITEM.getId(takenStack.getItem()), pos, poisoner);
        BomberTrapService.recordTrapTrigger(serverPlayer, BomberTrapService.TIMED_BOMB_TRAY_TRIGGERED, bomberUuid);

        /*
         * 陷阱只有在成功把炸弹挂到有效目标身上后才清除。
         * 这对应“目标已有炸弹时不消耗、不清除”的规则。
         */
        trap.sparkstrength$setTimedBombTrapOwner(null);

        if (poisoner != null) {
            takenStack.set(WatheDataComponentTypes.POISONER, poisoner);
            plate.setPoisoner(null);
        }

        player.playSoundToPlayer(SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.BLOCKS, 1.0F, 1.0F);
        player.setStackInHand(Hand.MAIN_HAND, takenStack);
        cir.setReturnValue(ActionResult.PASS);
    }

    @Unique
    private void sparkstrength$tryPlantTrayBomb(
            BeveragePlateBlockEntity plate,
            TimedBombTrapHolder trap,
            ItemStack handStack,
            PlayerEntity player,
            BlockPos pos,
            CallbackInfoReturnable<ActionResult> cir
    ) {
        /*
         * 手持定时炸弹右键托盘时始终由这里接管，避免创造模式或异常路径
         * 把“定时炸弹物品本身”当成普通食物塞进托盘列表。
         */
        if (!(player instanceof ServerPlayerEntity serverPlayer)
                || !BomberTrapService.canPlantTimedBomb(player)
                || plate.getStoredItems().isEmpty()
                || plate.getPoisoner() != null
                || trap.sparkstrength$hasTimedBombTrap()) {
            cir.setReturnValue(ActionResult.PASS);
            return;
        }

        trap.sparkstrength$setTimedBombTrapOwner(player.getUuid());
        handStack.decrementUnlessCreative(1, player);
        player.playSoundToPlayer(SoundEvents.BLOCK_BREWING_STAND_BREW, SoundCategory.BLOCKS, 0.5F, 1.0F);
        BomberTrapService.recordTrapPlacement(serverPlayer, BomberTrapService.TIMED_BOMB_TRAY_EMBEDDED, pos);
        cir.setReturnValue(ActionResult.SUCCESS);
    }

    @Unique
    private boolean sparkstrength$canTakeFromBombedTray(PlayerEntity player, List<ItemStack> platter) {
        int matchingStacks = this.sparkstrength$countMatchingInventoryStacks(player, platter);
        if (matchingStacks == 0) {
            return true;
        }
        return this.sparkstrength$isWaiterLike(player) && matchingStacks < 2;
    }

    @Unique
    private boolean sparkstrength$isWaiterLike(PlayerEntity player) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        return gameWorld.isRole(player, Noellesroles.WAITER) || CoronerService.hasWaiterDisguise(player);
    }

    @Unique
    private int sparkstrength$countMatchingInventoryStacks(PlayerEntity player, List<ItemStack> platter) {
        Set<Item> platterItemTypes = new HashSet<>();
        for (ItemStack platterItem : platter) {
            platterItemTypes.add(platterItem.getItem());
        }

        int count = 0;
        for (int slot = 0; slot < player.getInventory().size(); slot++) {
            ItemStack inventoryStack = player.getInventory().getStack(slot);
            if (!inventoryStack.isEmpty() && platterItemTypes.contains(inventoryStack.getItem())) {
                count++;
            }
        }
        return count;
    }
}
