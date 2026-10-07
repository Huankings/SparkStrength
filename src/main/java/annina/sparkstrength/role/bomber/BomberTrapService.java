package annina.sparkstrength.role.bomber;

import annina.sparkstrength.SparkStrength;
import annina.sparkstrength.mixin.bomber.BomberPlayerComponentAccessor;
import annina.sparkstrength.role.coroner.CoronerService;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.bomber.BomberPlayerComponent;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 炸弹客定时炸弹陷阱的服务端通用逻辑。
 *
 * <p>自改版 NoellesRoles 依赖 wathe 的公共托盘/床效果接口；Spark 版 wathe 暂无该接口，
 * 所以 Strength 在这里集中实现“能不能安置、如何挂到玩家身上、如何记录回放、如何清局”。</p>
 */
public final class BomberTrapService {
    public static final String TRAP_OWNER_NBT_KEY = "SparkStrengthTimedBombOwner";

    public static final Identifier TIMED_BOMB_TRAY_EMBEDDED = SparkStrength.id("timed_bomb_tray_embedded");
    public static final Identifier TIMED_BOMB_BED_EMBEDDED = SparkStrength.id("timed_bomb_bed_embedded");
    public static final Identifier TIMED_BOMB_TRAY_TRIGGERED = SparkStrength.id("timed_bomb_tray_triggered");
    public static final Identifier TIMED_BOMB_BED_TRIGGERED = SparkStrength.id("timed_bomb_bed_triggered");

    private static final Map<net.minecraft.registry.RegistryKey<World>, Set<BlockPos>> TRACKED_TRAPS = new HashMap<>();

    private BomberTrapService() {
    }

    public static boolean canPlantTimedBomb(PlayerEntity player) {
        if (!GameFunctions.isPlayerAliveAndSurvival(player) || !GameFunctions.isPlayerPlayingAndAlive(player)) {
            return false;
        }
        if (BomberPlayerComponent.KEY.get(player).hasBomb()) {
            return false;
        }
        if (player.getItemCooldownManager().isCoolingDown(ModItems.TIMED_BOMB)) {
            return false;
        }

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        return gameWorld.isRole(player, Noellesroles.BOMBER) || CoronerService.hasBomberDisguise(player);
    }

    public static boolean tryAttachTimedBomb(ServerPlayerEntity target, @Nullable UUID bomberUuid) {
        if (!GameFunctions.isPlayerAliveAndSurvival(target) || !GameFunctions.isPlayerPlayingAndAlive(target)) {
            return false;
        }

        BomberPlayerComponent component = BomberPlayerComponent.KEY.get(target);
        if (component.hasBomb()) {
            return false;
        }

        /*
         * 这里故意不调用 component.placeBomb(...)：
         * placeBomb 会写入“直接安置炸弹”的 NoellesRoles 回放，而托盘/床触发需要显示 Strength 的专属文案。
         */
        BomberPlayerComponentAccessor accessor = (BomberPlayerComponentAccessor) component;
        accessor.sparkstrength$setHasBomb(true);
        accessor.sparkstrength$setBombTimer(BomberPlayerComponent.BOMB_DELAY_TICKS);
        accessor.sparkstrength$setBeepTimer(0);
        accessor.sparkstrength$setBeeping(false);
        accessor.sparkstrength$setBomberUuid(bomberUuid);
        accessor.sparkstrength$setLastDisplayedSeconds(-1);
        component.sync();
        return true;
    }

    public static void recordTrapPlacement(ServerPlayerEntity planter, Identifier eventId, BlockPos pos) {
        NbtCompound extra = new NbtCompound();
        GameRecordManager.putBlockPos(extra, "pos", pos);
        GameRecordManager.recordGlobalEvent(planter.getServerWorld(), eventId, planter, extra);
    }

    public static void recordTrapTrigger(ServerPlayerEntity victim, Identifier eventId, @Nullable UUID bomberUuid) {
        NbtCompound extra = new NbtCompound();
        if (bomberUuid != null) {
            extra.putUuid("bomber", bomberUuid);
        }
        GameRecordManager.recordGlobalEvent(victim.getServerWorld(), eventId, victim, extra);
    }

    public static void rememberTrap(@Nullable World world, BlockPos pos) {
        if (world == null || world.isClient) {
            return;
        }
        TRACKED_TRAPS.computeIfAbsent(world.getRegistryKey(), key -> new HashSet<>()).add(pos.toImmutable());
    }

    public static void forgetTrap(@Nullable World world, BlockPos pos) {
        if (world == null || world.isClient) {
            return;
        }
        Set<BlockPos> positions = TRACKED_TRAPS.get(world.getRegistryKey());
        if (positions == null) {
            return;
        }
        positions.remove(pos);
        if (positions.isEmpty()) {
            TRACKED_TRAPS.remove(world.getRegistryKey());
        }
    }

    public static void clearRoundState(ServerWorld world) {
        Set<BlockPos> positions = TRACKED_TRAPS.remove(world.getRegistryKey());
        if (positions == null || positions.isEmpty()) {
            return;
        }

        /*
         * wathe 正常会在终局重置地图；这里额外按已记录位置清状态，
         * 防止地图重置失败或同一张图局内复用时，方块实体 NBT 里残留上一局炸弹。
         */
        for (BlockPos pos : positions) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof TimedBombTrapHolder holder && holder.sparkstrength$hasTimedBombTrap()) {
                holder.sparkstrength$setTimedBombTrapOwner(null);
            }
        }
    }
}
