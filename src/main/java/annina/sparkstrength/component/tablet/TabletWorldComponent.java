package annina.sparkstrength.component.tablet;

import annina.sparkstrength.SparkStrength;
import annina.sparkstrength.tablet.TabletRules;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Server-owned tablet network state; clients receive filtered snapshots through custom packets.
 * 服务端持有的平板网络状态；客户端通过专用网络包接收过滤后的快照。
 */
public final class TabletWorldComponent implements AutoSyncedComponent {
    public static final ComponentKey<TabletWorldComponent> KEY = ComponentRegistry.getOrCreate(
            SparkStrength.id("tablet_world"),
            TabletWorldComponent.class
    );

    private final World world;
    private final ArrayList<ChatMessage> chatHistory = new ArrayList<>();
    private final LinkedHashSet<UUID> suspects = new LinkedHashSet<>();
    private final LinkedHashMap<UUID, LinkedHashSet<UUID>> removalApprovals = new LinkedHashMap<>();
    private final LinkedHashMap<UUID, Integer> meetingCalls = new LinkedHashMap<>();
    private @Nullable Meeting meeting;
    private long meetingCooldownEndTick;
    private int emergencyMeetingChances = TabletRules.DEFAULT_EMERGENCY_MEETING_CHANCES;
    private int meetingDurationTicks = TabletRules.DEFAULT_MEETING_DURATION_TICKS;

    public TabletWorldComponent(World world) {
        this.world = world;
    }

    public List<ChatMessage> chatHistory() {
        return Collections.unmodifiableList(chatHistory);
    }

    public void addChatMessage(UUID senderUuid, String senderName, String message, long timeMillis) {
        chatHistory.add(new ChatMessage(senderUuid, senderName, message, timeMillis));
    }

    public Set<UUID> suspects() {
        return Collections.unmodifiableSet(suspects);
    }

    public boolean isSuspect(UUID uuid) {
        return suspects.contains(uuid);
    }

    public boolean addSuspect(UUID uuid) {
        if (uuid == null) {
            return false;
        }
        return suspects.add(uuid);
    }

    public boolean removeSuspect(UUID uuid) {
        boolean removed = suspects.remove(uuid);
        removalApprovals.remove(uuid);
        return removed;
    }

    public Set<UUID> removalApprovals(UUID suspectUuid) {
        return Collections.unmodifiableSet(removalApprovals.getOrDefault(suspectUuid, new LinkedHashSet<>()));
    }

    public void setRemovalApproval(UUID suspectUuid, UUID voterUuid, boolean approved) {
        if (!suspects.contains(suspectUuid)) {
            return;
        }
        LinkedHashSet<UUID> approvals = removalApprovals.computeIfAbsent(suspectUuid, ignored -> new LinkedHashSet<>());
        if (approved) {
            approvals.add(voterUuid);
        } else {
            approvals.remove(voterUuid);
        }
        if (approvals.isEmpty()) {
            removalApprovals.remove(suspectUuid);
        }
    }

    public void pruneRemovalApprovals(Set<UUID> electorate) {
        removalApprovals.values().forEach(approvals -> approvals.removeIf(voter -> !electorate.contains(voter)));
        removalApprovals.entrySet().removeIf(entry -> entry.getValue().isEmpty() || !suspects.contains(entry.getKey()));
    }

    public boolean hasActiveMeeting() {
        return meeting != null;
    }

    public @Nullable Meeting meeting() {
        return meeting;
    }

    public int emergencyMeetingChances() {
        return emergencyMeetingChances;
    }

    public void setEmergencyMeetingChances(int chances) {
        emergencyMeetingChances = Math.max(0, chances);
    }

    public int meetingDurationTicks() {
        return meetingDurationTicks;
    }

    public void setMeetingDurationTicks(int ticks) {
        meetingDurationTicks = Math.max(1, ticks);
    }

    public int usedMeetingCalls(UUID playerUuid) {
        return meetingCalls.getOrDefault(playerUuid, 0);
    }

    public int remainingMeetingCalls(UUID playerUuid) {
        return Math.max(0, emergencyMeetingChances - usedMeetingCalls(playerUuid));
    }

    public boolean canCallMeeting(UUID playerUuid) {
        return remainingMeetingCalls(playerUuid) > 0;
    }

    public void recordMeetingCall(UUID playerUuid) {
        meetingCalls.merge(playerUuid, 1, Integer::sum);
    }

    public void startMeeting() {
        meeting = new Meeting(meetingDurationTicks);
    }

    public void clearMeeting() {
        meeting = null;
    }

    public void startMeetingCooldown(long currentTick) {
        meetingCooldownEndTick = currentTick + TabletRules.MEETING_COOLDOWN_TICKS;
    }

    public int meetingCooldownTicks(long currentTick) {
        return (int) Math.max(0, meetingCooldownEndTick - currentTick);
    }

