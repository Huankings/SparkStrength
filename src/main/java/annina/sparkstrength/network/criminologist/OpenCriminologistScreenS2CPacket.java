package annina.sparkstrength.network.criminologist;

import annina.sparkstrength.SparkStrength;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

public record OpenCriminologistScreenS2CPacket(UUID victimUuid) implements CustomPayload {
    public static final Identifier PAYLOAD_ID = SparkStrength.id("open_criminologist_screen");
    public static final Id<OpenCriminologistScreenS2CPacket> ID = new Id<>(PAYLOAD_ID);
    public static final PacketCodec<RegistryByteBuf, OpenCriminologistScreenS2CPacket> CODEC =
            PacketCodec.of(OpenCriminologistScreenS2CPacket::write, OpenCriminologistScreenS2CPacket::read);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public void write(PacketByteBuf buf) {
        buf.writeUuid(victimUuid);
    }

    public static OpenCriminologistScreenS2CPacket read(PacketByteBuf buf) {
        return new OpenCriminologistScreenS2CPacket(buf.readUuid());
    }
}
