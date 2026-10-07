package annina.sparkstrength.network.tablet;

import annina.sparkstrength.SparkStrength;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record OpenTabletScreenS2CPacket() implements CustomPayload {
    public static final Identifier PAYLOAD_ID = SparkStrength.id("open_tablet_screen");
    public static final Id<OpenTabletScreenS2CPacket> ID = new Id<>(PAYLOAD_ID);
    public static final PacketCodec<RegistryByteBuf, OpenTabletScreenS2CPacket> CODEC =
            PacketCodec.of(OpenTabletScreenS2CPacket::write, OpenTabletScreenS2CPacket::read);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public void write(PacketByteBuf buf) {
    }

    public static OpenTabletScreenS2CPacket read(PacketByteBuf buf) {
        return new OpenTabletScreenS2CPacket();
    }
}
