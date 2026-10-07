package annina.sparkstrength.mixin.bomber;

import annina.sparkstrength.role.bomber.BomberTrapService;
import annina.sparkstrength.role.bomber.TimedBombTrapHolder;
import dev.doctor4t.wathe.block.TrimmedBedBlock;
import dev.doctor4t.wathe.block_entity.TrimmedBedBlockEntity;
import dev.doctor4t.wathe.index.WatheItems;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.BedPart;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.bomber.BomberPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

/**
 * 定时炸弹床增强。
 *
 * <p>床的实际陷阱状态统一记在床头方块实体上，脚部被点击时也会先解析到床头。
 * 睡觉触发时会从当前床开始搜索水平相连的整片 Wathe 床，只要连通区域里存在炸弹床，
 * 睡在其中任意一张床上都会带上炸弹。</p>
 */
@Mixin(value = TrimmedBedBlock.class, priority = 1300)
public abstract class TimedBombBedUseMixin {
    @Inject(method = "onUse", at = @At("HEAD"), cancellable = true)
    private void sparkstrength$handleTimedBombBed(
            BlockState state,
            World world,
            BlockPos pos,
            PlayerEntity player,
            BlockHitResult hit,
            CallbackInfoReturnable<ActionResult> cir
    ) {
        if (world.isClient) {
            return;
        }

        ItemStack handStack = player.getStackInHand(Hand.MAIN_HAND);
        if (handStack.isOf(WatheItems.KNIFE)
                || handStack.isOf(WatheItems.REVOLVER)
                || handStack.isOf(WatheItems.DERRINGER)) {
            return;
        }

        BedTrapTarget target = this.sparkstrength$resolveHeadBed(world, pos, state);
        if (target == null) {
            return;
        }

        TimedBombTrapHolder targetTrap = (TimedBombTrapHolder) target.bed();

        if (handStack.isOf(ModItems.TIMED_BOMB)) {
            this.sparkstrength$tryPlantBedBomb(target, targetTrap, handStack, player, cir);
            return;
        }

        if (handStack.isOf(WatheItems.SCORPION) && targetTrap.sparkstrength$hasTimedBombTrap()) {
            cir.setReturnValue(ActionResult.PASS);
            return;
        }

        if (target.state().get(BedBlock.OCCUPIED)
                || !(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }

        BedTrapTarget bombedBed = this.sparkstrength$findConnectedTimedBombBed(world, target);
        if (bombedBed == null) {
            return;
        }

        /*
         * 已经携带定时炸弹的人睡上相连的陷阱床区域时，不触发、不清除该陷阱。
         * 直接交回 wathe 原逻辑继续睡觉，保持“陷阱等待下一个有效目标”的规则。
         */
        if (BomberPlayerComponent.KEY.get(player).hasBomb()) {
            return;
        }

        TimedBombTrapHolder bombedTrap = (TimedBombTrapHolder) bombedBed.bed();
        UUID bomberUuid = bombedTrap.sparkstrength$getTimedBombTrapOwner();
        if (!BomberTrapService.tryAttachTimedBomb(serverPlayer, bomberUuid)) {
            return;
        }

        BomberTrapService.recordTrapTrigger(serverPlayer, BomberTrapService.TIMED_BOMB_BED_TRIGGERED, bomberUuid);
        bombedTrap.sparkstrength$setTimedBombTrapOwner(null);

        player.trySleep(target.pos()).ifLeft(reason -> {
            if (reason.getMessage() != null) {
                player.sendMessage(reason.getMessage(), true);
            }
        });
        cir.setReturnValue(ActionResult.SUCCESS);
    }

    @Unique
    private void sparkstrength$tryPlantBedBomb(
            BedTrapTarget target,
            TimedBombTrapHolder trap,
            ItemStack handStack,
            PlayerEntity player,
            CallbackInfoReturnable<ActionResult> cir
    ) {
        /*
         * 手持定时炸弹点击床时始终由这里接管。
         * 若床上已有蝎子或已有炸弹，直接失败，防止定时炸弹和蝎子叠加。
         */
        if (!(player instanceof ServerPlayerEntity serverPlayer)
                || !BomberTrapService.canPlantTimedBomb(player)
                || target.bed().hasScorpion()
                || trap.sparkstrength$hasTimedBombTrap()) {
            cir.setReturnValue(ActionResult.PASS);
            return;
        }

        trap.sparkstrength$setTimedBombTrapOwner(player.getUuid());
        handStack.decrementUnlessCreative(1, player);
        player.playSoundToPlayer(SoundEvents.BLOCK_BREWING_STAND_BREW, SoundCategory.BLOCKS, 0.5F, 1.0F);
        BomberTrapService.recordTrapPlacement(serverPlayer, BomberTrapService.TIMED_BOMB_BED_EMBEDDED, target.pos());
        cir.setReturnValue(ActionResult.SUCCESS);
    }

    @Unique
    private BedTrapTarget sparkstrength$resolveHeadBed(World world, BlockPos pos, BlockState state) {
        BlockPos headPos = pos;
        BlockState headState = state;
        if (state.get(BedBlock.PART) != BedPart.HEAD) {
            headPos = pos.offset(state.get(BedBlock.FACING));
            headState = world.getBlockState(headPos);
            if (!(headState.getBlock() instanceof TrimmedBedBlock)) {
                return null;
            }
        }

        if (world.getBlockEntity(headPos) instanceof TrimmedBedBlockEntity bed) {
            return new BedTrapTarget(headPos, headState, bed);
        }
        return null;
    }

    @Unique
    private BedTrapTarget sparkstrength$findConnectedTimedBombBed(World world, BedTrapTarget start) {
        /*
         * 按床块的水平四邻接做连通搜索：床头、床脚和相邻床都会被纳入同一片床区；
         * 一旦中间出现非 TrimmedBedBlock，搜索就不会继续穿过去。
         */
        Queue<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visitedBlocks = new HashSet<>();
        Set<BlockPos> checkedHeads = new HashSet<>();

        queue.add(start.pos());
        while (!queue.isEmpty()) {
            BlockPos currentPos = queue.remove();
            if (!visitedBlocks.add(currentPos)) {
                continue;
            }

            BlockState currentState = world.getBlockState(currentPos);
            if (!(currentState.getBlock() instanceof TrimmedBedBlock)) {
                continue;
            }

            BedTrapTarget currentBed = this.sparkstrength$resolveHeadBed(world, currentPos, currentState);
            if (currentBed != null && checkedHeads.add(currentBed.pos())) {
                TimedBombTrapHolder trap = (TimedBombTrapHolder) currentBed.bed();
                if (trap.sparkstrength$hasTimedBombTrap()) {
                    return currentBed;
                }
            }

            queue.add(currentPos.north());
            queue.add(currentPos.south());
            queue.add(currentPos.west());
            queue.add(currentPos.east());
        }

        return null;
    }

    @Unique
    private record BedTrapTarget(BlockPos pos, BlockState state, TrimmedBedBlockEntity bed) {
    }
}
