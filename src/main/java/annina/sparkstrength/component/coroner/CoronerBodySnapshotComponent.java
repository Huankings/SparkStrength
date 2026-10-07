package annina.sparkstrength.component.coroner;

import annina.sparkstrength.SparkStrength;
import annina.sparkstrength.role.coroner.CoronerRules;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

/**
 * 挂在玩家尸体上的“死亡时身份快照”。
 *
 * <p>验尸官后续拿采尸袋收尸时，只读取这个快照，不再读取在线玩家当前身份。
 * 这样即使某些职业在死亡后发生转身份、换身份或复活逻辑，验尸官获得的仍然是尸体展示出来的身份。</p>
 */
public final class CoronerBodySnapshotComponent implements AutoSyncedComponent {
    public static final ComponentKey<CoronerBodySnapshotComponent> KEY = ComponentRegistry.getOrCreate(
            SparkStrength.id("coroner_body_snapshot"),
            CoronerBodySnapshotComponent.class
    );

    private final PlayerBodyEntity body;
    private Identifier roleId = CoronerRules.WATHE_NO_ROLE_ID;

    public CoronerBodySnapshotComponent(PlayerBodyEntity body) {
        this.body = body;
    }

    public Identifier roleId() {
        return roleId;
    }

    public boolean hasSnapshot() {
        return !CoronerRules.WATHE_NO_ROLE_ID.equals(roleId);
    }

    public void setRoleId(Identifier roleId) {
        this.roleId = roleId == null ? CoronerRules.WATHE_NO_ROLE_ID : roleId;
        sync();
    }

    public void sync() {
        if (body != null) {
            KEY.sync(body);
        }
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity recipient) {
        return true;
    }

    @Override
    public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient) {
        buf.writeIdentifier(roleId);
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        roleId = buf.readIdentifier();
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putString("RoleId", roleId.toString());
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        roleId = tag.contains("RoleId")
                ? Identifier.of(tag.getString("RoleId"))
                : CoronerRules.WATHE_NO_ROLE_ID;
    }
}
