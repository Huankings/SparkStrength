package annina.sparkstrength.role.reporter;

import annina.sparkstrength.component.reporter.ReporterCommunicationComponent;
import annina.sparkstrength.network.reporter.ReporterCommunicationC2SPacket;
import annina.sparkstrength.replay.SparkStrengthReplayFormatters;
import com.mojang.authlib.GameProfile;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 记者背包通讯加强的服务端入口。
 *
 * <p>客户端只负责提交两次点击到的 UUID，真正的角色、冷却、死亡、饕餮吞噬豁免都在这里判定。</p>
 */
public final class ReporterCommunicationService {
    private ReporterCommunicationService() {
    }

    public static void handle(@NotNull ReporterCommunicationC2SPacket payload, @NotNull ServerPlayerEntity reporter) {
        if (payload.firstPlayer() == null || payload.secondPlayer() == null) {
            return;
        }

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(reporter.getWorld());
        if (!gameWorld.isRole(reporter, Noellesroles.REPORTER)
                || !gameWorld.isRunning()
                || !GameFunctions.isPlayerPlayingAndAlive(reporter)
                || !GameFunctions.isPlayerAliveAndSurvival(reporter)
                || SwallowedPlayerComponent.isPlayerSwallowed(reporter)) {
            return;
        }

        ReporterCommunicationComponent component = ReporterCommunicationComponent.KEY.get(reporter);
        if (component.isOnCooldown()) {
            return;
        }

        ServerPlayerEntity first = reporter.getServer().getPlayerManager().getPlayer(payload.firstPlayer());
        ServerPlayerEntity second = reporter.getServer().getPlayerManager().getPlayer(payload.secondPlayer());

        if (payload.firstPlayer().equals(payload.secondPlayer())) {
            handleBroadcast(reporter, component, gameWorld, payload.firstPlayer(), first);
        } else {
            handleConnection(reporter, component, gameWorld, payload.firstPlayer(), payload.secondPlayer(), first, second);
        }
    }

    private static void handleConnection(
            @NotNull ServerPlayerEntity reporter,
            @NotNull ReporterCommunicationComponent component,
            @NotNull GameWorldComponent gameWorld,
            @NotNull UUID firstUuid,
            @NotNull UUID secondUuid,
            @Nullable ServerPlayerEntity first,
            @Nullable ServerPlayerEntity second
    ) {
        boolean firstLive = isCommunicableEndpoint(first);
        boolean secondLive = isCommunicableEndpoint(second);
        String firstName = resolveName(gameWorld, first, firstUuid);
        String secondName = resolveName(gameWorld, second, secondUuid);

        if (firstLive && secondLive && first != null && second != null) {
            component.startConnection(first, second);
            component.setCooldownTicks(ReporterCommunicationConstants.CONNECTION_SUCCESS_COOLDOWN_TICKS);
            reporter.sendMessage(
                    Text.translatable("message.sparkstrength.reporter.connection_selected", first.getDisplayName(), second.getDisplayName())
                            .withColor(Noellesroles.REPORTER.color()),
                    true
            );
            reporter.playSoundToPlayer(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 1.0f, 1.0f);

            NbtCompound extra = new NbtCompound();
            writeConnectionExtra(extra, firstUuid, secondUuid, firstName, secondName);
            GameRecordManager.recordGlobalEvent(
                    reporter.getServerWorld(),
                    SparkStrengthReplayFormatters.REPORTER_CONNECTION_STARTED,
                    reporter,
                    extra
            );
            return;
        }

        component.setCooldownTicks(ReporterCommunicationConstants.CONNECTION_FAILURE_COOLDOWN_TICKS);
        reporter.playSoundToPlayer(SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.PLAYERS, 1.0f, 1.0f);

        NbtCompound extra = new NbtCompound();
        writeConnectionExtra(extra, firstUuid, secondUuid, firstName, secondName);
        if (!firstLive && !secondLive) {
            reporter.sendMessage(
                    Text.translatable("message.sparkstrength.reporter.connection_failed_both_dead")
                            .withColor(Noellesroles.REPORTER.color()),
                    true
            );
            GameRecordManager.recordGlobalEvent(
                    reporter.getServerWorld(),
                    SparkStrengthReplayFormatters.REPORTER_CONNECTION_FAILED_BOTH_DEAD,
                    reporter,
                    extra
            );
            return;
        }

        UUID deadUuid = !firstLive ? firstUuid : secondUuid;
        String deadName = !firstLive ? firstName : secondName;
        extra.putUuid("dead_player", deadUuid);
        extra.putString("dead_player_name", deadName);
        reporter.sendMessage(
                Text.translatable("message.sparkstrength.reporter.connection_failed_one_dead")
                        .withColor(Noellesroles.REPORTER.color()),
                true
        );
        GameRecordManager.recordGlobalEvent(
                reporter.getServerWorld(),
                SparkStrengthReplayFormatters.REPORTER_CONNECTION_FAILED_ONE_DEAD,
                reporter,
                extra
        );
    }

