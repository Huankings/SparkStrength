package annina.sparkstrength.mixin.coroner;

import annina.sparkstrength.role.coroner.CoronerService;
import dev.doctor4t.wathe.block.FoodPlatterBlock;
import dev.doctor4t.wathe.block_entity.BeveragePlateBlockEntity;
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
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 验尸官伪装服务员时复用服务员“可以从餐盘/饮品托盘拿两份”的被动能力。
 */
@Mixin(FoodPlatterBlock.class)
public abstract class CoronerWaiterPlatterMixin {
    @Inject(method = "onUse", at = @At("HEAD"), cancellable = true)
    private void sparkstrength$coronerWaiterDoublePickup(
            BlockState state,
            World world,
            BlockPos pos,
            PlayerEntity player,
            BlockHitResult hit,
            CallbackInfoReturnable<ActionResult> cir
    ) {
        if (world.isClient
                || !CoronerService.hasWaiterDisguise(player)
                || !player.getStackInHand(Hand.MAIN_HAND).isEmpty()
                || !(world.getBlockEntity(pos) instanceof BeveragePlateBlockEntity plate)) {
            return;
        }

        List<ItemStack> platter = plate.getStoredItems();
        if (platter.isEmpty()) {
            return;
        }

        /*
         * Wathe 原版餐盘逻辑只允许玩家携带 1 份餐盘同类物品；
         * NoellesRoles 的服务员增强允许携带第 2 份。验尸官穿上服务员尸体身份时只补第 2 份，
         * 第 1 份仍交给 Wathe 原逻辑处理，避免普通拿取、下毒、创造模式摆盘等行为被这里接管。
         */
        Set<Item> platterItemTypes = new HashSet<>();
        for (ItemStack platterItem : platter) {
            platterItemTypes.add(platterItem.getItem());
        }

        int matchCount = 0;
        for (int slot = 0; slot < player.getInventory().size(); slot++) {
            ItemStack inventoryStack = player.getInventory().getStack(slot);
            if (!inventoryStack.isEmpty() && platterItemTypes.contains(inventoryStack.getItem())) {
                matchCount++;
            }
        }

        if (matchCount == 0) {
            return;
        }
        if (matchCount >= 2) {
            cir.setReturnValue(ActionResult.PASS);
            return;
        }

        ItemStack takenStack = platter.get(world.getRandom().nextInt(platter.size())).copy();
        takenStack.setCount(1);
        takenStack.set(DataComponentTypes.MAX_STACK_SIZE, 1);

        String poisoner = plate.getPoisoner();
        if (player instanceof ServerPlayerEntity serverPlayer) {
            GameRecordManager.recordPlatterTake(
                    serverPlayer,
                    Registries.ITEM.getId(takenStack.getItem()),
                    pos,
                    poisoner
            );
        }
        if (poisoner != null) {
            takenStack.set(WatheDataComponentTypes.POISONER, poisoner);
            plate.setPoisoner(null);
        }

        player.playSoundToPlayer(SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.BLOCKS, 1.0F, 1.0F);
        player.setStackInHand(Hand.MAIN_HAND, takenStack);
        cir.setReturnValue(ActionResult.PASS);
    }
}
