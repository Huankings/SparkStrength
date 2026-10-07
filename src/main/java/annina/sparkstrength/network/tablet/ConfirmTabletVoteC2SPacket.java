package annina.sparkstrength.network.tablet;

import annina.sparkstrength.SparkStrength;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ConfirmTabletVoteC2SPacket() implements CustomPayload {
    public static final Identifier PAYLOAD_ID = SparkStrength.id("confirm_tablet_vote");
    public static final Id<ConfirmTabletVoteC2SPacket> ID = new Id<>(PAYLOAD_ID);
    public static final PacketCodec<RegistryByteBuf, ConfirmTabletVoteC2SPacket> CODEC =
            PacketCodec.of(ConfirmTabletVoteC2SPacket::write, ConfirmTabletVoteC2SPacket::read);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public void write(PacketByteBuf buf) {
    }

    public static ConfirmTabletVoteC2SPacket read(PacketByteBuf buf) {
        return new ConfirmTabletVoteC2SPacket();
    }
}
