package annina.sparkstrength.component.morphling;

import annina.sparkstrength.SparkStrength;
import annina.sparkstrength.replay.SparkStrengthReplayFormatters;
import annina.sparkstrength.role.morphling.MorphlingRules;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ClientTickingComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

import java.util.UUID;

/**
 * 保存“某个玩家身上的变形试剂标记”。
 *
 * <p>组件挂在被标记玩家身上，而不是挂在 Morphling 身上，是为了让客户端渲染一个玩家时
 * 能直接从该玩家实体读取“我现在是否要显示成别人”。这比在所有 Morphling 身上反查标记
 * 更稳定，也能自然支持多个玩家同时被遥控触发。</p>
 *
 * <p>同步时会按接收者裁剪：未触发的标记只让施加标记的 Morphling 看见；已经触发的变形
 * 需要所有客户端知道，才能正确换皮肤、披风、模型和名字。</p>
 */
public class MorphMarkPlayerComponent implements AutoSyncedComponent, ServerTickingComponent, ClientTickingComponent {
    public static final ComponentKey<MorphMarkPlayerComponent> KEY = ComponentRegistry.getOrCreate(
            SparkStrength.id("morph_mark_player"),
            MorphMarkPlayerComponent.class
    );

    private final PlayerEntity player;
    private @Nullable UUID markerUuid;
    private @Nullable UUID sampleUuid;
    private String sampleName = "";
    private String markedName = "";
    private int activeTicks;

    public MorphMarkPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public void setPending(ServerPlayerEntity marker, UUID sampleUuid, String sampleName, String markedName) {
        this.markerUuid = marker.getUuid();
        this.sampleUuid = sampleUuid;
        this.sampleName = sampleName == null ? "" : sampleName;
        this.markedName = markedName == null ? "" : markedName;
        this.activeTicks = 0;
        sync();
    }

    public boolean activate() {
        if (markerUuid == null || sampleUuid == null) {
            return false;
        }
        this.activeTicks = MorphlingRules.REAGENT_ACTIVE_DURATION_TICKS;
        sync();
        return true;
    }

    public void clear() {
        this.markerUuid = null;
        this.sampleUuid = null;
        this.sampleName = "";
        this.markedName = "";
        this.activeTicks = 0;
        sync();
    }

    public boolean hasMark() {
        return markerUuid != null && sampleUuid != null;
    }

    public boolean isPending() {
        return hasMark() && activeTicks <= 0;
    }

    public boolean isActive() {
        return hasMark() && activeTicks > 0;
    }

    public boolean isMarkedBy(UUID uuid) {
        return uuid != null && uuid.equals(markerUuid) && hasMark();
    }

    public @Nullable UUID markerUuid() {
        return markerUuid;
    }

    public @Nullable UUID sampleUuid() {
        return sampleUuid;
    }

    public String sampleName() {
        return sampleName;
    }

    public String markedName() {
        return markedName;
    }

    public int activeTicks() {
        return activeTicks;
    }

    public void sync() {
        KEY.sync(this.player);
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity recipient) {
        // 始终允许发送“空状态”，否则 active 结束后非标记者客户端可能留着上一帧的伪装缓存。
        return recipient != null;
    }

    @Override
    public void serverTick() {
        if (!isActive()) {
            return;
        }

        if (!(player instanceof ServerPlayerEntity serverPlayer)
                || !GameFunctions.isPlayerPlayingAndAlive(serverPlayer)) {
            clear();
            return;
        }

        activeTicks--;
        if (activeTicks <= 0) {
            recordNaturalEnd(serverPlayer);
            clear();
            return;
        }

        if (activeTicks % 20 == 0) {
            sync();
        }
    }

    @Override
    public void clientTick() {
        if (activeTicks > 0) {
            activeTicks--;
        }
    }

    private void recordNaturalEnd(ServerPlayerEntity target) {
        if (target.getWorld() instanceof ServerWorld serverWorld) {
            GameRecordManager.recordGlobalEvent(
                    serverWorld,
                    SparkStrengthReplayFormatters.MORPH_MARK_ENDED,
                    target,
                    null
            );
        }
    }

    @Override
    public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient) {
        boolean visibleToRecipient = isActive()
                || (recipient != null && markerUuid != null && recipient.getUuid().equals(markerUuid));
        boolean hasVisibleData = visibleToRecipient && markerUuid != null && sampleUuid != null;
        buf.writeBoolean(hasVisibleData);
        if (!hasVisibleData) {
            return;
        }
        buf.writeUuid(markerUuid);
        buf.writeUuid(sampleUuid);
        buf.writeString(sampleName);
        buf.writeString(markedName);
        buf.writeVarInt(activeTicks);
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        if (!buf.readBoolean()) {
            markerUuid = null;
            sampleUuid = null;
            sampleName = "";
            markedName = "";
            activeTicks = 0;
            return;
        }
        markerUuid = buf.readUuid();
        sampleUuid = buf.readUuid();
        sampleName = buf.readString();
        markedName = buf.readString();
        activeTicks = buf.readVarInt();
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        if (markerUuid != null) {
            tag.putUuid("Marker", markerUuid);
        }
        if (sampleUuid != null) {
            tag.putUuid("Sample", sampleUuid);
        }
        tag.putString("SampleName", sampleName);
        tag.putString("MarkedName", markedName);
        tag.putInt("ActiveTicks", activeTicks);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        markerUuid = tag.containsUuid("Marker") ? tag.getUuid("Marker") : null;
        sampleUuid = tag.containsUuid("Sample") ? tag.getUuid("Sample") : null;
        sampleName = tag.getString("SampleName");
        markedName = tag.getString("MarkedName");
        activeTicks = tag.getInt("ActiveTicks");
    }
}
