package annina.sparkstrength.network.phantom;

import annina.sparkstrength.SparkStrength;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Uuids;

import java.util.UUID;

/**
 * 客户端点击幽灵背包头像后发给服务器的请求。
 *
 * <p>客户端只提交目标 UUID；职业、冷却、目标存活和饕餮吞噬状态都由服务端重新判定，
 * 防止改包绕过冷却或隐身不合法目标。</p>
 */
public record PhantomBackpackInvisibilityC2SPacket(UUID targetPlayer) implements CustomPayload {
    public static final CustomPayload.Id<PhantomBackpackInvisibilityC2SPacket> ID =
            new CustomPayload.Id<>(SparkStrength.id("phantom_backpack_invisibility"));
    public static final PacketCodec<RegistryByteBuf, PhantomBackpackInvisibilityC2SPacket> CODEC = PacketCodec.tuple(
            Uuids.PACKET_CODEC, PhantomBackpackInvisibilityC2SPacket::targetPlayer,
            PhantomBackpackInvisibilityC2SPacket::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
