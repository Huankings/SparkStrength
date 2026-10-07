package annina.sparkstrength.network.veteran;

import annina.sparkstrength.SparkStrength;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * 同步“当前世界是否处于停电”的轻量包。
 *
 * <p>Wathe 的停电状态保存在服务端 WorldBlackoutComponent 中，客户端本能高亮事件不能可靠直接读取。
 * 所以这里只同步一个 boolean，客户端老兵高亮用它判断是否启用。</p>
 */
public record SyncVeteranBlackoutS2CPacket(boolean active) implements CustomPayload {
    public static final Identifier PAYLOAD_ID = SparkStrength.id("sync_veteran_blackout");
    public static final Id<SyncVeteranBlackoutS2CPacket> ID = new Id<>(PAYLOAD_ID);
    public static final PacketCodec<RegistryByteBuf, SyncVeteranBlackoutS2CPacket> CODEC =
            PacketCodec.of(SyncVeteranBlackoutS2CPacket::write, SyncVeteranBlackoutS2CPacket::read);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public void write(PacketByteBuf buf) {
        buf.writeBoolean(active);
    }

    public static SyncVeteranBlackoutS2CPacket read(PacketByteBuf buf) {
        return new SyncVeteranBlackoutS2CPacket(buf.readBoolean());
    }
}