    public void clearRoundState() {
        chatHistory.clear();
        suspects.clear();
        removalApprovals.clear();
        meetingCalls.clear();
        meeting = null;
        meetingCooldownEndTick = 0;
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity player) {
        return false;
    }

    @Override
    public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient) {
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        if (!chatHistory.isEmpty()) {
            NbtList messages = new NbtList();
            for (ChatMessage message : chatHistory) {
                NbtCompound messageTag = new NbtCompound();
                messageTag.putUuid("Sender", message.senderUuid());
                messageTag.putString("SenderName", message.senderName());
                messageTag.putString("Message", message.message());
                messageTag.putLong("Time", message.timeMillis());
                messages.add(messageTag);
            }
            tag.put("ChatHistory", messages);
        }

        if (!suspects.isEmpty()) {
            NbtList suspectTags = new NbtList();
            for (UUID suspect : suspects) {
                NbtCompound suspectTag = new NbtCompound();
                suspectTag.putUuid("Uuid", suspect);
                suspectTags.add(suspectTag);
            }
            tag.put("Suspects", suspectTags);
        }

        if (!removalApprovals.isEmpty()) {
            NbtList approvalsTags = new NbtList();
            for (Map.Entry<UUID, LinkedHashSet<UUID>> entry : removalApprovals.entrySet()) {
                NbtCompound approvalTag = new NbtCompound();
                approvalTag.putUuid("Suspect", entry.getKey());
                NbtList voters = new NbtList();
                for (UUID voter : entry.getValue()) {
                    NbtCompound voterTag = new NbtCompound();
                    voterTag.putUuid("Uuid", voter);
                    voters.add(voterTag);
                }
                approvalTag.put("Voters", voters);
                approvalsTags.add(approvalTag);
            }
            tag.put("RemovalApprovals", approvalsTags);
        }

        if (meeting != null) {
            tag.put("Meeting", meeting.toNbt());
        }
        if (meetingCooldownEndTick > 0) {
            tag.putLong("MeetingCooldownEndTick", meetingCooldownEndTick);
        }
        tag.putInt("EmergencyMeetingChances", emergencyMeetingChances);
        tag.putInt("MeetingDurationTicks", meetingDurationTicks);

        if (!meetingCalls.isEmpty()) {
            NbtList callTags = new NbtList();
            for (Map.Entry<UUID, Integer> entry : meetingCalls.entrySet()) {
                NbtCompound callTag = new NbtCompound();
                callTag.putUuid("Uuid", entry.getKey());
                callTag.putInt("Count", entry.getValue());
                callTags.add(callTag);
            }
            tag.put("MeetingCalls", callTags);
        }
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        chatHistory.clear();
        NbtList messages = tag.getList("ChatHistory", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < messages.size(); i++) {
            NbtCompound messageTag = messages.getCompound(i);
            if (messageTag.containsUuid("Sender")) {
                chatHistory.add(new ChatMessage(
                        messageTag.getUuid("Sender"),
                        messageTag.getString("SenderName"),
                        messageTag.getString("Message"),
                        messageTag.getLong("Time")
                ));
            }
        }

        suspects.clear();
        NbtList suspectTags = tag.getList("Suspects", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < suspectTags.size(); i++) {
            NbtCompound suspectTag = suspectTags.getCompound(i);
            if (suspectTag.containsUuid("Uuid")) {
                suspects.add(suspectTag.getUuid("Uuid"));
            }
        }

        removalApprovals.clear();
        NbtList approvalsTags = tag.getList("RemovalApprovals", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < approvalsTags.size(); i++) {
            NbtCompound approvalTag = approvalsTags.getCompound(i);
            if (!approvalTag.containsUuid("Suspect")) {
                continue;
            }
            LinkedHashSet<UUID> voters = new LinkedHashSet<>();
            NbtList voterTags = approvalTag.getList("Voters", NbtElement.COMPOUND_TYPE);
            for (int voterIndex = 0; voterIndex < voterTags.size(); voterIndex++) {
                NbtCompound voterTag = voterTags.getCompound(voterIndex);
                if (voterTag.containsUuid("Uuid")) {
                    voters.add(voterTag.getUuid("Uuid"));
                }
            }
            if (!voters.isEmpty()) {
                removalApprovals.put(approvalTag.getUuid("Suspect"), voters);
            }
        }

        meeting = tag.contains("Meeting", NbtElement.COMPOUND_TYPE)
                ? Meeting.fromNbt(tag.getCompound("Meeting"))
                : null;
        meetingCooldownEndTick = tag.contains("MeetingCooldownEndTick", NbtElement.NUMBER_TYPE)
                ? tag.getLong("MeetingCooldownEndTick")
                : 0;
        emergencyMeetingChances = tag.contains("EmergencyMeetingChances", NbtElement.NUMBER_TYPE)
                ? Math.max(0, tag.getInt("EmergencyMeetingChances"))
                : TabletRules.DEFAULT_EMERGENCY_MEETING_CHANCES;
        meetingDurationTicks = tag.contains("MeetingDurationTicks", NbtElement.NUMBER_TYPE)
                ? Math.max(1, tag.getInt("MeetingDurationTicks"))
                : TabletRules.DEFAULT_MEETING_DURATION_TICKS;

        meetingCalls.clear();
        NbtList callTags = tag.getList("MeetingCalls", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < callTags.size(); i++) {
            NbtCompound callTag = callTags.getCompound(i);
            if (callTag.containsUuid("Uuid")) {
                meetingCalls.put(callTag.getUuid("Uuid"), Math.max(0, callTag.getInt("Count")));
            }
        }
    }

    public record ChatMessage(UUID senderUuid, String senderName, String message, long timeMillis) {
    }

    public static final class Meeting {
        private int ticksRemaining;
        private final LinkedHashMap<UUID, UUID> votes = new LinkedHashMap<>();
        private final LinkedHashSet<UUID> abstentions = new LinkedHashSet<>();
        private final LinkedHashSet<UUID> confirmed = new LinkedHashSet<>();

        private Meeting(int ticksRemaining) {
            this.ticksRemaining = Math.max(0, ticksRemaining);
        }

        public int ticksRemaining() {
            return ticksRemaining;
        }

        public void tick() {
            if (ticksRemaining > 0) {
                ticksRemaining--;
            }
        }

        public boolean isExpired() {
            return ticksRemaining <= 0;
        }

        public Map<UUID, UUID> votes() {
            return Collections.unmodifiableMap(votes);
        }

        public Set<UUID> abstentions() {
            return Collections.unmodifiableSet(abstentions);
        }

        public Set<UUID> confirmed() {
            return Collections.unmodifiableSet(confirmed);
        }

        public boolean isConfirmed(UUID voterUuid) {
            return confirmed.contains(voterUuid);
        }

        public boolean isAbstaining(UUID voterUuid) {
            return abstentions.contains(voterUuid);
        }

        public Optional<UUID> voteFor(UUID voterUuid) {
            return Optional.ofNullable(votes.get(voterUuid));
        }

        public void castVote(UUID voterUuid, @Nullable UUID targetUuid) {
            if (confirmed.contains(voterUuid)) {
                return;
            }
            votes.remove(voterUuid);
            abstentions.remove(voterUuid);
            if (targetUuid == null) {
                abstentions.add(voterUuid);
            } else {
                votes.put(voterUuid, targetUuid);
            }
        }

        public void confirm(UUID voterUuid) {
            confirmed.add(voterUuid);
        }

        public boolean allConfirmed(Set<UUID> currentParticipants) {
            return !currentParticipants.isEmpty() && confirmed.containsAll(currentParticipants);
        }

        public void pruneVotes(Set<UUID> currentParticipants) {
            votes.entrySet().removeIf(entry -> !currentParticipants.contains(entry.getKey()));
            abstentions.removeIf(voter -> !currentParticipants.contains(voter));
            confirmed.removeIf(voter -> !currentParticipants.contains(voter));
        }

        private NbtCompound toNbt() {
            NbtCompound tag = new NbtCompound();
            tag.putInt("TicksRemaining", ticksRemaining);

            NbtList voteTags = new NbtList();
            for (Map.Entry<UUID, UUID> entry : votes.entrySet()) {
                NbtCompound voteTag = new NbtCompound();
                voteTag.putUuid("Voter", entry.getKey());
                voteTag.putUuid("Target", entry.getValue());
                voteTags.add(voteTag);
            }
            tag.put("Votes", voteTags);

            tag.put("Abstentions", uuidList(abstentions));
            tag.put("Confirmed", uuidList(confirmed));
            return tag;
        }

        private static Meeting fromNbt(NbtCompound tag) {
            Meeting meeting = new Meeting(tag.getInt("TicksRemaining"));

            NbtList voteTags = tag.getList("Votes", NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < voteTags.size(); i++) {
                NbtCompound voteTag = voteTags.getCompound(i);
                if (voteTag.containsUuid("Voter") && voteTag.containsUuid("Target")) {
                    meeting.votes.put(voteTag.getUuid("Voter"), voteTag.getUuid("Target"));
                }
            }

            readUuidList(tag.getList("Abstentions", NbtElement.COMPOUND_TYPE), meeting.abstentions);
            readUuidList(tag.getList("Confirmed", NbtElement.COMPOUND_TYPE), meeting.confirmed);
            return meeting;
        }

        private static NbtList uuidList(Set<UUID> uuids) {
            NbtList list = new NbtList();
            for (UUID uuid : uuids) {
                NbtCompound tag = new NbtCompound();
                tag.putUuid("Uuid", uuid);
                list.add(tag);
            }
            return list;
        }

        private static void readUuidList(NbtList tags, Set<UUID> uuids) {
            for (int i = 0; i < tags.size(); i++) {
                NbtCompound tag = tags.getCompound(i);
                if (tag.containsUuid("Uuid")) {
                    uuids.add(tag.getUuid("Uuid"));
                }
            }
        }
    }
}
