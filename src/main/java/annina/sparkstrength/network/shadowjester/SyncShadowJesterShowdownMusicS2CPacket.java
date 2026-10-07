package annina.sparkstrength.network.shadowjester;

import annina.sparkstrength.SparkStrength;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * 同步新版双影谢幕环境音的开始/结束状态。
 */
public record SyncShadowJesterShowdownMusicS2CPacket(boolean active) implements CustomPayload {
    public static final Identifier PAYLOAD_ID = SparkStrength.id("sync_shadow_jester_showdown_music");
    public static final Id<SyncShadowJesterShowdownMusicS2CPacket> ID = new Id<>(PAYLOAD_ID);
    public static final PacketCodec<RegistryByteBuf, SyncShadowJesterShowdownMusicS2CPacket> CODEC =
            PacketCodec.of(SyncShadowJesterShowdownMusicS2CPacket::write, SyncShadowJesterShowdownMusicS2CPacket::read);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public void write(PacketByteBuf buf) {
        buf.writeBoolean(active);
    }

    public static SyncShadowJesterShowdownMusicS2CPacket read(PacketByteBuf buf) {
        return new SyncShadowJesterShowdownMusicS2CPacket(buf.readBoolean());
    }
}
