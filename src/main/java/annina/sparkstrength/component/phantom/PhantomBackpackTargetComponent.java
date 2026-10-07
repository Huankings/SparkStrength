package annina.sparkstrength.component.phantom;

import annina.sparkstrength.SparkStrength;
import annina.sparkstrength.replay.SparkStrengthReplayFormatters;
import annina.sparkstrength.role.phantom.PhantomBackpackRules;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

/**
 * 被幽灵背包按钮隐身的目标状态。
 *
 * <p>真正的隐身来自原版 {@link StatusEffects#INVISIBILITY}；
 * 这个组件只负责追踪“这次隐身是幽灵背包造成的”，并在倒计时结束时写入回放。
 * 如果目标死亡、离开局内或回合重置，则静默清理，不补“自然结束”回放。</p>
 */
public class PhantomBackpackTargetComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<PhantomBackpackTargetComponent> KEY = ComponentRegistry.getOrCreate(
            SparkStrength.id("phantom_backpack_target"),
            PhantomBackpackTargetComponent.class
    );

    private final PlayerEntity player;
    private int invisibilityTicksRemaining;

    public PhantomBackpackTargetComponent(PlayerEntity player) {
        this.player = player;
    }

    public void startInvisibility() {
        invisibilityTicksRemaining = PhantomBackpackRules.INVISIBILITY_DURATION_TICKS;
        sync();
    }

    public void reset() {
        invisibilityTicksRemaining = 0;
        sync();
    }

    private void sync() {
        KEY.sync(player);
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity recipient) {
        // 目前目标组件只为服务端回放计时；同步给本人便于后续调试或扩展 HUD。
        return recipient == player;
    }

    @Override
    public void serverTick() {
        if (invisibilityTicksRemaining <= 0) {
            return;
        }

        if (!(player instanceof ServerPlayerEntity serverPlayer)
                || !GameFunctions.isPlayerPlayingAndAlive(serverPlayer)
                || !GameFunctions.isPlayerAliveAndSurvival(serverPlayer)) {
            reset();
            return;
        }

        /*
         * 如果隐身效果被其它逻辑提前移除，也立即补“隐身时间结束”回放。
         * 这样回放描述的是可见状态变化，而不是单纯等固定 30 秒。
         */
        if (!serverPlayer.hasStatusEffect(StatusEffects.INVISIBILITY)) {
            finishInvisibility(serverPlayer);
            return;
        }

        invisibilityTicksRemaining--;
        if (invisibilityTicksRemaining > 0) {
            if (invisibilityTicksRemaining % 20 == 0) {
                sync();
            }
            return;
        }

        finishInvisibility(serverPlayer);
    }

    private void finishInvisibility(ServerPlayerEntity target) {
        invisibilityTicksRemaining = 0;
        sync();
        if (target.getWorld() instanceof ServerWorld serverWorld) {
            GameRecordManager.recordGlobalEvent(
                    serverWorld,
                    SparkStrengthReplayFormatters.PHANTOM_BACKPACK_INVISIBILITY_ENDED,
                    target,
                    null
            );
        }
    }

    @Override
    public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient) {
        buf.writeVarInt(invisibilityTicksRemaining);
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        invisibilityTicksRemaining = buf.readVarInt();
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putInt("InvisibilityTicksRemaining", invisibilityTicksRemaining);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        invisibilityTicksRemaining = Math.max(0, tag.getInt("InvisibilityTicksRemaining"));
    }
}