    private static void handleBroadcast(
            @NotNull ServerPlayerEntity reporter,
            @NotNull ReporterCommunicationComponent component,
            @NotNull GameWorldComponent gameWorld,
            @NotNull UUID targetUuid,
            @Nullable ServerPlayerEntity target
    ) {
        String targetName = resolveName(gameWorld, target, targetUuid);
        if (isCommunicableEndpoint(target) && target != null) {
            component.startBroadcast(target);
            component.setCooldownTicks(ReporterCommunicationConstants.BROADCAST_SUCCESS_COOLDOWN_TICKS);
            reporter.sendMessage(
                    Text.translatable("message.sparkstrength.reporter.broadcast_selected", target.getDisplayName())
                            .withColor(Noellesroles.REPORTER.color()),
                    true
            );
            reporter.playSoundToPlayer(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 1.0f, 1.0f);

            NbtCompound extra = new NbtCompound();
            writeBroadcastExtra(extra, targetUuid, targetName);
            GameRecordManager.recordGlobalEvent(
                    reporter.getServerWorld(),
                    SparkStrengthReplayFormatters.REPORTER_BROADCAST_STARTED,
                    reporter,
                    extra
            );
            return;
        }

        component.setCooldownTicks(ReporterCommunicationConstants.BROADCAST_FAILURE_COOLDOWN_TICKS);
        reporter.sendMessage(
                Text.translatable("message.sparkstrength.reporter.broadcast_failed_dead")
                        .withColor(Noellesroles.REPORTER.color()),
                true
        );
        reporter.playSoundToPlayer(SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.PLAYERS, 1.0f, 1.0f);

        NbtCompound extra = new NbtCompound();
        writeBroadcastExtra(extra, targetUuid, targetName);
        extra.putUuid("dead_player", targetUuid);
        extra.putString("dead_player_name", targetName);
        GameRecordManager.recordGlobalEvent(
                reporter.getServerWorld(),
                SparkStrengthReplayFormatters.REPORTER_BROADCAST_FAILED,
                reporter,
                extra
        );
    }

    /**
     * 记者通讯的目标判定。
     *
     * <p>普通活人直接通过；被饕餮吞噬者虽然处于旁观模式，但仍未被 Wathe 标记死亡，
     * 这里显式放行，让“在饕餮肚子里的人”被接线或广播时可以和外界联系。</p>
     */
    public static boolean isCommunicableEndpoint(@Nullable ServerPlayerEntity player) {
        return player != null
                && GameFunctions.isPlayerPlayingAndAlive(player)
                && (GameFunctions.isPlayerAliveAndSurvival(player)
                || SwallowedPlayerComponent.isPlayerSwallowed(player));
    }

    public static boolean isInGameOnlinePlayer(@NotNull ServerPlayerEntity viewer, @NotNull ServerPlayerEntity player) {
        return GameWorldComponent.KEY.get(viewer.getWorld()).hasAnyRole(player.getUuid());
    }

    private static void writeConnectionExtra(
            @NotNull NbtCompound extra,
            @NotNull UUID firstUuid,
            @NotNull UUID secondUuid,
            @NotNull String firstName,
            @NotNull String secondName
    ) {
        extra.putUuid("player_one", firstUuid);
        extra.putUuid("player_two", secondUuid);
        extra.putString("player_one_name", firstName);
        extra.putString("player_two_name", secondName);
    }

    private static void writeBroadcastExtra(@NotNull NbtCompound extra, @NotNull UUID targetUuid, @NotNull String targetName) {
        extra.putUuid("target_player", targetUuid);
        extra.putString("target_player_name", targetName);
    }

    private static @NotNull String resolveName(
            @NotNull GameWorldComponent gameWorld,
            @Nullable ServerPlayerEntity player,
            @NotNull UUID uuid
    ) {
        if (player != null) {
            return player.getGameProfile().getName();
        }

        GameProfile profile = gameWorld.getGameProfiles().get(uuid);
        if (profile != null && profile.getName() != null && !profile.getName().isBlank()) {
            return profile.getName();
        }
        return uuid.toString().substring(0, 8);
    }
}
