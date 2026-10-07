package annina.sparkstrength.network;

import annina.sparkstrength.network.criminologist.OpenCriminologistScreenS2CPacket;
import annina.sparkstrength.network.criminologist.SelectCriminologistTargetC2SPacket;
import annina.sparkstrength.network.coroner.CoronerMorphC2SPacket;
import annina.sparkstrength.network.demonhunter.DemonHunterSniffC2SPacket;
import annina.sparkstrength.network.noisemaker.NoisemakerGlowC2SPacket;
import annina.sparkstrength.network.phantom.PhantomBackpackInvisibilityC2SPacket;
import annina.sparkstrength.network.professor.ProfessorRemoteFeedC2SPacket;
import annina.sparkstrength.network.reporter.ReporterCommunicationC2SPacket;
import annina.sparkstrength.network.tablet.ApproveSuspectRemovalC2SPacket;
import annina.sparkstrength.network.tablet.CallTabletMeetingC2SPacket;
import annina.sparkstrength.network.tablet.CastTabletVoteC2SPacket;
import annina.sparkstrength.network.tablet.ConfirmTabletVoteC2SPacket;
import annina.sparkstrength.network.tablet.OpenTabletScreenS2CPacket;
import annina.sparkstrength.network.tablet.RequestTabletSnapshotC2SPacket;
import annina.sparkstrength.network.tablet.SendTabletChatC2SPacket;
import annina.sparkstrength.network.tablet.SyncTabletSnapshotS2CPacket;
import annina.sparkstrength.network.shadowjester.SyncShadowJesterShowdownMusicS2CPacket;
import annina.sparkstrength.network.veteran.SyncVeteranBlackoutS2CPacket;
import annina.sparkstrength.network.timekeeper.TimekeeperWatchModeC2SPacket;
import annina.sparkstrength.role.noisemaker.NoisemakerGlowService;
import annina.sparkstrength.role.phantom.PhantomBackpackService;
import annina.sparkstrength.role.coroner.CoronerService;
import annina.sparkstrength.role.professor.ProfessorSerumService;
import annina.sparkstrength.role.reporter.ReporterCommunicationService;
import annina.sparkstrength.role.detective.CriminologistService;
import annina.sparkstrength.role.demonhunter.DemonHunterSniffService;
import annina.sparkstrength.role.timekeeper.TimekeeperWatchService;
import annina.sparkstrength.tablet.TabletStateService;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/**
 * SparkStrength 网络包注册。
 */
public final class SparkStrengthPackets {
    private SparkStrengthPackets() {
    }

    public static void registerServer() {
        PayloadTypeRegistry.playC2S().register(NoisemakerGlowC2SPacket.ID, NoisemakerGlowC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(PhantomBackpackInvisibilityC2SPacket.ID, PhantomBackpackInvisibilityC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(CoronerMorphC2SPacket.ID, CoronerMorphC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(ProfessorRemoteFeedC2SPacket.ID, ProfessorRemoteFeedC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(ReporterCommunicationC2SPacket.ID, ReporterCommunicationC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(SelectCriminologistTargetC2SPacket.ID, SelectCriminologistTargetC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(DemonHunterSniffC2SPacket.ID, DemonHunterSniffC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(RequestTabletSnapshotC2SPacket.ID, RequestTabletSnapshotC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(SendTabletChatC2SPacket.ID, SendTabletChatC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(CallTabletMeetingC2SPacket.ID, CallTabletMeetingC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(CastTabletVoteC2SPacket.ID, CastTabletVoteC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(ConfirmTabletVoteC2SPacket.ID, ConfirmTabletVoteC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(ApproveSuspectRemovalC2SPacket.ID, ApproveSuspectRemovalC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(TimekeeperWatchModeC2SPacket.ID, TimekeeperWatchModeC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(SerialPistolShootC2SPayload.ID, SerialPistolShootC2SPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(OpenCriminologistScreenS2CPacket.ID, OpenCriminologistScreenS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(OpenTabletScreenS2CPacket.ID, OpenTabletScreenS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(SyncTabletSnapshotS2CPacket.ID, SyncTabletSnapshotS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(SyncVeteranBlackoutS2CPacket.ID, SyncVeteranBlackoutS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(
                SyncShadowJesterShowdownMusicS2CPacket.ID,
                SyncShadowJesterShowdownMusicS2CPacket.CODEC
        );
        ServerPlayNetworking.registerGlobalReceiver(NoisemakerGlowC2SPacket.ID, (payload, context) ->
                NoisemakerGlowService.tryUseBackpackGlow(context.player(), payload.targetPlayer())
        );
        ServerPlayNetworking.registerGlobalReceiver(PhantomBackpackInvisibilityC2SPacket.ID, (payload, context) ->
                PhantomBackpackService.tryUseBackpackInvisibility(context.player(), payload.targetPlayer())
        );
        ServerPlayNetworking.registerGlobalReceiver(CoronerMorphC2SPacket.ID, (payload, context) ->
                CoronerService.selectDisguise(context.player(), payload.targetPlayer())
        );
        ServerPlayNetworking.registerGlobalReceiver(ProfessorRemoteFeedC2SPacket.ID, (payload, context) ->
                ProfessorSerumService.tryRemoteFeed(context.player(), payload.targetPlayer(), payload.serumType())
        );
        ServerPlayNetworking.registerGlobalReceiver(ReporterCommunicationC2SPacket.ID, (payload, context) ->
                ReporterCommunicationService.handle(payload, context.player())
        );
        ServerPlayNetworking.registerGlobalReceiver(SelectCriminologistTargetC2SPacket.ID,
                (payload, context) -> CriminologistService.handleSelection(
                        context.player(),
                        payload.victimUuid(),
                        payload.suspectUuid()
                ));
        ServerPlayNetworking.registerGlobalReceiver(DemonHunterSniffC2SPacket.ID,
                (payload, context) -> DemonHunterSniffService.trySniff(context.player()));
        ServerPlayNetworking.registerGlobalReceiver(RequestTabletSnapshotC2SPacket.ID,
                (payload, context) -> TabletStateService.syncTo(context.player()));
        ServerPlayNetworking.registerGlobalReceiver(SendTabletChatC2SPacket.ID,
                (payload, context) -> TabletStateService.sendChat(context.player(), payload.message()));
        ServerPlayNetworking.registerGlobalReceiver(CallTabletMeetingC2SPacket.ID,
                (payload, context) -> TabletStateService.callMeeting(context.player()));
        ServerPlayNetworking.registerGlobalReceiver(CastTabletVoteC2SPacket.ID,
                (payload, context) -> TabletStateService.castVote(context.player(), payload.targetUuid()));
        ServerPlayNetworking.registerGlobalReceiver(ConfirmTabletVoteC2SPacket.ID,
                (payload, context) -> TabletStateService.confirmVote(context.player()));
        ServerPlayNetworking.registerGlobalReceiver(ApproveSuspectRemovalC2SPacket.ID,
                (payload, context) -> TabletStateService.setSuspectRemovalApproval(
                        context.player(),
                        payload.suspectUuid(),
                        payload.approved()
                ));
        ServerPlayNetworking.registerGlobalReceiver(TimekeeperWatchModeC2SPacket.ID,
                (payload, context) -> TimekeeperWatchService.switchMode(context.player(), payload.modeOrdinal()));
        ServerPlayNetworking.registerGlobalReceiver(SerialPistolShootC2SPayload.ID,
                SerialPistolShootC2SPayload::receive);
    }
}
