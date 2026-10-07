package annina.sparkstrength.network.tablet;

import annina.sparkstrength.SparkStrength;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record CastTabletVoteC2SPacket(@Nullable UUID targetUuid) implements CustomPayload {
    public static final Identifier PAYLOAD_ID = SparkStrength.id("cast_tablet_vote");
    public static final Id<CastTabletVoteC2SPacket> ID = new Id<>(PAYLOAD_ID);
    public static final PacketCodec<RegistryByteBuf, CastTabletVoteC2SPacket> CODEC =
            PacketCodec.of(CastTabletVoteC2SPacket::write, CastTabletVoteC2SPacket::read);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public void write(PacketByteBuf buf) {
        buf.writeBoolean(targetUuid != null);
        if (targetUuid != null) {
            buf.writeUuid(targetUuid);
        }
    }

    public static CastTabletVoteC2SPacket read(PacketByteBuf buf) {
        return new CastTabletVoteC2SPacket(buf.readBoolean() ? buf.readUuid() : null);
    }
}
