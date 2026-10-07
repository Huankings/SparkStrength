package annina.sparkstrength.role.coroner;

import dev.doctor4t.wathe.api.event.DoorInteraction;
import dev.doctor4t.wathe.api.event.DoorStateChanged;
import dev.doctor4t.wathe.block.SmallDoorBlock;
import dev.doctor4t.wathe.block_entity.DoorBlockEntity;
import dev.doctor4t.wathe.block_entity.SmallDoorBlockEntity;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheSounds;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.item.RepairToolItem;
import org.agmas.noellesroles.packet.EngineerDoorHighlightS2CPacket;

/**
 * 验尸官伪装工程师时借用 NoellesRoles 工程师维修工具能力。
 *
 * <p>NoellesRoles 原实现直接在事件里判断真实 ENGINEER，因此验尸官拿到维修工具后不会进入那段逻辑。
 * 这里只在“真实职业是验尸官且当前有效伪装是工程师”时接管，避免影响真实工程师。</p>
 */
public final class CoronerEngineerService {
    private static boolean registered;

    private CoronerEngineerService() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        DoorInteraction.EVENT.register(CoronerEngineerService::doorInteraction);
        DoorStateChanged.BLAST.register((world, pos, doorEntity) -> notifyCoronerEngineers(world, pos,
                "tip.engineer.door_blasted"));
        DoorStateChanged.JAM.register((world, pos, doorEntity) -> notifyCoronerEngineers(world, pos,
                "tip.engineer.door_jammed"));
    }

    private static DoorInteraction.DoorInteractionResult doorInteraction(DoorInteraction.DoorInteractionContext context) {
        if (!context.getHandItem().isOf(ModItems.REPAIR_TOOL) || !context.isServerSide()) {
            return DoorInteraction.DoorInteractionResult.PASS;
        }
        if (!(context.getPlayer() instanceof ServerPlayerEntity player) || !CoronerService.hasEngineerDisguise(player)) {
            return DoorInteraction.DoorInteractionResult.PASS;
        }
        if (player.getItemCooldownManager().isCoolingDown(ModItems.REPAIR_TOOL)) {
            return DoorInteraction.DoorInteractionResult.DENY;
        }

        DoorBlockEntity entity = context.getEntity();
        World world = context.getWorld();
        BlockPos lowerPos = context.getLowerPos();
        BlockState state = world.getBlockState(lowerPos);

        if (entity.isBlasted()) {
            entity.setBlasted(false);
            if (entity.isOpen()) {
                entity.toggle(false);
            }
            entity.sync();
            if (entity instanceof SmallDoorBlockEntity) {
                SmallDoorBlockEntity neighbor = SmallDoorBlock.getNeighborDoorEntity(state, world, lowerPos);
                if (neighbor != null && neighbor.isBlasted()) {
                    neighbor.setBlasted(false);
                    if (neighbor.isOpen()) {
                        neighbor.toggle(false);
                    }
                    neighbor.sync();
                }
            }
            finishRepairToolAction(player, world, lowerPos, "tip.engineer.repaired", "repair",
                    WatheSounds.BLOCK_DOOR_TOGGLE, 1.2F);
            return DoorInteraction.DoorInteractionResult.HANDLED;
        }

        if (entity.isJammed()) {
            entity.setJammed(0);
            entity.sync();
            if (entity instanceof SmallDoorBlockEntity) {
                SmallDoorBlockEntity neighbor = SmallDoorBlock.getNeighborDoorEntity(state, world, lowerPos);
                if (neighbor != null && neighbor.isJammed()) {
                    neighbor.setJammed(0);
                    neighbor.sync();
                }
            }
            finishRepairToolAction(player, world, lowerPos, "tip.engineer.unlocked", "unlock",
                    WatheSounds.ITEM_LOCKPICK_DOOR, 1.2F);
            return DoorInteraction.DoorInteractionResult.HANDLED;
        }

        entity.setJammed(GameConstants.JAMMED_DOOR_TIME);
        if (entity.isOpen()) {
            entity.toggle(false);
        }
        entity.sync();
        if (entity instanceof SmallDoorBlockEntity) {
            SmallDoorBlockEntity neighbor = SmallDoorBlock.getNeighborDoorEntity(state, world, lowerPos);
            if (neighbor != null) {
                neighbor.setJammed(GameConstants.JAMMED_DOOR_TIME);
                if (neighbor.isOpen()) {
                    neighbor.toggle(false);
                }
                neighbor.sync();
            }
        }
        finishRepairToolAction(player, world, lowerPos, "tip.engineer.locked", "lock",
                WatheSounds.BLOCK_DOOR_LOCKED, 0.8F);
        return DoorInteraction.DoorInteractionResult.HANDLED;
    }

    private static void finishRepairToolAction(
            ServerPlayerEntity player,
            World world,
            BlockPos lowerPos,
            String messageKey,
            String action,
            net.minecraft.sound.SoundEvent sound,
            float pitch
    ) {
        player.getItemCooldownManager().set(ModItems.REPAIR_TOOL, RepairToolItem.COOLDOWN_TICKS);
        player.sendMessage(Text.translatable(messageKey), true);
        world.playSound(null, lowerPos.getX() + .5f, lowerPos.getY() + 1, lowerPos.getZ() + .5f,
                sound, net.minecraft.sound.SoundCategory.BLOCKS, 1f, pitch);

        NbtCompound extra = new NbtCompound();
        GameRecordManager.putBlockPos(extra, "pos", lowerPos);
        extra.putString("action", action);
        GameRecordManager.recordItemUse(player, net.minecraft.registry.Registries.ITEM.getId(ModItems.REPAIR_TOOL),
                null, extra);
    }

    private static void notifyCoronerEngineers(World world, BlockPos pos, String messageKey) {
        if (!(world instanceof ServerWorld serverWorld)) {
            return;
        }
        for (ServerPlayerEntity player : serverWorld.getPlayers()) {
            if (CoronerService.hasEngineerDisguise(player) && GameFunctions.isPlayerPlayingAndAlive(player)) {
                player.sendMessage(Text.translatable(messageKey), true);
                ServerPlayNetworking.send(player, new EngineerDoorHighlightS2CPacket(pos));
            }
        }
    }
}
