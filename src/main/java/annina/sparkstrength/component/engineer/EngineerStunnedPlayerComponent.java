package annina.sparkstrength.component.engineer;

import annina.sparkstrength.SparkStrength;
import annina.sparkstrength.replay.SparkStrengthReplayFormatters;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

/**
 * 捕捉装置造成的“定身”状态。
 *
 * <p>客户端 mixin 会根据这个组件禁用键鼠输入；服务端这里每 tick 清速度并把玩家拉回锁定点。
 * 两边同时做，是为了防止单靠客户端输入拦截被网络移动包或延迟边缘情况绕过。</p>
 */
public final class EngineerStunnedPlayerComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<EngineerStunnedPlayerComponent> KEY = ComponentRegistry.getOrCreate(
            SparkStrength.id("engineer_stunned"),
            EngineerStunnedPlayerComponent.class
    );

    private final PlayerEntity player;
    private int stunTicks;
    private double lockedX;
    private double lockedY;
    private double lockedZ;

    public EngineerStunnedPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public void stun(int ticks) {
        this.stunTicks = Math.max(this.stunTicks, ticks);
        Vec3d pos = player.getPos();
        this.lockedX = pos.x;
        this.lockedY = pos.y;
        this.lockedZ = pos.z;
        sync();
    }

    public boolean isStunned() {
        return stunTicks > 0;
    }

    public void clear() {
        boolean wasStunned = isStunned();
        this.stunTicks = 0;
        player.setVelocity(Vec3d.ZERO);
        if (wasStunned) {
            sync();
        }
    }

    @Override
    public void serverTick() {
        if (stunTicks <= 0) {
            return;
        }

        if (!GameFunctions.isPlayerPlayingAndAlive(player) || player.isSpectator() || player.isCreative()) {
            clear();
            return;
        }

        // Server authority keeps clearing velocity and restoring the lock point despite late movement packets.
        // 服务端是最终权威：即使客户端还有输入或移动包到达，也持续清速度并把位置拉回捕捉点。
        player.setVelocity(Vec3d.ZERO);
        player.velocityModified = true;
        player.fallDistance = 0.0F;
        if (player.squaredDistanceTo(lockedX, lockedY, lockedZ) > 0.0025D) {
            player.requestTeleport(lockedX, lockedY, lockedZ);
        }

        stunTicks--;
        if (stunTicks <= 0) {
            stunTicks = 0;
            if (player instanceof ServerPlayerEntity serverPlayer) {
                GameRecordManager.recordGlobalEvent(
                        serverPlayer.getServerWorld(),
                        SparkStrengthReplayFormatters.CAPTURE_DEVICE_RELEASED,
                        serverPlayer,
                        null
                );
            }
        }
        sync();
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putInt("StunTicks", stunTicks);
        tag.putDouble("LockedX", lockedX);
        tag.putDouble("LockedY", lockedY);
        tag.putDouble("LockedZ", lockedZ);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        stunTicks = tag.getInt("StunTicks");
        lockedX = tag.getDouble("LockedX");
        lockedY = tag.getDouble("LockedY");
        lockedZ = tag.getDouble("LockedZ");
    }

    private void sync() {
        KEY.sync(player);
    }
}
