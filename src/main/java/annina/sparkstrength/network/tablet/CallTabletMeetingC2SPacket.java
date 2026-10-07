package annina.sparkstrength.network.tablet;

import annina.sparkstrength.SparkStrength;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record CallTabletMeetingC2SPacket() implements CustomPayload {
    public static final Identifier PAYLOAD_ID = SparkStrength.id("call_tablet_meeting");
    public static final Id<CallTabletMeetingC2SPacket> ID = new Id<>(PAYLOAD_ID);
    public static final PacketCodec<RegistryByteBuf, CallTabletMeetingC2SPacket> CODEC =
            PacketCodec.of(CallTabletMeetingC2SPacket::write, CallTabletMeetingC2SPacket::read);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public void write(PacketByteBuf buf) {
    }

    public static CallTabletMeetingC2SPacket read(PacketByteBuf buf) {
        return new CallTabletMeetingC2SPacket();
    }
}
