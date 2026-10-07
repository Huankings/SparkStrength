package annina.sparkstrength.network.timekeeper;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import annina.sparkstrength.SparkStrength;

/** 客户端左键切换怀表模式时发送的模式序号。服务端会重新验证职业和手持物品。 */
public record TimekeeperWatchModeC2SPacket(int modeOrdinal) implements CustomPayload {
    public static final Identifier PAYLOAD_ID = SparkStrength.id("timekeeper_watch_mode");
    public static final Id<TimekeeperWatchModeC2SPacket> ID = new Id<>(PAYLOAD_ID);
    public static final PacketCodec<RegistryByteBuf, TimekeeperWatchModeC2SPacket> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT,
            TimekeeperWatchModeC2SPacket::modeOrdinal,
            TimekeeperWatchModeC2SPacket::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
