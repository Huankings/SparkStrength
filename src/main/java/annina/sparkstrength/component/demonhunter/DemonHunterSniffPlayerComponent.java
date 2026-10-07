package annina.sparkstrength.component.demonhunter;

import annina.sparkstrength.SparkStrength;
import annina.sparkstrength.role.demonhunter.DemonHunterSniffRules;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 保存猎魔人嗅探技能的玩家运行态。
 *
 * <p>状态分成两层：{@code sniffMarkedTargets} 是“本局已经被这个猎魔人嗅探到过”的长期标记；
 * {@code sniffRevealTicks} 是“这次二次嗅探后，还剩多少 tick 可以透视”的短期倒计时。
 * 长期标记只在服务端保存和持久化，不同步给客户端，避免客户端提前知道哪些玩家已经被标记。</p>
 */
public final class DemonHunterSniffPlayerComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<DemonHunterSniffPlayerComponent> KEY = ComponentRegistry.getOrCreate(
            SparkStrength.id("demon_hunter_sniff"),
            DemonHunterSniffPlayerComponent.class
    );

    private final PlayerEntity player;
    private int sniffCooldownTicks;
    private final Set<UUID> sniffMarkedTargets = new HashSet<>();
    private final Map<UUID, Integer> sniffRevealTicks = new HashMap<>();

    public DemonHunterSniffPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public int getSniffCooldownTicks() {
        return sniffCooldownTicks;
    }

    public boolean isSniffOnCooldown() {
        return sniffCooldownTicks > 0;
    }

    public boolean isSniffMarked(UUID targetUuid) {
        return sniffMarkedTargets.contains(targetUuid);
    }

    public boolean isSniffRevealing(UUID targetUuid) {
        return sniffRevealTicks.getOrDefault(targetUuid, 0) > 0;
    }

    public void initializeSniff() {
        sniffCooldownTicks = DemonHunterSniffRules.INITIAL_COOLDOWN_TICKS;
        sniffMarkedTargets.clear();
        sniffRevealTicks.clear();
        sync();
    }

    public void markSniffTarget(UUID targetUuid) {
        sniffMarkedTargets.add(targetUuid);
    }

    public void revealSniffTarget(UUID targetUuid) {
        sniffRevealTicks.put(targetUuid, DemonHunterSniffRules.REVEAL_TICKS);
    }

    public void startSniffCooldown() {
        sniffCooldownTicks = DemonHunterSniffRules.COOLDOWN_TICKS;
        sync();
    }

    /** 只刷新嗅探技能冷却，保留本局标记和当前透视目标。 */
    public void clearSniffCooldown() {
        if (sniffCooldownTicks == 0) {
            return;
        }
        sniffCooldownTicks = 0;
        sync();
    }

    public void clearSniff() {
        if (!hasSniffRuntime()) {
            return;
        }
        sniffCooldownTicks = 0;
        sniffMarkedTargets.clear();
        sniffRevealTicks.clear();
        sync();
    }

    public void sync() {
        KEY.sync(player);
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity recipient) {
        // 嗅探结果是猎魔人私有信息：只把冷却和当前透视倒计时同步给猎魔人本人。
        return recipient == player;
    }

    @Override
    public void serverTick() {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }

        GameWorldComponent game = GameWorldComponent.KEY.get(serverPlayer.getServerWorld());
        Role role = game.getRole(serverPlayer);
        if (!DemonHunterSniffRules.isDemonHunter(role)) {
            clearSniff();
            return;
        }

        boolean shouldSync = false;
        if (sniffCooldownTicks > 0) {
            sniffCooldownTicks--;
            shouldSync |= sniffCooldownTicks == 0 || sniffCooldownTicks % 20 == 0;
        }

        shouldSync |= tickRevealTargets(serverPlayer.getServerWorld());
        if (shouldSync) {
            sync();
        }
    }

    private boolean tickRevealTargets(ServerWorld world) {
        boolean changed = false;
        Iterator<Map.Entry<UUID, Integer>> iterator = sniffRevealTicks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Integer> entry = iterator.next();
            UUID targetUuid = entry.getKey();
            int remainingTicks = entry.getValue();

            // 透视只对仍在局内存活的目标有意义；目标死亡或离线时直接收掉短期透视状态。
            ServerPlayerEntity target = world.getServer().getPlayerManager().getPlayer(targetUuid);
            if (target == null || !GameFunctions.isPlayerPlayingAndAlive(target) || remainingTicks <= 1) {
                iterator.remove();
                changed = true;
                continue;
            }

            int nextTicks = remainingTicks - 1;
            entry.setValue(nextTicks);
            changed |= nextTicks % 20 == 0;
        }
        return changed;
    }

    private boolean hasSniffRuntime() {
        return sniffCooldownTicks > 0 || !sniffMarkedTargets.isEmpty() || !sniffRevealTicks.isEmpty();
    }

    @Override
    public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient) {
        buf.writeVarInt(sniffCooldownTicks);
        buf.writeVarInt(sniffRevealTicks.size());
        for (Map.Entry<UUID, Integer> entry : sniffRevealTicks.entrySet()) {
            buf.writeUuid(entry.getKey());
            buf.writeVarInt(entry.getValue());
        }
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        sniffCooldownTicks = Math.max(0, buf.readVarInt());
        sniffMarkedTargets.clear();
        sniffRevealTicks.clear();
        int revealCount = buf.readVarInt();
        for (int i = 0; i < revealCount; i++) {
            UUID targetUuid = buf.readUuid();
            int ticks = Math.max(0, buf.readVarInt());
            if (ticks > 0) {
                sniffRevealTicks.put(targetUuid, ticks);
            }
        }
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        if (sniffCooldownTicks > 0) {
            tag.putInt("SniffCooldownTicks", sniffCooldownTicks);
        }

        NbtList markedList = new NbtList();
        for (UUID uuid : sniffMarkedTargets) {
            markedList.add(NbtString.of(uuid.toString()));
        }
        if (!markedList.isEmpty()) {
            tag.put("SniffMarkedTargets", markedList);
        }

        NbtList revealList = new NbtList();
        for (Map.Entry<UUID, Integer> entry : sniffRevealTicks.entrySet()) {
            NbtCompound revealTag = new NbtCompound();
            revealTag.putUuid("Target", entry.getKey());
            revealTag.putInt("Ticks", entry.getValue());
            revealList.add(revealTag);
        }
        if (!revealList.isEmpty()) {
            tag.put("SniffRevealTargets", revealList);
        }
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        sniffCooldownTicks = tag.contains("SniffCooldownTicks", NbtElement.NUMBER_TYPE)
                ? Math.max(0, tag.getInt("SniffCooldownTicks"))
                : 0;

        sniffMarkedTargets.clear();
        if (tag.contains("SniffMarkedTargets", NbtElement.LIST_TYPE)) {
            NbtList markedList = tag.getList("SniffMarkedTargets", NbtElement.STRING_TYPE);
            for (int i = 0; i < markedList.size(); i++) {
                try {
                    sniffMarkedTargets.add(UUID.fromString(markedList.getString(i)));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        sniffRevealTicks.clear();
        if (tag.contains("SniffRevealTargets", NbtElement.LIST_TYPE)) {
            NbtList revealList = tag.getList("SniffRevealTargets", NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < revealList.size(); i++) {
                NbtCompound revealTag = revealList.getCompound(i);
                if (revealTag.containsUuid("Target")) {
                    int ticks = revealTag.contains("Ticks", NbtElement.NUMBER_TYPE)
                            ? Math.max(0, revealTag.getInt("Ticks"))
                            : 0;
                    if (ticks > 0) {
                        sniffRevealTicks.put(revealTag.getUuid("Target"), ticks);
                    }
                }
            }
        }
    }
}
