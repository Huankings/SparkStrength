package annina.sparkstrength.component.noisemaker;

import annina.sparkstrength.SparkStrength;
import annina.sparkstrength.role.noisemaker.NoisemakerGlowConstants;
import annina.sparkstrength.replay.SparkStrengthReplayFormatters;
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
 * 被大嗓门“点亮”的目标状态。
 *
 * <p>真正让玩家发光的是原版 {@link StatusEffects#GLOWING} 状态效果；
 * 这个组件只负责保存“这是一次大嗓门主动点亮”以及倒计时结束时写回放。
 * 这样死亡反光给杀手的 15 秒发光可以只加状态效果，不会误写回放事件。</p>
 */
public class NoisemakerGlowTargetComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<NoisemakerGlowTargetComponent> KEY = ComponentRegistry.getOrCreate(
            SparkStrength.id("noisemaker_glow_target"),
            NoisemakerGlowTargetComponent.class
    );

    private final PlayerEntity player;
    private int glowTicksRemaining;

    public NoisemakerGlowTargetComponent(PlayerEntity player) {
        this.player = player;
    }

    /**
     * 记录一次由背包头像按钮触发的发光。
     *
     * <p>这里不保存点亮者 UUID，因为结束回放文案只需要目标：
     * “%s 的发光时间结束”。如果同一个目标被多次刷新发光，结束事件也只在最后一次倒计时结束时写一次。</p>
     */
    public void startGlow() {
        this.glowTicksRemaining = NoisemakerGlowConstants.GLOW_DURATION_TICKS;
        this.sync();
    }

    public void reset() {
        this.glowTicksRemaining = 0;
        this.sync();
    }

    public int getGlowTicksRemaining() {
        return glowTicksRemaining;
    }

    public void sync() {
        KEY.sync(this.player);
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity recipient) {
        // 目前客户端不需要读取目标组件，但保留同步给本人能方便后续调试或 HUD 扩展。
        return recipient == this.player;
    }

    @Override
    public void serverTick() {
        if (glowTicksRemaining <= 0) {
            return;
        }

        // 目标死亡、退出局内或变为非存活状态时直接清理，不补写结束回放。
        // 回放只描述一次完整的“存活目标被点亮并自然结束”的过程。
        if (!(player instanceof ServerPlayerEntity serverPlayer) || !GameFunctions.isPlayerPlayingAndAlive(serverPlayer)) {
            reset();
            return;
        }

        glowTicksRemaining--;
        if (glowTicksRemaining > 0) {
            if (glowTicksRemaining % 20 == 0) {
                sync();
            }
            return;
        }

        sync();
        if (serverPlayer.getWorld() instanceof ServerWorld serverWorld) {
            GameRecordManager.recordGlobalEvent(
                    serverWorld,
                    SparkStrengthReplayFormatters.NOISEMAKER_GLOW_ENDED,
                    serverPlayer,
                    null
            );
        }
    }

    @Override
    public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient) {
        buf.writeVarInt(this.glowTicksRemaining);
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        this.glowTicksRemaining = buf.readVarInt();
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putInt("GlowTicksRemaining", this.glowTicksRemaining);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.glowTicksRemaining = tag.getInt("GlowTicksRemaining");
    }
}
