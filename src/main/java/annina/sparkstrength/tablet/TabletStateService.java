package annina.sparkstrength.tablet;

import annina.sparkstrength.component.tablet.TabletWorldComponent;
import annina.sparkstrength.network.tablet.OpenTabletScreenS2CPacket;
import annina.sparkstrength.network.tablet.SyncTabletSnapshotS2CPacket;
import annina.sparkstrength.network.tablet.TabletSnapshot;
import com.mojang.authlib.GameProfile;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Authoritative server actions for the tablet network.
 * 平板网络的服务端权威操作入口。
 */
public final class TabletStateService {
    private TabletStateService() {
    }

    public static void openTablet(ServerPlayerEntity player) {
        if (!TabletAccess.hasTabletInHotbar(player)) {
            player.sendMessage(Text.translatable("message.sparkstrength.tablet.no_tablet"), true);
            return;
        }
        ServerPlayNetworking.send(player, new OpenTabletScreenS2CPacket());
        syncTo(player);
    }

    public static void syncTo(ServerPlayerEntity player) {
        if (!TabletAccess.hasTabletInHotbar(player)) {
            return;
        }
        ServerPlayNetworking.send(player, new SyncTabletSnapshotS2CPacket(buildSnapshot(player)));
    }

    public static void syncToTabletHolders(ServerWorld world) {
        for (ServerPlayerEntity player : TabletAccess.tabletHolders(world)) {
            syncTo(player);
        }
    }

    public static void sendChat(ServerPlayerEntity sender, String rawMessage) {
        if (!TabletAccess.hasTabletInHotbar(sender)) {
            sender.sendMessage(Text.translatable("message.sparkstrength.tablet.no_tablet"), true);
            return;
        }
        String message = rawMessage == null ? "" : rawMessage.trim();
        if (message.isEmpty()) {
            sender.sendMessage(Text.translatable("message.sparkstrength.tablet.chat_empty"), true);
            return;
        }
        if (message.length() > TabletRules.CHAT_MESSAGE_MAX_LENGTH) {
            message = message.substring(0, TabletRules.CHAT_MESSAGE_MAX_LENGTH);
        }
        TabletWorldComponent.KEY.get(sender.getServerWorld()).addChatMessage(
                sender.getUuid(),
                sender.getName().getString(),
                message,
                Instant.now().toEpochMilli()
        );
        syncToTabletHolders(sender.getServerWorld());
    }

    public static void callMeeting(ServerPlayerEntity caller) {
        ServerWorld world = caller.getServerWorld();
        TabletWorldComponent tablet = TabletWorldComponent.KEY.get(world);
        if (!TabletAccess.isAliveTabletParticipant(caller)) {
            caller.sendMessage(Text.translatable("message.sparkstrength.tablet.meeting_denied"), true);
            return;
        }
        if (tablet.hasActiveMeeting()) {
            caller.sendMessage(Text.translatable("message.sparkstrength.tablet.meeting_active"), true);
            return;
        }
        int cooldownTicks = tablet.meetingCooldownTicks(world.getTime());
        if (cooldownTicks > 0) {
            caller.sendMessage(Text.translatable(
                    "message.sparkstrength.tablet.meeting_cooldown",
                    TabletRules.secondsCeil(cooldownTicks)
            ), true);
            return;
        }
        if (!tablet.canCallMeeting(caller.getUuid())) {
            caller.sendMessage(Text.translatable("message.sparkstrength.tablet.meeting_chances_empty"), true);
            return;
        }

        tablet.recordMeetingCall(caller.getUuid());
        tablet.startMeeting();
        broadcastToTabletHolders(world, Text.translatable("message.sparkstrength.tablet.meeting_started"));
        syncToTabletHolders(world);
    }

    public static void castVote(ServerPlayerEntity voter, @Nullable UUID targetUuid) {
        ServerWorld world = voter.getServerWorld();
        TabletWorldComponent tablet = TabletWorldComponent.KEY.get(world);
        TabletWorldComponent.Meeting meeting = tablet.meeting();
        if (!TabletAccess.isAliveTabletParticipant(voter) || meeting == null) {
            voter.sendMessage(Text.translatable("message.sparkstrength.tablet.meeting_denied"), true);
            return;
        }
        if (meeting.isConfirmed(voter.getUuid())) {
            voter.sendMessage(Text.translatable("message.sparkstrength.tablet.vote_locked"), true);
            return;
        }
        if (targetUuid != null && !isSelectableMeetingTarget(world, tablet, targetUuid)) {
            return;
        }

        meeting.castVote(voter.getUuid(), targetUuid);
        voter.sendMessage(Text.translatable("message.sparkstrength.tablet.vote_recorded"), true);
        syncToTabletHolders(world);
    }

