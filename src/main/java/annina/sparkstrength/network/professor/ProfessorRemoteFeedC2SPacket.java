package annina.sparkstrength.network.professor;

import annina.sparkstrength.SparkStrength;
import annina.sparkstrength.role.professor.ProfessorSerumType;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Uuids;

import java.util.UUID;

/**
 * 教授背包二级界面点击试剂后发给服务端的请求。
 *
 * <p>客户端只提交目标 UUID 和试剂类型；服务端会重新检查教授职业、存活状态、
 * 背包内是否真的有试剂、目标是否存活以及远程投喂冷却。</p>
 */
public record ProfessorRemoteFeedC2SPacket(UUID targetPlayer, ProfessorSerumType serumType) implements CustomPayload {
    public static final CustomPayload.Id<ProfessorRemoteFeedC2SPacket> ID =
            new CustomPayload.Id<>(SparkStrength.id("professor_remote_feed"));

    public static final PacketCodec<RegistryByteBuf, ProfessorRemoteFeedC2SPacket> CODEC = new PacketCodec<>() {
        @Override
        public ProfessorRemoteFeedC2SPacket decode(RegistryByteBuf buf) {
            UUID targetPlayer = Uuids.PACKET_CODEC.decode(buf);
            ProfessorSerumType serumType = buf.readEnumConstant(ProfessorSerumType.class);
            return new ProfessorRemoteFeedC2SPacket(targetPlayer, serumType);
        }

        @Override
        public void encode(RegistryByteBuf buf, ProfessorRemoteFeedC2SPacket value) {
            Uuids.PACKET_CODEC.encode(buf, value.targetPlayer);
            buf.writeEnumConstant(value.serumType);
        }
    };

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
