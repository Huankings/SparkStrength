package annina.sparkstrength.network.tablet;

import annina.sparkstrength.SparkStrength;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

public record ApproveSuspectRemovalC2SPacket(UUID suspectUuid, boolean approved) implements CustomPayload {
    public static final Identifier PAYLOAD_ID = SparkStrength.id("approve_suspect_removal");
    public static final Id<ApproveSuspectRemovalC2SPacket> ID = new Id<>(PAYLOAD_ID);
    public static final PacketCodec<RegistryByteBuf, ApproveSuspectRemovalC2SPacket> CODEC =
            PacketCodec.of(ApproveSuspectRemovalC2SPacket::write, ApproveSuspectRemovalC2SPacket::read);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public void write(PacketByteBuf buf) {
        buf.writeUuid(suspectUuid);
        buf.writeBoolean(approved);
    }

    public static ApproveSuspectRemovalC2SPacket read(PacketByteBuf buf) {
        return new ApproveSuspectRemovalC2SPacket(buf.readUuid(), buf.readBoolean());
    }
}
