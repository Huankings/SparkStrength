package annina.sparkstrength.network.tablet;

import annina.sparkstrength.SparkStrength;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record RequestTabletSnapshotC2SPacket() implements CustomPayload {
    public static final Identifier PAYLOAD_ID = SparkStrength.id("request_tablet_snapshot");
    public static final Id<RequestTabletSnapshotC2SPacket> ID = new Id<>(PAYLOAD_ID);
    public static final PacketCodec<RegistryByteBuf, RequestTabletSnapshotC2SPacket> CODEC =
            PacketCodec.of(RequestTabletSnapshotC2SPacket::write, RequestTabletSnapshotC2SPacket::read);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public void write(PacketByteBuf buf) {
    }

    public static RequestTabletSnapshotC2SPacket read(PacketByteBuf buf) {
        return new RequestTabletSnapshotC2SPacket();
    }
}
