package annina.sparkstrength.component.coroner;

import annina.sparkstrength.SparkStrength;
import annina.sparkstrength.role.coroner.CoronerRules;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 保存验尸官本命内的收尸、变形和靠近尸体领奖状态。
 *
 * <p>组件注册时使用 NEVER_COPY，并且死亡/重置都会清空；这正是用户确认的规则：
 * 采尸袋解锁记录不跨命保留。这里仍然把状态写入 NBT，是为了服务端同一局运行中保存/重载时不丢状态，
 * 但不会跟随重生复制。</p>
 */
public final class CoronerPlayerComponent implements AutoSyncedComponent {
    public static final ComponentKey<CoronerPlayerComponent> KEY = ComponentRegistry.getOrCreate(
            SparkStrength.id("coroner_player"),
            CoronerPlayerComponent.class
    );

    private final PlayerEntity player;
    private final LinkedHashMap<UUID, BodySnapshot> unlockedBodies = new LinkedHashMap<>();
    /*
     * 靠近尸体的 50 金币奖励按“尸体实体 UUID”去重，而不是按尸体主人 UUID 去重。
     * 这样同一名玩家如果因为特殊流程留下多具尸体，验尸官靠近每一具尸体都能各领一次钱。
     */
    private final Set<UUID> rewardedBodies = new LinkedHashSet<>();
    private @Nullable UUID activeDisguiseUuid;
    private @Nullable Identifier activeDisguiseRoleId;
    /*
     * 当 SparkStrength/Noelles 的变形怪效果覆盖验尸官外观时，验尸官尸体身份的装备也会被临时收回。
     * 这个标记用于在高优先级变形结束后，只补发一次原伪装身份对应的临时装备。
     */
    private boolean temporaryEquipmentSuppressed;

    public CoronerPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public void initializeForRole() {
        // 开局只拥有“自己头像”：自己不写入 unlockedBodies，UI 会固定把自己放在第一格。
        clearAll();
    }

    public void clearAll() {
        unlockedBodies.clear();
        rewardedBodies.clear();
        activeDisguiseUuid = null;
        activeDisguiseRoleId = null;
        temporaryEquipmentSuppressed = false;
        sync();
    }

    public void clearDisguise() {
        if (activeDisguiseUuid == null && activeDisguiseRoleId == null) {
            return;
        }
        activeDisguiseUuid = null;
        activeDisguiseRoleId = null;
        temporaryEquipmentSuppressed = false;
        sync();
    }

    public boolean unlockBody(UUID bodyOwnerUuid, BodySnapshot snapshot) {
        if (bodyOwnerUuid == null || bodyOwnerUuid.equals(player.getUuid()) || snapshot == null) {
            return false;
        }
        BodySnapshot previous = unlockedBodies.put(bodyOwnerUuid, snapshot);
        sync();
        return previous == null || !previous.equals(snapshot);
    }

    public boolean knowsDisguise(UUID uuid) {
        return uuid != null && (uuid.equals(player.getUuid()) || unlockedBodies.containsKey(uuid));
    }

    public Set<UUID> unlockedBodyUuids() {
        return unlockedBodies.keySet();
    }

    public @Nullable BodySnapshot snapshot(UUID bodyOwnerUuid) {
        return unlockedBodies.get(bodyOwnerUuid);
    }

    public @Nullable UUID activeDisguiseUuid() {
        return activeDisguiseUuid;
    }

    public @Nullable Identifier activeDisguiseRoleId() {
        return activeDisguiseRoleId;
    }

    public boolean hasActiveDisguise() {
        return activeDisguiseUuid != null;
    }

    public void setActiveDisguise(UUID bodyOwnerUuid, BodySnapshot snapshot) {
        this.activeDisguiseUuid = bodyOwnerUuid;
        this.activeDisguiseRoleId = snapshot == null ? CoronerRules.WATHE_NO_ROLE_ID : snapshot.roleId();
        this.temporaryEquipmentSuppressed = false;
        sync();
    }

    public boolean isTemporaryEquipmentSuppressed() {
        return temporaryEquipmentSuppressed;
    }

    public void setTemporaryEquipmentSuppressed(boolean temporaryEquipmentSuppressed) {
        this.temporaryEquipmentSuppressed = temporaryEquipmentSuppressed;
    }

    public boolean hasRewardedBody(UUID bodyEntityUuid) {
        return rewardedBodies.contains(bodyEntityUuid);
    }

    public void markRewardedBody(UUID bodyEntityUuid) {
        rewardedBodies.add(bodyEntityUuid);
    }

    public void sync() {
        if (player != null) {
            KEY.sync(player);
        }
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity recipient) {
        // 外观变形需要所有客户端知道；完整解锁清单只在 writeSyncPacket 中按接收者裁剪。
        return recipient != null;
    }

