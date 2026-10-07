package annina.sparkstrength.role.reporter;

import annina.sparkstrength.component.reporter.ReporterCommunicationComponent;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 记者通讯加强的文字聊天桥接。
 *
 * <p>这里不取消原聊天，只在接线/广播生效期间，把发送者的聊天额外复制到目标玩家的 actionbar。
 * 语音转发在 {@code SparkStrengthVoiceChatPlugin} 内处理。</p>
 */
public final class ReporterCommunicationManager {
    private ReporterCommunicationManager() {
    }

    public static void register() {
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register(ReporterCommunicationManager::handleChatMessage);
    }

    private static boolean handleChatMessage(
            SignedMessage message,
            ServerPlayerEntity sender,
            MessageType.Parameters params
    ) {
        if (!ReporterCommunicationService.isCommunicableEndpoint(sender)) {
            return true;
        }

        String rawContent = message.getSignedContent();
        if (rawContent == null || rawContent.isBlank()) {
            return true;
        }

        bridgeConnectionChat(sender, rawContent);
        bridgeBroadcastChat(sender, rawContent);
        return true;
    }

    private static void bridgeConnectionChat(@NotNull ServerPlayerEntity sender, @NotNull String rawContent) {
        for (ServerPlayerEntity possibleReporter : sender.getServer().getPlayerManager().getPlayerList()) {
            ReporterCommunicationComponent component = ReporterCommunicationComponent.KEY.get(possibleReporter);
            UUIDPair pair = getConnectionPair(component);
            if (pair == null) {
                continue;
            }

            UUID recipientUuid = null;
            if (sender.getUuid().equals(pair.first())) {
                recipientUuid = pair.second();
            } else if (sender.getUuid().equals(pair.second())) {
                recipientUuid = pair.first();
            }

            if (recipientUuid == null) {
                continue;
            }

            ServerPlayerEntity recipient = sender.getServer().getPlayerManager().getPlayer(recipientUuid);
            if (ReporterCommunicationService.isCommunicableEndpoint(recipient)) {
                sendBridgedActionbar(recipient, sender, rawContent);
            }
        }
    }

    private static void bridgeBroadcastChat(@NotNull ServerPlayerEntity sender, @NotNull String rawContent) {
        for (ServerPlayerEntity possibleReporter : sender.getServer().getPlayerManager().getPlayerList()) {
            ReporterCommunicationComponent component = ReporterCommunicationComponent.KEY.get(possibleReporter);
            if (!component.hasActiveBroadcast() || component.getBroadcastTarget() == null) {
                continue;
            }
            if (!sender.getUuid().equals(component.getBroadcastTarget())) {
                continue;
            }

            for (ServerPlayerEntity recipient : sender.getServer().getPlayerManager().getPlayerList()) {
                if (recipient.getUuid().equals(sender.getUuid())) {
                    continue;
                }
                // 广播采访要求“所有在场玩家都能听到，无论是否存活”，因此只要求还属于本局且在线。
                if (ReporterCommunicationService.isInGameOnlinePlayer(sender, recipient)) {
                    sendBridgedActionbar(recipient, sender, rawContent);
                }
            }
        }
    }

    private static void sendBridgedActionbar(
            @NotNull ServerPlayerEntity recipient,
            @NotNull ServerPlayerEntity sender,
            @NotNull String rawContent
    ) {
        MutableText text = Text.translatable(
                "message.sparkstrength.reporter.chat_bridge",
                sender.getGameProfile().getName(),
                rawContent
        ).withColor(Noellesroles.REPORTER.color());
        recipient.sendMessage(text, true);
    }

    public static @Nullable UUIDPair getConnectionPair(@NotNull ReporterCommunicationComponent component) {
        if (!component.hasActiveConnection()
                || component.getConnectionPlayerOne() == null
                || component.getConnectionPlayerTwo() == null) {
            return null;
        }
        return new UUIDPair(component.getConnectionPlayerOne(), component.getConnectionPlayerTwo());
    }

    public record UUIDPair(UUID first, UUID second) {
    }
}
