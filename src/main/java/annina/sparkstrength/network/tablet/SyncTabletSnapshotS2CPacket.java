package annina.sparkstrength.network.tablet;

import annina.sparkstrength.SparkStrength;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record SyncTabletSnapshotS2CPacket(TabletSnapshot snapshot) implements CustomPayload {
    public static final Identifier PAYLOAD_ID = SparkStrength.id("sync_tablet_snapshot");
    public static final Id<SyncTabletSnapshotS2CPacket> ID = new Id<>(PAYLOAD_ID);
    public static final PacketCodec<RegistryByteBuf, SyncTabletSnapshotS2CPacket> CODEC =
            PacketCodec.of(SyncTabletSnapshotS2CPacket::write, SyncTabletSnapshotS2CPacket::read);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public void write(PacketByteBuf buf) {
        snapshot.write(buf);
    }

    public static SyncTabletSnapshotS2CPacket read(PacketByteBuf buf) {
        return new SyncTabletSnapshotS2CPacket(TabletSnapshot.read(buf));
    }
}
