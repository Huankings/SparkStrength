package annina.sparkstrength.component.phantom;

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
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

/**
 * 幽灵背包按钮的“使用者冷却”。
 *
 * <p>NoellesRoles 原幽灵按键隐身使用通用能力冷却；这里单独保存背包按钮冷却，
 * 保证两个技能互不影响，同时只同步给幽灵本人用于背包头像上的倒计时显示。</p>
 */
public class PhantomBackpackUserComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<PhantomBackpackUserComponent> KEY = ComponentRegistry.getOrCreate(
            SparkStrength.id("phantom_backpack_user"),
            PhantomBackpackUserComponent.class
    );

    private final PlayerEntity player;
    private int cooldownTicks;

    public PhantomBackpackUserComponent(PlayerEntity player) {
        this.player = player;
    }

    public int getCooldownTicks() {
        return cooldownTicks;
    }

    public boolean isOnCooldown() {
        return cooldownTicks > 0;
    }

    public void setCooldownTicks(int cooldownTicks) {
        this.cooldownTicks = Math.max(0, cooldownTicks);
        sync();
    }

    public void reset() {
        cooldownTicks = 0;
        sync();
    }

    private void sync() {
        KEY.sync(player);
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity recipient) {
        // 冷却只影响幽灵本人背包 UI，不向其它玩家暴露。
        return recipient == player;
    }

    @Override
    public void serverTick() {
        if (cooldownTicks <= 0) {
            return;
        }

        cooldownTicks--;
        if (cooldownTicks == 0 || cooldownTicks % 20 == 0) {
            sync();
        }
    }

    @Override
    public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient) {
        buf.writeVarInt(cooldownTicks);
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        cooldownTicks = buf.readVarInt();
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putInt("CooldownTicks", cooldownTicks);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        cooldownTicks = tag.getInt("CooldownTicks");
    }
}