    public static void confirmVote(ServerPlayerEntity voter) {
        ServerWorld world = voter.getServerWorld();
        TabletWorldComponent tablet = TabletWorldComponent.KEY.get(world);
        TabletWorldComponent.Meeting meeting = tablet.meeting();
        if (!TabletAccess.isAliveTabletParticipant(voter) || meeting == null) {
            voter.sendMessage(Text.translatable("message.sparkstrength.tablet.meeting_denied"), true);
            return;
        }

        meeting.confirm(voter.getUuid());
        voter.sendMessage(Text.translatable("message.sparkstrength.tablet.vote_confirmed"), true);
        if (meeting.allConfirmed(currentParticipantUuids(world))) {
            finishMeeting(world);
        } else {
            syncToTabletHolders(world);
        }
    }

    public static void setSuspectRemovalApproval(ServerPlayerEntity voter, UUID suspectUuid, boolean approved) {
        ServerWorld world = voter.getServerWorld();
        TabletWorldComponent tablet = TabletWorldComponent.KEY.get(world);
        if (!TabletAccess.isAliveTabletParticipant(voter)) {
            voter.sendMessage(Text.translatable("message.sparkstrength.tablet.meeting_denied"), true);
            return;
        }
        if (!tablet.isSuspect(suspectUuid)) {
            syncTo(voter);
            return;
        }

        tablet.setRemovalApproval(suspectUuid, voter.getUuid(), approved);
        Set<UUID> electorate = currentParticipantUuids(world);
        tablet.pruneRemovalApprovals(electorate);
        int approvals = tablet.removalApprovals(suspectUuid).size();
        if (TabletRules.meetsTwoThirds(approvals, electorate.size())) {
            String suspectName = playerName(world, suspectUuid);
            tablet.removeSuspect(suspectUuid);
            broadcastToTabletHolders(world, Text.translatable("message.sparkstrength.tablet.suspect_removed", suspectName));
        }
        syncToTabletHolders(world);
    }

    public static void tick(ServerWorld world) {
        if (world.getTime() % 20 == 0) {
            syncToTabletHolders(world);
        }

        TabletWorldComponent tablet = TabletWorldComponent.KEY.get(world);
        TabletWorldComponent.Meeting meeting = tablet.meeting();
        if (meeting == null) {
            return;
        }

        Set<UUID> participants = currentParticipantUuids(world);
        meeting.pruneVotes(participants);
        meeting.tick();
        if (meeting.isExpired() || meeting.allConfirmed(participants)) {
            finishMeeting(world);
        } else if (meeting.ticksRemaining() % 20 == 0) {
            syncToTabletHolders(world);
        }
    }

    public static void clearRoundState(ServerWorld world) {
        TabletWorldComponent.KEY.get(world).clearRoundState();
        syncToTabletHolders(world);
    }

    public static TabletSnapshot buildSnapshot(ServerPlayerEntity viewer) {
        ServerWorld world = viewer.getServerWorld();
        TabletWorldComponent tablet = TabletWorldComponent.KEY.get(world);
        GameWorldComponent game = GameWorldComponent.KEY.get(world);
        Set<UUID> participants = currentParticipantUuids(world);
        tablet.pruneRemovalApprovals(participants);

        return new TabletSnapshot(
                TabletAccess.hasTabletInHotbar(viewer),
                participants.contains(viewer.getUuid()),
                TabletRules.secondsCeil(tablet.meetingCooldownTicks(world.getTime())),
                tablet.remainingMeetingCalls(viewer.getUuid()),
                TabletAccess.tabletHolders(world).stream()
                        .sorted(Comparator.comparing(player -> player.getName().getString(), String.CASE_INSENSITIVE_ORDER))
                        .map(player -> new TabletSnapshot.PlayerRow(
                                player.getUuid(),
                                player.getName().getString(),
                                game.hasAnyRole(player.getUuid())
                        ))
                        .toList(),
                tablet.chatHistory().stream()
                        .map(message -> new TabletSnapshot.ChatRow(
                                message.senderUuid(),
                                message.senderName(),
                                message.message(),
                                message.timeMillis()
                        ))
                        .toList(),
                meetingSnapshot(world, tablet, viewer),
                tablet.suspects().stream()
                        .sorted(Comparator.comparing(uuid -> playerName(world, uuid), String.CASE_INSENSITIVE_ORDER))
                        .map(uuid -> new TabletSnapshot.SuspectRow(
                                uuid,
                                playerName(world, uuid),
                                tablet.removalApprovals(uuid).size(),
                                TabletRules.requiredTwoThirds(participants.size()),
                                tablet.removalApprovals(uuid).contains(viewer.getUuid())
                        ))
                        .toList()
        );
    }

