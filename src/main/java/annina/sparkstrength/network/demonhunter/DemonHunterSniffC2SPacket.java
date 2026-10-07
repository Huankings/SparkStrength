package annina.sparkstrength.network.demonhunter;

import annina.sparkstrength.SparkStrength;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * 客户端按下 NoellesRoles 能力键后发给服务端的猎魔人嗅探请求。
 *
 * <p>包里没有字段，因为嗅探范围、目标列表和冷却都必须由服务端根据当前世界状态计算。</p>
 */
public record DemonHunterSniffC2SPacket() implements CustomPayload {
    public static final Identifier PAYLOAD_ID = SparkStrength.id("demon_hunter_sniff");
    public static final Id<DemonHunterSniffC2SPacket> ID = new Id<>(PAYLOAD_ID);
    public static final PacketCodec<RegistryByteBuf, DemonHunterSniffC2SPacket> CODEC =
            PacketCodec.of(DemonHunterSniffC2SPacket::write, DemonHunterSniffC2SPacket::read);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public void write(PacketByteBuf buf) {
    }

    public static DemonHunterSniffC2SPacket read(PacketByteBuf buf) {
        return new DemonHunterSniffC2SPacket();
    }
}
