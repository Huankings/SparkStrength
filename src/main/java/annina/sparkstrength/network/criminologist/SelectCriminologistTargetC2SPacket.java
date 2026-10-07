package annina.sparkstrength.network.criminologist;

import annina.sparkstrength.SparkStrength;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

public record SelectCriminologistTargetC2SPacket(UUID victimUuid, UUID suspectUuid) implements CustomPayload {
    public static final Identifier PAYLOAD_ID = SparkStrength.id("select_criminologist_target");
    public static final Id<SelectCriminologistTargetC2SPacket> ID = new Id<>(PAYLOAD_ID);
    public static final PacketCodec<RegistryByteBuf, SelectCriminologistTargetC2SPacket> CODEC =
            PacketCodec.of(SelectCriminologistTargetC2SPacket::write, SelectCriminologistTargetC2SPacket::read);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public void write(PacketByteBuf buf) {
        buf.writeUuid(victimUuid);
        buf.writeUuid(suspectUuid);
    }

    public static SelectCriminologistTargetC2SPacket read(PacketByteBuf buf) {
        return new SelectCriminologistTargetC2SPacket(buf.readUuid(), buf.readUuid());
    }
}
