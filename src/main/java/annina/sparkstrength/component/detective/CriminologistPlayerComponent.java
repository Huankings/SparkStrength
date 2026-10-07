package annina.sparkstrength.component.detective;

import annina.sparkstrength.SparkStrength;
import annina.sparkstrength.role.detective.CriminologistRules;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

import java.util.UUID;

/**
 * Stores the Detective's extra criminologist runtime.
 * 保存侦探“犯罪学家”第二技能的玩家运行态。
 *
 * <p>这个组件只负责侦探二技能：冷却、待选择尸体、正在追踪的目标以及周期显形计时。
 * 以前它叫 role_enhancements，容易让人误以为所有 NoellesRoles 增强都挤在这里；
 * 现在组件名和 CCA ID 都改成 criminologist，后续排查状态会更直观。</p>
 */
public final class CriminologistPlayerComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<CriminologistPlayerComponent> KEY = ComponentRegistry.getOrCreate(
            SparkStrength.id("criminologist_player"),
            CriminologistPlayerComponent.class
    );

    private final PlayerEntity player;
    private int criminologistCooldownTicks;
    private @Nullable UUID criminologistPendingVictimUuid;
    private @Nullable UUID criminologistTrackingTargetUuid;
    private int criminologistRevealTicks;
    private int criminologistRevealIntervalTicks;

    public CriminologistPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public int getCriminologistCooldownTicks() {
        return criminologistCooldownTicks;
    }

    public @Nullable UUID getCriminologistPendingVictimUuid() {
        return criminologistPendingVictimUuid;
    }

    public @Nullable UUID getCriminologistTrackingTargetUuid() {
        return criminologistTrackingTargetUuid;
    }

    public int getCriminologistRevealTicks() {
        return criminologistRevealTicks;
    }

    public boolean isCriminologistRevealing(UUID targetUuid) {
        return criminologistRevealTicks > 0 && targetUuid.equals(criminologistTrackingTargetUuid);
    }

    public boolean hasCriminologistTarget() {
        return criminologistTrackingTargetUuid != null;
    }

    public void initializeCriminologist() {
        criminologistCooldownTicks = CriminologistRules.INITIAL_COOLDOWN_TICKS;
        criminologistPendingVictimUuid = null;
        criminologistTrackingTargetUuid = null;
        criminologistRevealTicks = 0;
        criminologistRevealIntervalTicks = 0;
        sync();
    }

    public void setCriminologistPendingVictim(@Nullable UUID victimUuid) {
        if (victimUuid == null ? criminologistPendingVictimUuid == null : victimUuid.equals(criminologistPendingVictimUuid)) {
            return;
        }
        criminologistPendingVictimUuid = victimUuid;
        sync();
    }

    public void startCriminologistTracking(UUID targetUuid) {
        criminologistPendingVictimUuid = null;
        criminologistTrackingTargetUuid = targetUuid;
        criminologistCooldownTicks = 0;
        criminologistRevealTicks = CriminologistRules.REVEAL_TICKS;
        criminologistRevealIntervalTicks = CriminologistRules.REVEAL_INTERVAL_TICKS;
        sync();
    }

    public void startCriminologistCooldown() {
        criminologistPendingVictimUuid = null;
        criminologistTrackingTargetUuid = null;
        criminologistRevealTicks = 0;
        criminologistRevealIntervalTicks = 0;
        criminologistCooldownTicks = CriminologistRules.COOLDOWN_TICKS;
        sync();
    }

    /** 只刷新犯罪学家技能冷却，保留当前追踪目标和显形状态。 */
    public void clearCriminologistCooldown() {
        if (criminologistCooldownTicks == 0) {
            return;
        }
        criminologistCooldownTicks = 0;
        sync();
    }

    public void clearCriminologist() {
        if (criminologistCooldownTicks == 0
                && criminologistPendingVictimUuid == null
                && criminologistTrackingTargetUuid == null
                && criminologistRevealTicks == 0
                && criminologistRevealIntervalTicks == 0) {
            return;
        }
        criminologistCooldownTicks = 0;
        criminologistPendingVictimUuid = null;
        criminologistTrackingTargetUuid = null;
        criminologistRevealTicks = 0;
        criminologistRevealIntervalTicks = 0;
        sync();
    }

    public void clearAll() {
        boolean changed = criminologistCooldownTicks != 0
                || criminologistPendingVictimUuid != null
                || criminologistTrackingTargetUuid != null
                || criminologistRevealTicks != 0
                || criminologistRevealIntervalTicks != 0;
        criminologistCooldownTicks = 0;
        criminologistPendingVictimUuid = null;
        criminologistTrackingTargetUuid = null;
        criminologistRevealTicks = 0;
        criminologistRevealIntervalTicks = 0;
        if (changed) {
            sync();
        }
    }

    public void sync() {
        if (player != null) {
            KEY.sync(player);
        }
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity recipient) {
        return recipient == player || GameFunctions.isPlayerSpectatingOrCreative(recipient);
    }

    @Override
    public void serverTick() {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }

        GameWorldComponent gameComponent = GameWorldComponent.KEY.get(serverPlayer.getServerWorld());
        Role role = gameComponent.getRole(serverPlayer);
        if (!CriminologistRules.isDetective(role) && hasCriminologistRuntime()) {
            clearCriminologist();
        }

        tickCriminologist(serverPlayer, gameComponent, role);
    }

    private void tickCriminologist(ServerPlayerEntity serverPlayer, GameWorldComponent gameComponent, Role role) {
        if (!CriminologistRules.isDetective(role)) {
            return;
        }

        boolean shouldSync = false;
        if (criminologistCooldownTicks > 0) {
            criminologistCooldownTicks--;
            shouldSync |= criminologistCooldownTicks == 0 || criminologistCooldownTicks % 20 == 0;
        }

        if (criminologistTrackingTargetUuid != null) {
            if (gameComponent.isPlayerDead(criminologistTrackingTargetUuid)
                    || !targetIsPlayingAndAlive(serverPlayer.getServerWorld(), criminologistTrackingTargetUuid)) {
                startCriminologistCooldown();
                return;
            }
            if (criminologistRevealTicks > 0) {
                criminologistRevealTicks--;
                shouldSync |= criminologistRevealTicks == 0 || criminologistRevealTicks % 20 == 0;
            } else {
                criminologistRevealIntervalTicks--;
                if (criminologistRevealIntervalTicks <= 0) {
                    criminologistRevealTicks = CriminologistRules.REVEAL_TICKS;
                    criminologistRevealIntervalTicks = CriminologistRules.REVEAL_INTERVAL_TICKS;
                    shouldSync = true;
                }
            }
        }

        if (shouldSync) {
            sync();
        }
    }

    private static boolean targetIsPlayingAndAlive(ServerWorld world, UUID targetUuid) {
        ServerPlayerEntity target = world.getServer().getPlayerManager().getPlayer(targetUuid);
        return target != null && GameFunctions.isPlayerPlayingAndAlive(target);
    }

    private boolean hasCriminologistRuntime() {
        return criminologistCooldownTicks > 0
                || criminologistPendingVictimUuid != null
                || criminologistTrackingTargetUuid != null
                || criminologistRevealTicks > 0
                || criminologistRevealIntervalTicks > 0;
    }

    @Override
    public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient) {
        boolean ownerVisible = recipient == player || GameFunctions.isPlayerSpectatingOrCreative(recipient);
        buf.writeVarInt(ownerVisible ? criminologistCooldownTicks : 0);
        writeOptionalUuid(buf, ownerVisible ? criminologistPendingVictimUuid : null);
        writeOptionalUuid(buf, ownerVisible ? criminologistTrackingTargetUuid : null);
        buf.writeVarInt(ownerVisible ? criminologistRevealTicks : 0);
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        criminologistCooldownTicks = Math.max(0, buf.readVarInt());
        criminologistPendingVictimUuid = readOptionalUuid(buf);
        criminologistTrackingTargetUuid = readOptionalUuid(buf);
        criminologistRevealTicks = Math.max(0, buf.readVarInt());
        criminologistRevealIntervalTicks = 0;
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        if (criminologistCooldownTicks > 0) {
            tag.putInt("CriminologistCooldownTicks", criminologistCooldownTicks);
        }
        if (criminologistPendingVictimUuid != null) {
            tag.putUuid("CriminologistPendingVictim", criminologistPendingVictimUuid);
        }
        if (criminologistTrackingTargetUuid != null) {
            tag.putUuid("CriminologistTrackingTarget", criminologistTrackingTargetUuid);
        }
        if (criminologistRevealTicks > 0) {
            tag.putInt("CriminologistRevealTicks", criminologistRevealTicks);
        }
        if (criminologistRevealIntervalTicks > 0) {
            tag.putInt("CriminologistRevealIntervalTicks", criminologistRevealIntervalTicks);
        }
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        criminologistCooldownTicks = tag.contains("CriminologistCooldownTicks", NbtElement.NUMBER_TYPE)
                ? Math.max(0, tag.getInt("CriminologistCooldownTicks"))
                : 0;
        criminologistPendingVictimUuid = tag.containsUuid("CriminologistPendingVictim")
                ? tag.getUuid("CriminologistPendingVictim")
                : null;
        criminologistTrackingTargetUuid = tag.containsUuid("CriminologistTrackingTarget")
                ? tag.getUuid("CriminologistTrackingTarget")
                : null;
        criminologistRevealTicks = tag.contains("CriminologistRevealTicks", NbtElement.NUMBER_TYPE)
                ? Math.max(0, tag.getInt("CriminologistRevealTicks"))
                : 0;
        criminologistRevealIntervalTicks = tag.contains("CriminologistRevealIntervalTicks", NbtElement.NUMBER_TYPE)
                ? Math.max(0, tag.getInt("CriminologistRevealIntervalTicks"))
                : 0;
    }

    private static void writeOptionalUuid(RegistryByteBuf buf, @Nullable UUID uuid) {
        buf.writeBoolean(uuid != null);
        if (uuid != null) {
            buf.writeUuid(uuid);
        }
    }

    private static @Nullable UUID readOptionalUuid(RegistryByteBuf buf) {
        if (!buf.readBoolean()) {
            return null;
        }
        return buf.readUuid();
    }
}
