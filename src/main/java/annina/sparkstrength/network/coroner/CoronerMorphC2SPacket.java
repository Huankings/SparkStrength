package annina.sparkstrength.network.coroner;

import annina.sparkstrength.SparkStrength;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Uuids;

import java.util.UUID;

/**
 * 客户端点击验尸官背包头像后发给服务端的变形请求。
 *
 * <p>客户端只提交目标 UUID；是否已用采尸袋解锁、是否仍是存活验尸官、
 * 以及这次变形需要发什么临时装备，都必须由服务端重新判断。</p>
 */
public record CoronerMorphC2SPacket(UUID targetPlayer) implements CustomPayload {
    public static final CustomPayload.Id<CoronerMorphC2SPacket> ID =
            new CustomPayload.Id<>(SparkStrength.id("coroner_morph"));
    public static final PacketCodec<RegistryByteBuf, CoronerMorphC2SPacket> CODEC = PacketCodec.tuple(
            Uuids.PACKET_CODEC, CoronerMorphC2SPacket::targetPlayer,
            CoronerMorphC2SPacket::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
