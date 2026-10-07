package annina.sparkstrength.component.detective;

import annina.sparkstrength.SparkStrength;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Stores world-level criminologist evidence gathered during the current round.
 * 保存当前回合内犯罪学家需要查询的世界级击杀线索。
 *
 * <p>组件只记录“尸体 UUID -> 真凶 UUID”。这些数据不需要同步给客户端，
 * 客户端只会通过专门的犯罪学家网络包看到自己有资格查看的选择界面。</p>
 */
public final class CriminologistWorldComponent implements AutoSyncedComponent {
    public static final ComponentKey<CriminologistWorldComponent> KEY = ComponentRegistry.getOrCreate(
            SparkStrength.id("criminologist_world"),
            CriminologistWorldComponent.class
    );

    private final World world;
    private final LinkedHashMap<UUID, UUID> criminologistKillers = new LinkedHashMap<>();

    public CriminologistWorldComponent(World world) {
        this.world = world;
    }

    public void recordCriminologistKill(UUID victimUuid, UUID killerUuid) {
        if (victimUuid == null || killerUuid == null || victimUuid.equals(killerUuid)) {
            return;
        }
        criminologistKillers.put(victimUuid, killerUuid);
        sync();
    }

    public Optional<UUID> getCriminologistKiller(UUID victimUuid) {
        return Optional.ofNullable(criminologistKillers.get(victimUuid));
    }

    public void clearRoundState() {
        if (criminologistKillers.isEmpty()) {
            return;
        }
        criminologistKillers.clear();
        sync();
    }

    public void sync() {
        if (world != null) {
            KEY.sync(world);
        }
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity player) {
        return false;
    }

    @Override
    public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient) {
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        if (criminologistKillers.isEmpty()) {
            return;
        }
        NbtList records = new NbtList();
        for (Map.Entry<UUID, UUID> entry : criminologistKillers.entrySet()) {
            NbtCompound record = new NbtCompound();
            record.putUuid("Victim", entry.getKey());
            record.putUuid("Killer", entry.getValue());
            records.add(record);
        }
        tag.put("CriminologistKillers", records);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        criminologistKillers.clear();
        NbtList records = tag.getList("CriminologistKillers", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < records.size(); i++) {
            NbtCompound record = records.getCompound(i);
            if (record.containsUuid("Victim") && record.containsUuid("Killer")) {
                criminologistKillers.put(record.getUuid("Victim"), record.getUuid("Killer"));
            }
        }
    }
}
