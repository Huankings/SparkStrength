package annina.sparkstrength.component.morphling;

import annina.sparkstrength.SparkStrength;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 保存本回合“尸体应显示成谁”的外观记录。
 *
 * <p>Wathe/Spark 版 PlayerBodyEntity 只稳定同步死亡玩家 UUID，不保存一份可改写的皮肤 UUID。
 * 所以试剂变形期间死亡时，不能直接改尸体实体本身；这里用世界组件按
 * “尸体 owner UUID -> 伪装目标 UUID/名字”记录一份旁路数据，客户端渲染尸体时再读取。</p>
 */
public final class MorphBodyDisguiseWorldComponent implements AutoSyncedComponent {
    public static final ComponentKey<MorphBodyDisguiseWorldComponent> KEY = ComponentRegistry.getOrCreate(
            SparkStrength.id("morph_body_disguise_world"),
            MorphBodyDisguiseWorldComponent.class
    );

    private final World world;
    private final LinkedHashMap<UUID, BodyDisguise> bodyDisguises = new LinkedHashMap<>();

    public MorphBodyDisguiseWorldComponent(World world) {
        this.world = world;
    }

    public void recordBodyDisguise(UUID bodyOwnerUuid, UUID disguiseUuid, String disguiseName) {
        if (bodyOwnerUuid == null || disguiseUuid == null) {
            return;
        }
        bodyDisguises.put(bodyOwnerUuid, new BodyDisguise(disguiseUuid, disguiseName == null ? "" : disguiseName));
        sync();
    }

    public Optional<BodyDisguise> getDisguise(UUID bodyOwnerUuid) {
        return Optional.ofNullable(bodyDisguises.get(bodyOwnerUuid));
    }

    public void clearRoundState() {
        if (bodyDisguises.isEmpty()) {
            return;
        }
        bodyDisguises.clear();
        sync();
    }

    public void sync() {
        if (world != null) {
            KEY.sync(world);
        }
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity player) {
        return true;
    }

    @Override
    public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient) {
        buf.writeVarInt(bodyDisguises.size());
        for (Map.Entry<UUID, BodyDisguise> entry : bodyDisguises.entrySet()) {
            buf.writeUuid(entry.getKey());
            buf.writeUuid(entry.getValue().disguiseUuid());
            buf.writeString(entry.getValue().disguiseName());
        }
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        bodyDisguises.clear();
        int size = buf.readVarInt();
        for (int i = 0; i < size; i++) {
            UUID owner = buf.readUuid();
            UUID disguise = buf.readUuid();
            String name = buf.readString();
            bodyDisguises.put(owner, new BodyDisguise(disguise, name));
        }
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        if (bodyDisguises.isEmpty()) {
            return;
        }
        NbtList records = new NbtList();
        for (Map.Entry<UUID, BodyDisguise> entry : bodyDisguises.entrySet()) {
            NbtCompound record = new NbtCompound();
            record.putUuid("BodyOwner", entry.getKey());
            record.putUuid("Disguise", entry.getValue().disguiseUuid());
            record.putString("DisguiseName", entry.getValue().disguiseName());
            records.add(record);
        }
        tag.put("BodyDisguises", records);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        bodyDisguises.clear();
        NbtList records = tag.getList("BodyDisguises", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < records.size(); i++) {
            NbtCompound record = records.getCompound(i);
            if (record.containsUuid("BodyOwner") && record.containsUuid("Disguise")) {
                bodyDisguises.put(
                        record.getUuid("BodyOwner"),
                        new BodyDisguise(record.getUuid("Disguise"), record.getString("DisguiseName"))
                );
            }
        }
    }

    public record BodyDisguise(UUID disguiseUuid, String disguiseName) {
    }
}