    private static TabletSnapshot.Meeting meetingSnapshot(
            ServerWorld world,
            TabletWorldComponent tablet,
            ServerPlayerEntity viewer
    ) {
        TabletWorldComponent.Meeting meeting = tablet.meeting();
        if (meeting == null) {
            return TabletSnapshot.Meeting.inactive();
        }

        Map<UUID, Integer> voteCounts = validVoteCounts(world, tablet, meeting);
        return new TabletSnapshot.Meeting(
                true,
                TabletRules.secondsCeil(meeting.ticksRemaining()),
                meeting.isConfirmed(viewer.getUuid()),
                meeting.isAbstaining(viewer.getUuid()),
                meeting.voteFor(viewer.getUuid()).orElse(null),
                GameWorldComponent.KEY.get(world).getAllPlayers().stream()
                        .sorted(Comparator.comparing(uuid -> playerName(world, uuid), String.CASE_INSENSITIVE_ORDER))
                        .map(uuid -> new TabletSnapshot.VoteTarget(
                                uuid,
                                playerName(world, uuid),
                                isSelectableMeetingTarget(world, tablet, uuid),
                                voteCounts.getOrDefault(uuid, 0)
                        ))
                        .toList()
        );
    }

    private static void finishMeeting(ServerWorld world) {
        TabletWorldComponent tablet = TabletWorldComponent.KEY.get(world);
        TabletWorldComponent.Meeting meeting = tablet.meeting();
        if (meeting == null) {
            return;
        }

        Optional<UUID> winner = TabletRules.uniqueHighestVote(validVoteCounts(world, tablet, meeting));
        tablet.clearMeeting();
        tablet.startMeetingCooldown(world.getTime());
        if (winner.isPresent()) {
            tablet.addSuspect(winner.get());
            broadcastToTabletHolders(world, Text.translatable(
                    "message.sparkstrength.tablet.suspect_added",
                    playerName(world, winner.get())
            ));
        } else {
            broadcastToTabletHolders(world, Text.translatable("message.sparkstrength.tablet.suspect_tie"));
        }
        syncToTabletHolders(world);
    }

    private static Map<UUID, Integer> validVoteCounts(
            ServerWorld world,
            TabletWorldComponent tablet,
            TabletWorldComponent.Meeting meeting
    ) {
        Set<UUID> participants = currentParticipantUuids(world);
        LinkedHashMap<UUID, Integer> voteCounts = new LinkedHashMap<>();
        for (Map.Entry<UUID, UUID> entry : meeting.votes().entrySet()) {
            if (!participants.contains(entry.getKey()) || !isSelectableMeetingTarget(world, tablet, entry.getValue())) {
                continue;
            }
            voteCounts.merge(entry.getValue(), 1, Integer::sum);
        }
        return voteCounts;
    }

    private static boolean isSelectableMeetingTarget(ServerWorld world, TabletWorldComponent tablet, UUID targetUuid) {
        return GameWorldComponent.KEY.get(world).hasAnyRole(targetUuid) && !tablet.isSuspect(targetUuid);
    }

    private static Set<UUID> currentParticipantUuids(ServerWorld world) {
        return TabletAccess.aliveTabletParticipants(world).stream()
                .map(ServerPlayerEntity::getUuid)
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    private static String playerName(ServerWorld world, UUID uuid) {
        ServerPlayerEntity online = world.getServer().getPlayerManager().getPlayer(uuid);
        if (online != null) {
            return online.getName().getString();
        }
        GameProfile profile = GameWorldComponent.KEY.get(world).getGameProfiles().get(uuid);
        return profile == null ? uuid.toString() : profile.getName();
    }

    private static void broadcastToTabletHolders(ServerWorld world, Text message) {
        for (ServerPlayerEntity player : TabletAccess.tabletHolders(world)) {
            player.sendMessage(message, true);
        }
    }
}
