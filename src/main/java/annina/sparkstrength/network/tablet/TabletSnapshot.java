package annina.sparkstrength.network.tablet;

import net.minecraft.network.PacketByteBuf;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record TabletSnapshot(
        boolean localHasTablet,
        boolean localMeetingParticipant,
        int cooldownSeconds,
        int localMeetingCallsRemaining,
        List<PlayerRow> connections,
        List<ChatRow> chat,
        Meeting meeting,
        List<SuspectRow> suspects
) {
    public static TabletSnapshot empty() {
        return new TabletSnapshot(false, false, 0, 0, List.of(), List.of(), Meeting.inactive(), List.of());
    }

    public void write(PacketByteBuf buf) {
        buf.writeBoolean(localHasTablet);
        buf.writeBoolean(localMeetingParticipant);
        buf.writeVarInt(cooldownSeconds);
        buf.writeVarInt(localMeetingCallsRemaining);
        writeList(buf, connections, (targetBuf, row) -> row.write(targetBuf));
        writeList(buf, chat, (targetBuf, row) -> row.write(targetBuf));
        meeting.write(buf);
        writeList(buf, suspects, (targetBuf, row) -> row.write(targetBuf));
    }

    public static TabletSnapshot read(PacketByteBuf buf) {
        return new TabletSnapshot(
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readVarInt(),
                buf.readVarInt(),
                readList(buf, PlayerRow::read),
                readList(buf, ChatRow::read),
                Meeting.read(buf),
                readList(buf, SuspectRow::read)
        );
    }

    private static <T> void writeList(PacketByteBuf buf, List<T> list, Writer<T> writer) {
        buf.writeVarInt(list.size());
        for (T value : list) {
            writer.write(buf, value);
        }
    }

    private static <T> List<T> readList(PacketByteBuf buf, Reader<T> reader) {
        int size = buf.readVarInt();
        ArrayList<T> values = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            values.add(reader.read(buf));
        }
        return List.copyOf(values);
    }

    private static void writeOptionalUuid(PacketByteBuf buf, @Nullable UUID uuid) {
        buf.writeBoolean(uuid != null);
        if (uuid != null) {
            buf.writeUuid(uuid);
        }
    }

    private static @Nullable UUID readOptionalUuid(PacketByteBuf buf) {
        return buf.readBoolean() ? buf.readUuid() : null;
    }

    private interface Writer<T> {
        void write(PacketByteBuf buf, T value);
    }

    private interface Reader<T> {
        T read(PacketByteBuf buf);
    }

    public record PlayerRow(UUID uuid, String name, boolean inGame) {
        private void write(PacketByteBuf buf) {
            buf.writeUuid(uuid);
            buf.writeString(name);
            buf.writeBoolean(inGame);
        }

        private static PlayerRow read(PacketByteBuf buf) {
            return new PlayerRow(buf.readUuid(), buf.readString(32767), buf.readBoolean());
        }
    }

    public record ChatRow(UUID senderUuid, String senderName, String message, long timeMillis) {
        private void write(PacketByteBuf buf) {
            buf.writeUuid(senderUuid);
            buf.writeString(senderName);
            buf.writeString(message);
            buf.writeLong(timeMillis);
        }

        private static ChatRow read(PacketByteBuf buf) {
            return new ChatRow(buf.readUuid(), buf.readString(32767), buf.readString(32767), buf.readLong());
        }
    }

    public record Meeting(
            boolean active,
            int remainingSeconds,
            boolean localConfirmed,
            boolean localAbstained,
            @Nullable UUID localVoteTarget,
            List<VoteTarget> targets
    ) {
        public static Meeting inactive() {
            return new Meeting(false, 0, false, false, null, List.of());
        }

        private void write(PacketByteBuf buf) {
            buf.writeBoolean(active);
            buf.writeVarInt(remainingSeconds);
            buf.writeBoolean(localConfirmed);
            buf.writeBoolean(localAbstained);
            writeOptionalUuid(buf, localVoteTarget);
            writeList(buf, targets, (targetBuf, row) -> row.write(targetBuf));
        }

        private static Meeting read(PacketByteBuf buf) {
            return new Meeting(
                    buf.readBoolean(),
                    buf.readVarInt(),
                    buf.readBoolean(),
                    buf.readBoolean(),
                    readOptionalUuid(buf),
                    readList(buf, VoteTarget::read)
            );
        }
    }

    public record VoteTarget(UUID uuid, String name, boolean selectable, int votes) {
        private void write(PacketByteBuf buf) {
            buf.writeUuid(uuid);
            buf.writeString(name);
            buf.writeBoolean(selectable);
            buf.writeVarInt(votes);
        }

        private static VoteTarget read(PacketByteBuf buf) {
            return new VoteTarget(buf.readUuid(), buf.readString(32767), buf.readBoolean(), buf.readVarInt());
        }
    }

    public record SuspectRow(UUID uuid, String name, int approvals, int requiredApprovals, boolean localApproved) {
        private void write(PacketByteBuf buf) {
            buf.writeUuid(uuid);
            buf.writeString(name);
            buf.writeVarInt(approvals);
            buf.writeVarInt(requiredApprovals);
            buf.writeBoolean(localApproved);
        }

        private static SuspectRow read(PacketByteBuf buf) {
            return new SuspectRow(
                    buf.readUuid(),
                    buf.readString(32767),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readBoolean()
            );
        }
    }
}
