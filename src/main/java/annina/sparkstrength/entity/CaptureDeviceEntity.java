package annina.sparkstrength.entity;

import annina.sparkstrength.SparkStrengthEntities;
import annina.sparkstrength.component.engineer.EngineerStunnedPlayerComponent;
import annina.sparkstrength.replay.SparkStrengthReplayFormatters;
import annina.sparkstrength.role.engineer.EngineerCaptureReport;
import annina.sparkstrength.role.engineer.EngineerRules;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Placed capture-device entity and its server lifecycle.
 * 放置后的捕捉装置实体。
 *
 * <p>The entity owns detection, capture effects, reports, expiry, and related replay events.
 * <p>实体在服务端独立 tick：120 秒未触发就自然消失；触发时只捕捉放置者以外的存活玩家。
 * 放置者 UUID 和天花板/地板状态使用 DataTracker 同步给客户端，客户端据此决定是否渲染。</p>
 */
public final class CaptureDeviceEntity extends Entity {
    private static final TrackedData<Optional<UUID>> OWNER_UUID =
            DataTracker.registerData(CaptureDeviceEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
    private static final TrackedData<Boolean> CEILING_MOUNTED =
            DataTracker.registerData(CaptureDeviceEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    private int lifetimeTicks;

    public CaptureDeviceEntity(EntityType<? extends CaptureDeviceEntity> entityType, World world) {
        super(entityType, world);
        this.noClip = true;
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(OWNER_UUID, Optional.empty());
        builder.add(CEILING_MOUNTED, false);
    }

    @Override
    public void tick() {
        super.tick();
        if (getWorld().isClient()) {
            return;
        }

        lifetimeTicks++;
        if (lifetimeTicks >= EngineerRules.CAPTURE_MAX_LIFETIME_TICKS) {
            recordExpired();
            discard();
            return;
        }

        List<ServerPlayerEntity> capturedPlayers = findCapturedPlayers();
        if (!capturedPlayers.isEmpty()) {
            trigger(capturedPlayers);
            discard();
        }
    }

    public void setOwnerUuid(UUID ownerUuid) {
        this.dataTracker.set(OWNER_UUID, Optional.ofNullable(ownerUuid));
    }

    public @Nullable UUID getOwnerUuid() {
        return this.dataTracker.get(OWNER_UUID).orElse(null);
    }

    public void setCeilingMounted(boolean ceilingMounted) {
        this.dataTracker.set(CEILING_MOUNTED, ceilingMounted);
    }

    public boolean isCeilingMounted() {
        return this.dataTracker.get(CEILING_MOUNTED);
    }

    @Override
    public boolean shouldRender(double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        if (nbt.containsUuid("Owner")) {
            setOwnerUuid(nbt.getUuid("Owner"));
        }
        if (nbt.contains("CeilingMounted")) {
            setCeilingMounted(nbt.getBoolean("CeilingMounted"));
        }
        lifetimeTicks = nbt.getInt("LifetimeTicks");
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        UUID ownerUuid = getOwnerUuid();
        if (ownerUuid != null) {
            nbt.putUuid("Owner", ownerUuid);
        }
        nbt.putBoolean("CeilingMounted", isCeilingMounted());
        nbt.putInt("LifetimeTicks", lifetimeTicks);
    }

    private List<ServerPlayerEntity> findCapturedPlayers() {
        if (!(getWorld() instanceof ServerWorld serverWorld)) {
            return List.of();
        }

        UUID ownerUuid = getOwnerUuid();
        Box box = getBoundingBox().expand(EngineerRules.CAPTURE_RADIUS);
        return serverWorld.getEntitiesByClass(ServerPlayerEntity.class, box, player ->
                isCaptureCandidate(player, ownerUuid)
                        && player.squaredDistanceTo(this) <= EngineerRules.CAPTURE_RADIUS_SQUARED
        );
    }

    private boolean isCaptureCandidate(ServerPlayerEntity player, @Nullable UUID ownerUuid) {
        return !player.getUuid().equals(ownerUuid)
                && GameFunctions.isPlayerPlayingAndAlive(player)
                && GameFunctions.isPlayerAliveAndSurvival(player);
    }

    private void trigger(List<ServerPlayerEntity> capturedPlayers) {
        for (ServerPlayerEntity player : capturedPlayers) {
            // CCA stores and syncs stun state; the client blocks input while the server locks position and velocity.
            // 定身状态由 CCA 组件保存并同步：客户端负责禁输入，服务端负责锁位置与清速度。
            EngineerStunnedPlayerComponent.KEY.get(player).stun(EngineerRules.CAPTURE_STUN_TICKS);
            player.sendMessage(Text.translatable("message.sparkstrength.capture_device.captured")
                    .formatted(Formatting.RED), true);
            player.playSoundToPlayer(SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.PLAYERS, 1.0F, 1.0F);
            GameRecordManager.recordGlobalEvent(
                    player.getServerWorld(),
                    SparkStrengthReplayFormatters.CAPTURE_DEVICE_TRIGGERED,
                    player,
                    null
            );
        }

        giveReport(capturedPlayers);
    }

    private void giveReport(List<ServerPlayerEntity> capturedPlayers) {
        UUID ownerUuid = getOwnerUuid();
        if (ownerUuid == null || !(getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }

        PlayerEntity owner = serverWorld.getPlayerByUuid(ownerUuid);
        if (owner == null) {
            return;
        }

        EngineerCaptureReport.removeOldReports(owner);

        ItemStack reportStack = Items.PAPER.getDefaultStack();
        reportStack.set(DataComponentTypes.CUSTOM_NAME,
                Text.translatable("item.sparkstrength.capture_report").formatted(Formatting.RESET, Formatting.GOLD));
        EngineerCaptureReport.mark(reportStack, owner.getUuid());

        List<Text> loreLines = new ArrayList<>();
        loreLines.add(Text.translatable("item.sparkstrength.capture_report.tooltip", capturedPlayers.size())
                .formatted(Formatting.GRAY));
        for (ServerPlayerEntity capturedPlayer : capturedPlayers) {
            loreLines.add(Text.literal(" - " + capturedPlayer.getName().getString())
                    .formatted(Formatting.WHITE));
        }
        reportStack.set(DataComponentTypes.LORE, new LoreComponent(loreLines));

        owner.getInventory().offerOrDrop(reportStack);
        if (owner instanceof ServerPlayerEntity serverPlayer) {
            serverPlayer.playSoundToPlayer(SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.PLAYERS, 1.0F, 1.0F);
        }
    }

    private void recordExpired() {
        if (!(getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }

        UUID ownerUuid = getOwnerUuid();
        PlayerEntity ownerPlayer = ownerUuid == null ? null : serverWorld.getPlayerByUuid(ownerUuid);
        ServerPlayerEntity owner = ownerPlayer instanceof ServerPlayerEntity serverPlayer ? serverPlayer : null;
        NbtCompound extra = null;
        if (owner == null && ownerUuid != null) {
            extra = new NbtCompound();
            extra.putUuid("actor", ownerUuid);
        }
        GameRecordManager.recordGlobalEvent(
                serverWorld,
                SparkStrengthReplayFormatters.CAPTURE_DEVICE_EXPIRED,
                owner,
                extra
        );
    }
}
