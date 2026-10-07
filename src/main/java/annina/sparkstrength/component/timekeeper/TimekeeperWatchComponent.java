package annina.sparkstrength.component.timekeeper;

import annina.sparkstrength.SparkStrength;
import annina.sparkstrength.role.timekeeper.TimekeeperConstants;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

/**
 * 计时员怀表的两个独立自身冷却。
 * 物品刷新和技能刷新不能共用一个计时器，因为两种模式的冷却分别是 40 秒和 30 秒。
 */
public final class TimekeeperWatchComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<TimekeeperWatchComponent> KEY = ComponentRegistry.getOrCreate(
            SparkStrength.id("timekeeper_watch"),
            TimekeeperWatchComponent.class
    );

    private final PlayerEntity player;
    private int itemRefreshCooldownTicks;
    private int abilityRefreshCooldownTicks;

    public TimekeeperWatchComponent(PlayerEntity player) {
        this.player = player;
    }

    public int getItemRefreshCooldownTicks() {
        return itemRefreshCooldownTicks;
    }

    public int getAbilityRefreshCooldownTicks() {
        return abilityRefreshCooldownTicks;
    }

    public boolean isOnCooldown(boolean itemRefresh) {
        return (itemRefresh ? itemRefreshCooldownTicks : abilityRefreshCooldownTicks) > 0;
    }

    public void startCooldown(boolean itemRefresh) {
        if (itemRefresh) {
            itemRefreshCooldownTicks = TimekeeperConstants.ITEM_REFRESH_COOLDOWN_TICKS;
        } else {
            abilityRefreshCooldownTicks = TimekeeperConstants.ABILITY_REFRESH_COOLDOWN_TICKS;
        }
        sync();
    }

    public void reset() {
        itemRefreshCooldownTicks = 0;
        abilityRefreshCooldownTicks = 0;
        sync();
    }

    public void sync() {
        KEY.sync(player);
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity recipient) {
        return recipient == player;
    }

    @Override
    public void serverTick() {
        boolean shouldSync = false;
        if (itemRefreshCooldownTicks > 0) {
            itemRefreshCooldownTicks--;
            shouldSync = itemRefreshCooldownTicks == 0 || itemRefreshCooldownTicks % 20 == 0;
        }
        if (abilityRefreshCooldownTicks > 0) {
            abilityRefreshCooldownTicks--;
            shouldSync |= abilityRefreshCooldownTicks == 0 || abilityRefreshCooldownTicks % 20 == 0;
        }
        if (shouldSync) {
            sync();
        }
    }

    @Override
    public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient) {
        buf.writeVarInt(itemRefreshCooldownTicks);
        buf.writeVarInt(abilityRefreshCooldownTicks);
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        itemRefreshCooldownTicks = Math.max(0, buf.readVarInt());
        abilityRefreshCooldownTicks = Math.max(0, buf.readVarInt());
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putInt("ItemRefreshCooldownTicks", itemRefreshCooldownTicks);
        tag.putInt("AbilityRefreshCooldownTicks", abilityRefreshCooldownTicks);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        itemRefreshCooldownTicks = Math.max(0, tag.getInt("ItemRefreshCooldownTicks"));
        abilityRefreshCooldownTicks = Math.max(0, tag.getInt("AbilityRefreshCooldownTicks"));
    }
}
