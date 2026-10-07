package annina.sparkstrength.component.shadowjester;

import annina.sparkstrength.SparkStrength;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

import java.util.UUID;

/**
 * 保存 SparkStrength 新版“双影谢幕”的世界级状态。
 *
 * <p>这个组件专门对应“最后一名有效杀手死亡后，影子小丑转而清场”的新机制。
 * NoellesRoles 原有的 {@code ShadowJesterPlayerComponent.showdownActive} 仍然只代表
 * 原版“与杀手对战”的旧双影谢幕，两个状态不能混用。</p>
 */
public final class ShadowJesterShowdownWorldComponent implements AutoSyncedComponent {
    public static final ComponentKey<ShadowJesterShowdownWorldComponent> KEY =
            ComponentRegistry.getOrCreate(
                    SparkStrength.id("shadow_jester_showdown"),
                    ShadowJesterShowdownWorldComponent.class
            );

    private final World world;
    private boolean active;
    private @Nullable UUID firstJester;
    private @Nullable UUID secondJester;

    public ShadowJesterShowdownWorldComponent(World world) {
        this.world = world;
    }

    public boolean isActive() {
        return active;
    }

    public @Nullable UUID getFirstJester() {
        return firstJester;
    }

    public @Nullable UUID getSecondJester() {
        return secondJester;
    }

    /**
     * 开始新版谢幕，并同步给所有客户端。
     */
    public void start(UUID firstJester, UUID secondJester) {
        this.active = true;
        this.firstJester = firstJester;
        this.secondJester = secondJester;
        sync();
    }

    /**
     * 结束新版谢幕并同步状态，让客户端停止或淡出环境音。
     */
    public void clear() {
        boolean changed = this.active || this.firstJester != null || this.secondJester != null;
        this.active = false;
        this.firstJester = null;
        this.secondJester = null;
        if (changed) {
            sync();
        }
    }

    public void clearRoundState() {
        clear();
    }

    public void sync() {
        KEY.sync(this.world);
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity player) {
        // 新谢幕是全场状态，因此所有在线玩家都要收到同步。
        return true;
    }

    @Override
    public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient) {
        buf.writeBoolean(active);
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        this.active = buf.readBoolean();
        // UUID 只用于服务端判定，客户端只需要知道是否播放环境音。
        if (!this.active) {
            this.firstJester = null;
            this.secondJester = null;
        }
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putBoolean("Active", active);
        if (firstJester != null) {
            tag.putUuid("FirstJester", firstJester);
        }
        if (secondJester != null) {
            tag.putUuid("SecondJester", secondJester);
        }
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        active = tag.getBoolean("Active");
        firstJester = tag.containsUuid("FirstJester") ? tag.getUuid("FirstJester") : null;
        secondJester = tag.containsUuid("SecondJester") ? tag.getUuid("SecondJester") : null;
        if (!active) {
            firstJester = null;
            secondJester = null;
        }
    }
}
