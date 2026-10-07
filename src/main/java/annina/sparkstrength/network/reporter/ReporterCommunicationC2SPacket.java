package annina.sparkstrength.network.reporter;

import annina.sparkstrength.SparkStrength;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Uuids;

import java.util.UUID;

/**
 * 记者背包通讯按钮的双目标选择包。
 *
 * <p>和自改版接线员一致：服务端根据两个 UUID 是否相同来决定是“接线”还是“广播采访”。</p>
 */
public record ReporterCommunicationC2SPacket(UUID firstPlayer, UUID secondPlayer) implements CustomPayload {
    public static final CustomPayload.Id<ReporterCommunicationC2SPacket> ID =
            new CustomPayload.Id<>(SparkStrength.id("reporter_communication"));
    public static final PacketCodec<RegistryByteBuf, ReporterCommunicationC2SPacket> CODEC = PacketCodec.tuple(
            Uuids.PACKET_CODEC, ReporterCommunicationC2SPacket::firstPlayer,
            Uuids.PACKET_CODEC, ReporterCommunicationC2SPacket::secondPlayer,
            ReporterCommunicationC2SPacket::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
