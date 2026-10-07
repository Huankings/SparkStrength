package annina.sparkstrength.network.tablet;

import annina.sparkstrength.SparkStrength;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record SendTabletChatC2SPacket(String message) implements CustomPayload {
    public static final Identifier PAYLOAD_ID = SparkStrength.id("send_tablet_chat");
    public static final Id<SendTabletChatC2SPacket> ID = new Id<>(PAYLOAD_ID);
    public static final PacketCodec<RegistryByteBuf, SendTabletChatC2SPacket> CODEC =
            PacketCodec.of(SendTabletChatC2SPacket::write, SendTabletChatC2SPacket::read);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public void write(PacketByteBuf buf) {
        buf.writeString(message);
    }

    public static SendTabletChatC2SPacket read(PacketByteBuf buf) {
        return new SendTabletChatC2SPacket(buf.readString(32767));
    }
}