    @Override
    public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient) {
        writeOptionalUuid(buf, activeDisguiseUuid);
        writeOptionalIdentifier(buf, activeDisguiseRoleId);

        boolean fullVisible = recipient == player || GameFunctions.isPlayerSpectatingOrCreative(recipient);
        buf.writeBoolean(fullVisible);
        if (!fullVisible) {
            return;
        }

        buf.writeVarInt(unlockedBodies.size());
        for (Map.Entry<UUID, BodySnapshot> entry : unlockedBodies.entrySet()) {
            buf.writeUuid(entry.getKey());
            buf.writeIdentifier(entry.getValue().roleId());
        }
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        activeDisguiseUuid = readOptionalUuid(buf);
        activeDisguiseRoleId = readOptionalIdentifier(buf);

        unlockedBodies.clear();
        boolean fullVisible = buf.readBoolean();
        if (fullVisible) {
            int size = buf.readVarInt();
            for (int i = 0; i < size; i++) {
                unlockedBodies.put(buf.readUuid(), new BodySnapshot(buf.readIdentifier()));
            }
        } else if (activeDisguiseUuid != null && activeDisguiseRoleId != null) {
            // 非本人客户端只需要当前变形的身份快照，用于外观和杀手同伙提示。
            unlockedBodies.put(activeDisguiseUuid, new BodySnapshot(activeDisguiseRoleId));
        }
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        if (activeDisguiseUuid != null) {
            tag.putUuid("ActiveDisguise", activeDisguiseUuid);
        }
        if (activeDisguiseRoleId != null) {
            tag.putString("ActiveDisguiseRole", activeDisguiseRoleId.toString());
        }
        tag.putBoolean("TemporaryEquipmentSuppressed", temporaryEquipmentSuppressed);

        NbtList unlocked = new NbtList();
        for (Map.Entry<UUID, BodySnapshot> entry : unlockedBodies.entrySet()) {
            NbtCompound body = new NbtCompound();
            body.putUuid("BodyOwner", entry.getKey());
            body.putString("RoleId", entry.getValue().roleId().toString());
            unlocked.add(body);
        }
        tag.put("UnlockedBodies", unlocked);

        NbtList rewarded = new NbtList();
        for (UUID bodyEntityUuid : rewardedBodies) {
            NbtCompound body = new NbtCompound();
            body.putUuid("BodyEntity", bodyEntityUuid);
            rewarded.add(body);
        }
        tag.put("RewardedBodies", rewarded);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        activeDisguiseUuid = tag.containsUuid("ActiveDisguise") ? tag.getUuid("ActiveDisguise") : null;
        activeDisguiseRoleId = tag.contains("ActiveDisguiseRole")
                ? Identifier.of(tag.getString("ActiveDisguiseRole"))
                : null;
        temporaryEquipmentSuppressed = tag.getBoolean("TemporaryEquipmentSuppressed");

        unlockedBodies.clear();
        NbtList unlocked = tag.getList("UnlockedBodies", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < unlocked.size(); i++) {
            NbtCompound body = unlocked.getCompound(i);
            if (body.containsUuid("BodyOwner") && body.contains("RoleId")) {
                unlockedBodies.put(
                        body.getUuid("BodyOwner"),
                        new BodySnapshot(Identifier.of(body.getString("RoleId")))
                );
            }
        }

        rewardedBodies.clear();
        NbtList rewarded = tag.getList("RewardedBodies", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < rewarded.size(); i++) {
            NbtCompound body = rewarded.getCompound(i);
            if (body.containsUuid("BodyEntity")) {
                rewardedBodies.add(body.getUuid("BodyEntity"));
            } else if (body.containsUuid("BodyOwner")) {
                // 兼容早期开发版曾经按尸体主人 UUID 记录的存档字段。
                rewardedBodies.add(body.getUuid("BodyOwner"));
            }
        }
    }

    private static void writeOptionalUuid(RegistryByteBuf buf, @Nullable UUID uuid) {
        buf.writeBoolean(uuid != null);
        if (uuid != null) {
            buf.writeUuid(uuid);
        }
    }

    private static @Nullable UUID readOptionalUuid(RegistryByteBuf buf) {
        return buf.readBoolean() ? buf.readUuid() : null;
    }

    private static void writeOptionalIdentifier(RegistryByteBuf buf, @Nullable Identifier identifier) {
        buf.writeBoolean(identifier != null);
        if (identifier != null) {
            buf.writeIdentifier(identifier);
        }
    }

    private static @Nullable Identifier readOptionalIdentifier(RegistryByteBuf buf) {
        return buf.readBoolean() ? buf.readIdentifier() : null;
    }

    public record BodySnapshot(Identifier roleId) {
    }
}
