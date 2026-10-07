package annina.sparkstrength.component.waiter;

import annina.sparkstrength.SparkStrength;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ClientTickingComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

/**
 * 记录“玩家刚完成任务后，服务员可以看到该玩家”的倒计时。
 *
 * <p>组件挂在完成任务的目标玩家身上，而不是挂在服务员身上。这样每个目标只需要
 * 同步一个简单的倒计时，客户端高亮事件再判断当前观看者是否是服务员或服务员伪装。</p>
 */
public final class WaiterTaskRevealComponent
        implements AutoSyncedComponent, ServerTickingComponent, ClientTickingComponent {
    public static final ComponentKey<WaiterTaskRevealComponent> KEY = ComponentRegistry.getOrCreate(
            SparkStrength.id("waiter_task_reveal"), WaiterTaskRevealComponent.class
    );

    /** 任务完成后可被服务员透视的持续时间：30 秒。 */
    public static final int REVEAL_DURATION_TICKS = 30 * 20;

    private final PlayerEntity player;
    private int revealTicks;

    public WaiterTaskRevealComponent(PlayerEntity player) {
        this.player = player;
    }

    /** 开始或刷新透视时间；连续完成任务不会叠加，只会刷新到至少 30 秒。 */
    public void startReveal() {
        this.revealTicks = Math.max(this.revealTicks, REVEAL_DURATION_TICKS);
        this.sync();
    }

    public boolean isRevealed() {
        return this.revealTicks > 0;
    }

    public void reset() {
        this.revealTicks = 0;
        this.sync();
    }

    public void sync() {
        KEY.sync(this.player);
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity recipient) {
        // 所有客户端都需要收到目标状态；是否显示由客户端再判断观看者身份。
        return recipient != null;
    }

    @Override
    public void serverTick() {
        if (this.revealTicks <= 0) {
            return;
        }
        if (!(this.player instanceof ServerPlayerEntity serverPlayer)
                || !GameFunctions.isPlayerPlayingAndAlive(serverPlayer)) {
            this.reset();
            return;
        }

        this.revealTicks--;
        if (this.revealTicks == 0 || this.revealTicks % 20 == 0) {
            this.sync();
        }
    }

    @Override
    public void clientTick() {
        // 客户端本地递减，让透视结束不必等待下一次服务端同步。
        if (this.revealTicks > 0) {
            this.revealTicks--;
        }
    }

    @Override
    public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient) {
        buf.writeVarInt(this.revealTicks);
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        this.revealTicks = buf.readVarInt();
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putInt("RevealTicks", this.revealTicks);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.revealTicks = Math.max(0, tag.getInt("RevealTicks"));
    }
}
