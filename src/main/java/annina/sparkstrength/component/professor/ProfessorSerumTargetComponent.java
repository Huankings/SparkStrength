package annina.sparkstrength.component.professor;

import annina.sparkstrength.SparkStrength;
import annina.sparkstrength.replay.SparkStrengthReplayFormatters;
import annina.sparkstrength.role.coroner.CoronerService;
import annina.sparkstrength.role.professor.ProfessorSerumRules;
import annina.sparkstrength.role.professor.ProfessorSerumType;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ClientTickingComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

/**
 * 被教授试剂影响的目标状态。
 *
 * <p>三个持续试剂都放在同一个组件中，是因为教授本能高亮需要处理“多个试剂叠加时的颜色优先级”。
 * 服务器负责自然结束事件；客户端只本地递减数值，用于教授透视和穿门碰撞的即时表现。</p>
 */
public class ProfessorSerumTargetComponent implements AutoSyncedComponent, ServerTickingComponent, ClientTickingComponent {
    public static final ComponentKey<ProfessorSerumTargetComponent> KEY = ComponentRegistry.getOrCreate(
            SparkStrength.id("professor_serum_target"),
            ProfessorSerumTargetComponent.class
    );

    private final PlayerEntity player;
    private int invisibilityTicks;
    private int doorpassingTicks;
    private int sedativeTicks;

    public ProfessorSerumTargetComponent(PlayerEntity player) {
        this.player = player;
    }

    public void apply(ProfessorSerumType type) {
        switch (type) {
            case INVISIBILITY -> invisibilityTicks = ProfessorSerumRules.INVISIBILITY_DURATION_TICKS;
            case DOORPASSING -> doorpassingTicks = ProfessorSerumRules.DOORPASSING_DURATION_TICKS;
            case SEDATIVE -> sedativeTicks = ProfessorSerumRules.SEDATIVE_DURATION_TICKS;
            case TRUTH -> {
                return;
            }
        }
        sync();
    }

    public boolean hasInvisibility() {
        return invisibilityTicks > 0;
    }

    public boolean hasDoorpassing() {
        return doorpassingTicks > 0;
    }

    public boolean hasSedative() {
        return sedativeTicks > 0;
    }

    public int highestPriorityHighlightColor() {
        // 用户已确认叠加优先级：隐身 > 穿门 > 镇静。
        if (hasInvisibility()) {
            return ProfessorSerumRules.INVISIBILITY_HIGHLIGHT_COLOR;
        }
        if (hasDoorpassing()) {
            return ProfessorSerumRules.DOORPASSING_HIGHLIGHT_COLOR;
        }
        if (hasSedative()) {
            return ProfessorSerumRules.SEDATIVE_HIGHLIGHT_COLOR;
        }
        return -1;
    }

    public void reset() {
        invisibilityTicks = 0;
        doorpassingTicks = 0;
        sedativeTicks = 0;
        sync();
    }

    private void sync() {
        KEY.sync(this.player);
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity recipient) {
        if (recipient == this.player) {
            // 目标本人需要知道穿门状态，客户端碰撞预测才不会和服务端互相拉扯。
            return true;
        }
        // 非教授也需要知道“目标处于教授隐身试剂中”，否则客户端本能事件无法精准返回 skip。
        // 具体同步哪些字段在 writeSyncPacket 中按接收者裁剪，避免把穿门/镇静状态无意义地广播给所有人。
        return GameFunctions.isPlayerPlayingAndAlive(recipient);
    }

    @Override
    public void serverTick() {
        if (invisibilityTicks <= 0 && doorpassingTicks <= 0 && sedativeTicks <= 0) {
            return;
        }

        if (!(player instanceof ServerPlayerEntity serverPlayer)
                || !GameFunctions.isPlayerPlayingAndAlive(serverPlayer)) {
            // 非自然结束不写回放：死亡、重置、离局等情况只清掉残留状态。
            reset();
            return;
        }

        boolean changed = false;
        changed |= tickOne(serverPlayer, ProfessorSerumType.INVISIBILITY);
        changed |= tickOne(serverPlayer, ProfessorSerumType.DOORPASSING);
        changed |= tickOne(serverPlayer, ProfessorSerumType.SEDATIVE);

        if (changed
                || (invisibilityTicks > 0 && invisibilityTicks % 20 == 0)
                || (doorpassingTicks > 0 && doorpassingTicks % 20 == 0)
                || (sedativeTicks > 0 && sedativeTicks % 20 == 0)) {
            sync();
        }
    }

    @Override
    public void clientTick() {
        if (invisibilityTicks > 0) {
            invisibilityTicks--;
        }
        if (doorpassingTicks > 0) {
            doorpassingTicks--;
        }
        if (sedativeTicks > 0) {
            sedativeTicks--;
        }
    }

    private boolean tickOne(ServerPlayerEntity serverPlayer, ProfessorSerumType type) {
        int before = getTicks(type);
        if (before <= 0) {
            return false;
        }

        setTicks(type, before - 1);
        if (before - 1 == 0) {
            recordNaturalEnd(serverPlayer, type);
            return true;
        }
        return false;
    }

    private int getTicks(ProfessorSerumType type) {
        return switch (type) {
            case INVISIBILITY -> invisibilityTicks;
            case DOORPASSING -> doorpassingTicks;
            case SEDATIVE -> sedativeTicks;
            case TRUTH -> 0;
        };
    }

    private void setTicks(ProfessorSerumType type, int ticks) {
        int value = Math.max(0, ticks);
        switch (type) {
            case INVISIBILITY -> invisibilityTicks = value;
            case DOORPASSING -> doorpassingTicks = value;
            case SEDATIVE -> sedativeTicks = value;
            case TRUTH -> {
            }
        }
    }

    private static void recordNaturalEnd(ServerPlayerEntity target, ProfessorSerumType type) {
        if (!(target.getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }
        switch (type) {
            case INVISIBILITY -> GameRecordManager.recordGlobalEvent(
                    serverWorld,
                    SparkStrengthReplayFormatters.PROFESSOR_INVISIBILITY_ENDED,
                    target,
                    null
            );
            case DOORPASSING -> GameRecordManager.recordGlobalEvent(
                    serverWorld,
                    SparkStrengthReplayFormatters.PROFESSOR_DOORPASSING_ENDED,
                    target,
                    null
            );
            case SEDATIVE -> GameRecordManager.recordGlobalEvent(
                    serverWorld,
                    SparkStrengthReplayFormatters.PROFESSOR_SEDATIVE_ENDED,
                    target,
                    null
            );
            case TRUTH -> {
            }
        }
    }

    @Override
    public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(recipient.getWorld());
        boolean fullSync = recipient == this.player
                || gameWorld.isRole(recipient, Noellesroles.PROFESSOR)
                || CoronerService.hasProfessorDisguise(recipient);
        buf.writeVarInt(invisibilityTicks);
        buf.writeVarInt(fullSync ? doorpassingTicks : 0);
        buf.writeVarInt(fullSync ? sedativeTicks : 0);
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        invisibilityTicks = buf.readVarInt();
        doorpassingTicks = buf.readVarInt();
        sedativeTicks = buf.readVarInt();
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putInt("InvisibilityTicks", invisibilityTicks);
        tag.putInt("DoorpassingTicks", doorpassingTicks);
        tag.putInt("SedativeTicks", sedativeTicks);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        invisibilityTicks = tag.getInt("InvisibilityTicks");
        doorpassingTicks = tag.getInt("DoorpassingTicks");
        sedativeTicks = tag.getInt("SedativeTicks");
    }
}
