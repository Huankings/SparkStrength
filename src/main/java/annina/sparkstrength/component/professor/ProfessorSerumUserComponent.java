package annina.sparkstrength.component.professor;

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
 * 教授远程投喂背包技能的使用者冷却。
 *
 * <p>冷却只同步给教授本人，用于背包头像和试剂按钮显示剩余秒数；
 * 服务端依旧会在收到网络包后重新检查冷却，客户端显示只负责交互体验。</p>
 */
public class ProfessorSerumUserComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<ProfessorSerumUserComponent> KEY = ComponentRegistry.getOrCreate(
            SparkStrength.id("professor_serum_user"),
            ProfessorSerumUserComponent.class
    );

    private final PlayerEntity player;
    private int cooldownTicks;

    public ProfessorSerumUserComponent(PlayerEntity player) {
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
        this.cooldownTicks = 0;
        sync();
    }

    private void sync() {
        KEY.sync(this.player);
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity recipient) {
        return recipient == this.player;
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
