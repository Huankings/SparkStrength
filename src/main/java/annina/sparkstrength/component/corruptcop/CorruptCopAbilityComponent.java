package annina.sparkstrength.component.corruptcop;

import annina.sparkstrength.SparkStrength;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

/**
 * Owner-only runtime state for the Corrupt Cop ability.
 * 黑警主动技能仅同步给本人的运行时状态。
 */
public final class CorruptCopAbilityComponent implements AutoSyncedComponent {
    public static final ComponentKey<CorruptCopAbilityComponent> KEY = ComponentRegistry.getOrCreate(
            SparkStrength.id("corrupt_cop_ability"),
            CorruptCopAbilityComponent.class
    );

    private final PlayerEntity player;
    private boolean active;

    public CorruptCopAbilityComponent(PlayerEntity player) {
        this.player = player;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        if (this.active == active) {
            return;
        }
        this.active = active;
        sync();
    }

    public void reset() {
        setActive(false);
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity recipient) {
        // The toggle is private HUD/audio state and is never disclosed to other players.
        // 开关只服务于本人的 HUD 与音乐，绝不向其他玩家披露。
        return recipient == player;
    }

    @Override
    public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient) {
        buf.writeBoolean(active);
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        active = buf.readBoolean();
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        // Runtime-only by contract: reconnecting or respawning always starts inactive.
        // 按契约仅保留运行态：重连或重生后始终从关闭状态开始。
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        active = false;
    }

    private void sync() {
        KEY.sync(player);
    }
}
