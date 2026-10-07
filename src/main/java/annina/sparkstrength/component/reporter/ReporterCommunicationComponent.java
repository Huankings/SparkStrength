package annina.sparkstrength.component.reporter;

import annina.sparkstrength.SparkStrength;
import annina.sparkstrength.replay.SparkStrengthReplayFormatters;
import annina.sparkstrength.role.reporter.ReporterCommunicationConstants;
import annina.sparkstrength.role.reporter.ReporterCommunicationService;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

import java.util.UUID;

/**
 * 记者通讯加强的个人状态。
 *
 * <p>组件挂在记者本人身上，保存独立冷却、当前接线对象和当前广播对象。
 * 它刻意不使用 NoellesRoles 的 {@code AbilityPlayerComponent}，这样背包通讯不会影响
 * 原记者 G 键标记透视的 30 秒冷却。</p>
 */
public class ReporterCommunicationComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<ReporterCommunicationComponent> KEY = ComponentRegistry.getOrCreate(
            SparkStrength.id("reporter_communication"),
            ReporterCommunicationComponent.class
    );

    private final PlayerEntity player;

    private int cooldownTicks;

    private boolean connectionActive;
    private int connectionTicksRemaining;
    @Nullable private UUID connectionPlayerOne;
    @Nullable private UUID connectionPlayerTwo;
    private String connectionPlayerOneName = "";
    private String connectionPlayerTwoName = "";

    private boolean broadcastActive;
    private int broadcastTicksRemaining;
    @Nullable private UUID broadcastTarget;
    private String broadcastTargetName = "";

    public ReporterCommunicationComponent(PlayerEntity player) {
        this.player = player;
    }

    public int getCooldownTicks() {
        return this.cooldownTicks;
    }

    public boolean isOnCooldown() {
        return this.cooldownTicks > 0;
    }

    public void setCooldownTicks(int cooldownTicks) {
        this.cooldownTicks = Math.max(0, cooldownTicks);
        this.sync();
    }

    public void startConnection(@NotNull ServerPlayerEntity first, @NotNull ServerPlayerEntity second) {
        this.connectionActive = true;
        this.connectionTicksRemaining = ReporterCommunicationConstants.CONNECTION_DURATION_TICKS;
        this.connectionPlayerOne = first.getUuid();
        this.connectionPlayerTwo = second.getUuid();
        this.connectionPlayerOneName = first.getGameProfile().getName();
        this.connectionPlayerTwoName = second.getGameProfile().getName();

        // 记者同一时间只保留一种通讯效果，开启接线时清掉旧广播。
        this.broadcastActive = false;
        this.broadcastTicksRemaining = 0;
        this.broadcastTarget = null;
        this.broadcastTargetName = "";
        this.sync();
    }

    public void startBroadcast(@NotNull ServerPlayerEntity target) {
        this.broadcastActive = true;
        this.broadcastTicksRemaining = ReporterCommunicationConstants.BROADCAST_DURATION_TICKS;
        this.broadcastTarget = target.getUuid();
        this.broadcastTargetName = target.getGameProfile().getName();

        // 同理，开启广播时清掉旧接线。
        this.connectionActive = false;
        this.connectionTicksRemaining = 0;
        this.connectionPlayerOne = null;
        this.connectionPlayerTwo = null;
        this.connectionPlayerOneName = "";
        this.connectionPlayerTwoName = "";
        this.sync();
    }

    public boolean hasActiveConnection() {
        return this.connectionActive && this.connectionPlayerOne != null && this.connectionPlayerTwo != null;
    }

    public boolean hasActiveBroadcast() {
        return this.broadcastActive && this.broadcastTarget != null;
    }

    public @Nullable UUID getConnectionPlayerOne() {
        return this.connectionPlayerOne;
    }

    public @Nullable UUID getConnectionPlayerTwo() {
        return this.connectionPlayerTwo;
    }

    public @Nullable UUID getBroadcastTarget() {
        return this.broadcastTarget;
    }

    public void reset() {
        this.cooldownTicks = 0;
        this.clearActiveState();
        this.sync();
    }

    public void sync() {
        KEY.sync(this.player);
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity recipient) {
        // 冷却和选中状态只影响记者本人背包 UI，其他玩家不需要知道。
        return recipient == this.player;
    }

    @Override
    public void serverTick() {
        if (!(this.player instanceof ServerPlayerEntity reporter)) {
            return;
        }

        if (!isValidReporterOwner(reporter)) {
            this.reset();
            return;
        }

        if (this.cooldownTicks > 0) {
            this.cooldownTicks--;
            if (this.cooldownTicks == 0 || this.cooldownTicks % 20 == 0) {
                this.sync();
            }
        }

        this.tickConnection(reporter);
        this.tickBroadcast(reporter);
    }

    private boolean isValidReporterOwner(@NotNull ServerPlayerEntity reporter) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(reporter.getWorld());
        return gameWorld.isRunning()
                && gameWorld.isRole(reporter, Noellesroles.REPORTER)
                && GameFunctions.isPlayerPlayingAndAlive(reporter)
                && GameFunctions.isPlayerAliveAndSurvival(reporter)
                && !SwallowedPlayerComponent.isPlayerSwallowed(reporter);
    }

    private void tickConnection(@NotNull ServerPlayerEntity reporter) {
        if (!this.hasActiveConnection()) {
            return;
        }

        ServerPlayerEntity first = this.getOnlinePlayer(reporter, this.connectionPlayerOne);
        ServerPlayerEntity second = this.getOnlinePlayer(reporter, this.connectionPlayerTwo);

        if (!ReporterCommunicationService.isCommunicableEndpoint(first)) {
            this.finishConnection(reporter, true, this.connectionPlayerOne, this.connectionPlayerOneName);
            return;
        }
        if (!ReporterCommunicationService.isCommunicableEndpoint(second)) {
            this.finishConnection(reporter, true, this.connectionPlayerTwo, this.connectionPlayerTwoName);
            return;
        }

        this.connectionTicksRemaining--;
        if (this.connectionTicksRemaining <= 0) {
            this.finishConnection(reporter, false, null, "");
        } else if (this.connectionTicksRemaining % 20 == 0) {
            this.sync();
        }
    }

    private void tickBroadcast(@NotNull ServerPlayerEntity reporter) {
        if (!this.hasActiveBroadcast()) {
            return;
        }

        ServerPlayerEntity target = this.getOnlinePlayer(reporter, this.broadcastTarget);
        if (!ReporterCommunicationService.isCommunicableEndpoint(target)) {
            this.finishBroadcast(reporter, true);
            return;
        }

        this.broadcastTicksRemaining--;
        if (this.broadcastTicksRemaining <= 0) {
            this.finishBroadcast(reporter, false);
        } else if (this.broadcastTicksRemaining % 20 == 0) {
            this.sync();
        }
    }

    private @Nullable ServerPlayerEntity getOnlinePlayer(@NotNull ServerPlayerEntity reporter, @Nullable UUID uuid) {
        return uuid == null ? null : reporter.getServer().getPlayerManager().getPlayer(uuid);
    }

    private void finishConnection(
            @NotNull ServerPlayerEntity reporter,
            boolean interrupted,
            @Nullable UUID deadPlayerUuid,
            @NotNull String deadPlayerName
    ) {
        if (!this.hasActiveConnection()) {
            this.clearConnectionState();
            this.sync();
            return;
        }

        NbtCompound extra = new NbtCompound();
        this.writeConnectionExtra(extra);
        if (interrupted) {
            if (deadPlayerUuid != null) {
                extra.putUuid("dead_player", deadPlayerUuid);
            }
            extra.putString("dead_player_name", deadPlayerName);
        }
        GameRecordManager.recordGlobalEvent(
                reporter.getServerWorld(),
                interrupted
                        ? SparkStrengthReplayFormatters.REPORTER_CONNECTION_INTERRUPTED
                        : SparkStrengthReplayFormatters.REPORTER_CONNECTION_ENDED,
                reporter,
                extra
        );

        this.clearConnectionState();
        this.sync();
    }

    private void finishBroadcast(@NotNull ServerPlayerEntity reporter, boolean interrupted) {
        if (!this.hasActiveBroadcast()) {
            this.clearBroadcastState();
            this.sync();
            return;
        }

        NbtCompound extra = new NbtCompound();
        this.writeBroadcastExtra(extra);
        if (interrupted && this.broadcastTarget != null) {
            extra.putUuid("dead_player", this.broadcastTarget);
            extra.putString("dead_player_name", this.broadcastTargetName);
        }
        GameRecordManager.recordGlobalEvent(
                reporter.getServerWorld(),
                interrupted
                        ? SparkStrengthReplayFormatters.REPORTER_BROADCAST_INTERRUPTED
                        : SparkStrengthReplayFormatters.REPORTER_BROADCAST_ENDED,
                reporter,
                extra
        );

        this.clearBroadcastState();
        this.sync();
    }

    private void writeConnectionExtra(@NotNull NbtCompound extra) {
        if (this.connectionPlayerOne != null) {
            extra.putUuid("player_one", this.connectionPlayerOne);
        }
        if (this.connectionPlayerTwo != null) {
            extra.putUuid("player_two", this.connectionPlayerTwo);
        }
        extra.putString("player_one_name", this.connectionPlayerOneName);
        extra.putString("player_two_name", this.connectionPlayerTwoName);
    }

    private void writeBroadcastExtra(@NotNull NbtCompound extra) {
        if (this.broadcastTarget != null) {
            extra.putUuid("target_player", this.broadcastTarget);
        }
        extra.putString("target_player_name", this.broadcastTargetName);
    }

    private void clearActiveState() {
        this.clearConnectionState();
        this.clearBroadcastState();
    }

    private void clearConnectionState() {
        this.connectionActive = false;
        this.connectionTicksRemaining = 0;
        this.connectionPlayerOne = null;
        this.connectionPlayerTwo = null;
        this.connectionPlayerOneName = "";
        this.connectionPlayerTwoName = "";
    }

    private void clearBroadcastState() {
        this.broadcastActive = false;
        this.broadcastTicksRemaining = 0;
        this.broadcastTarget = null;
        this.broadcastTargetName = "";
    }

    @Override
    public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient) {
        buf.writeVarInt(this.cooldownTicks);
        buf.writeBoolean(this.connectionActive);
        buf.writeVarInt(this.connectionTicksRemaining);
        buf.writeBoolean(this.broadcastActive);
        buf.writeVarInt(this.broadcastTicksRemaining);
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        this.cooldownTicks = buf.readVarInt();
        this.connectionActive = buf.readBoolean();
        this.connectionTicksRemaining = buf.readVarInt();
        this.broadcastActive = buf.readBoolean();
        this.broadcastTicksRemaining = buf.readVarInt();
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putInt("CooldownTicks", this.cooldownTicks);

        tag.putBoolean("ConnectionActive", this.connectionActive);
        tag.putInt("ConnectionTicksRemaining", this.connectionTicksRemaining);
        if (this.connectionPlayerOne != null) {
            tag.putUuid("ConnectionPlayerOne", this.connectionPlayerOne);
        }
        if (this.connectionPlayerTwo != null) {
            tag.putUuid("ConnectionPlayerTwo", this.connectionPlayerTwo);
        }
        tag.putString("ConnectionPlayerOneName", this.connectionPlayerOneName);
        tag.putString("ConnectionPlayerTwoName", this.connectionPlayerTwoName);

        tag.putBoolean("BroadcastActive", this.broadcastActive);
        tag.putInt("BroadcastTicksRemaining", this.broadcastTicksRemaining);
        if (this.broadcastTarget != null) {
            tag.putUuid("BroadcastTarget", this.broadcastTarget);
        }
        tag.putString("BroadcastTargetName", this.broadcastTargetName);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.cooldownTicks = Math.max(0, tag.getInt("CooldownTicks"));

        this.connectionActive = tag.getBoolean("ConnectionActive");
        this.connectionTicksRemaining = Math.max(0, tag.getInt("ConnectionTicksRemaining"));
        this.connectionPlayerOne = tag.containsUuid("ConnectionPlayerOne") ? tag.getUuid("ConnectionPlayerOne") : null;
        this.connectionPlayerTwo = tag.containsUuid("ConnectionPlayerTwo") ? tag.getUuid("ConnectionPlayerTwo") : null;
        this.connectionPlayerOneName = tag.getString("ConnectionPlayerOneName");
        this.connectionPlayerTwoName = tag.getString("ConnectionPlayerTwoName");

        this.broadcastActive = tag.getBoolean("BroadcastActive");
        this.broadcastTicksRemaining = Math.max(0, tag.getInt("BroadcastTicksRemaining"));
        this.broadcastTarget = tag.containsUuid("BroadcastTarget") ? tag.getUuid("BroadcastTarget") : null;
        this.broadcastTargetName = tag.getString("BroadcastTargetName");
    }
}
