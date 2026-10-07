package annina.sparkstrength.component.collision;

import annina.sparkstrength.SparkStrength;
import annina.sparkstrength.collision.PlayerCollisionGraceRules;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

/**
 * 保存当前世界本局“开局无 Wathe 玩家实体墙”保护期的起点。
 *
 * <p>状态放在世界组件而不是玩家组件中，是因为碰撞是玩家对之间的全局规则，
 * 并且所有客户端都必须使用同一个起始 tick 做本地移动预测。服务端在
 * {@code GameEvents.ON_FINISH_INITIALIZE} 中写入起点，客户端通过 CCA 同步该值。</p>
 */
public final class PlayerCollisionGraceWorldComponent implements AutoSyncedComponent {
    public static final ComponentKey<PlayerCollisionGraceWorldComponent> KEY = ComponentRegistry.getOrCreate(
            SparkStrength.id("player_collision_grace"),
            PlayerCollisionGraceWorldComponent.class
    );

    private static final String ROUND_START_TICK_TAG = "RoundStartTick";

    private final World world;

    /** -1 表示当前没有已经记录的 ACTIVE 回合起点。 */
    private long roundStartTick = -1L;

    public PlayerCollisionGraceWorldComponent(World world) {
        this.world = world;
    }

    /**
     * 记录本局正式开局时的世界 tick，并立即同步给客户端。
     *
     * <p>调用点位于 Wathe 初始化流程的 ON_FINISH_INITIALIZE 回调中，此时玩家已经完成传送、
     * 职业分配和扩展初始化，但 GameWorldComponent 尚未切换到 ACTIVE；随后 Wathe 会切换状态
     * 并再次同步，所以客户端能够在保护期开始时拿到完整状态。</p>
     */
    public void markRoundStart(long worldTime) {
        this.roundStartTick = worldTime;
        sync();
    }

    /**
     * 清理上一局的起点，避免下一局在尚未初始化时误读旧的保护时间。
     */
    public void clearRoundState() {
        if (this.roundStartTick == -1L) {
            return;
        }
        this.roundStartTick = -1L;
        sync();
    }

    /**
     * 判断当前是否处于开局无碰撞保护期。
     *
     * <p>保护只在 ACTIVE 状态生效；STARTING 阶段还没有正式开局，STOPPING 阶段也不应重新
     * 打开保护。实际碰撞 Mixin 还会检查双方是否为玩家以及原始碰撞结果是否为 true。</p>
     */
    public boolean isGracePeriodActive() {
        if (GameWorldComponent.KEY.get(world).getGameStatus() != GameWorldComponent.GameStatus.ACTIVE) {
            return false;
        }
        if (roundStartTick < 0L || PlayerCollisionGraceRules.START_NO_COLLISION_TICKS <= 0L) {
            return false;
        }

        long elapsedTicks = world.getTime() - roundStartTick;
        return elapsedTicks >= 0L && elapsedTicks < PlayerCollisionGraceRules.START_NO_COLLISION_TICKS;
    }

    public void sync() {
        if (world != null) {
            KEY.sync(world);
        }
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity player) {
        // 开局碰撞会在每个客户端本地参与移动预测，因此所有在线玩家都需要该起点。
        return true;
    }

    @Override
    public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient) {
        buf.writeLong(roundStartTick);
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        roundStartTick = buf.readLong();
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putLong(ROUND_START_TICK_TAG, roundStartTick);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        roundStartTick = tag.contains(ROUND_START_TICK_TAG)
                ? tag.getLong(ROUND_START_TICK_TAG)
                : -1L;
    }
}
