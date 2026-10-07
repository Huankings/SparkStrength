package annina.sparkstrength.role.demonhunter;

import annina.sparkstrength.component.demonhunter.DemonHunterSniffPlayerComponent;
import annina.sparkstrength.replay.SparkStrengthReplayFormatters;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 猎魔人嗅探技能的服务端权威逻辑。
 *
 * <p>客户端只负责“按了能力键”这一输入请求；职业、冷却、目标范围、目标阵营、
 * 是否吞噬/死亡等全部在服务端重新校验，避免改包绕过冷却或嗅探不合法目标。</p>
 */
public final class DemonHunterSniffService {
    private DemonHunterSniffService() {
    }

    public static void assignForRole(ServerPlayerEntity player, Role role) {
        DemonHunterSniffPlayerComponent component = DemonHunterSniffPlayerComponent.KEY.get(player);
        if (DemonHunterSniffRules.isDemonHunter(role)) {
            component.initializeSniff();
        } else {
            component.clearSniff();
        }
    }

    public static void trySniff(ServerPlayerEntity hunter) {
        ServerWorld world = hunter.getServerWorld();
        GameWorldComponent game = GameWorldComponent.KEY.get(world);
        Role hunterRole = game.getRole(hunter);
        if (!DemonHunterSniffRules.isDemonHunter(hunterRole)) {
            return;
        }
        if (!GameFunctions.isPlayerPlayingAndAlive(hunter)
                || !GameFunctions.isPlayerAliveAndSurvival(hunter)
                || SwallowedPlayerComponent.isPlayerSwallowed(hunter)) {
            return;
        }

        DemonHunterSniffPlayerComponent component = DemonHunterSniffPlayerComponent.KEY.get(hunter);
        if (component.isSniffOnCooldown()) {
            return;
        }

        List<ServerPlayerEntity> targets = findSniffTargets(hunter, game);
        sendHunterResult(hunter, targets.size());
        recordSniffScan(world, hunter, targets.size());

        for (ServerPlayerEntity target : targets) {
            handleSniffedTarget(world, hunter, component, target);
        }

        // 无论有没有嗅探到目标，只要本次合法发动，就进入 90 秒冷却。
        component.startSniffCooldown();
    }

    public static void clearPlayer(ServerPlayerEntity player) {
        DemonHunterSniffPlayerComponent.KEY.get(player).clearSniff();
    }

    private static List<ServerPlayerEntity> findSniffTargets(ServerPlayerEntity hunter, GameWorldComponent game) {
        List<ServerPlayerEntity> targets = new ArrayList<>();
        UUID hunterUuid = hunter.getUuid();
        for (ServerPlayerEntity target : hunter.getServerWorld().getPlayers()) {
            if (hunterUuid.equals(target.getUuid())) {
                continue;
            }
            if (!isValidLivingTarget(target)) {
                continue;
            }
            if (hunter.squaredDistanceTo(target) > DemonHunterSniffRules.SNIFF_RADIUS_SQUARED) {
                continue;
            }

            Role targetRole = game.getRole(target);
            if (DemonHunterSniffRules.isSniffableFrenzyCandidate(target, targetRole)) {
                targets.add(target);
            }
        }
        return targets;
    }

    private static boolean isValidLivingTarget(ServerPlayerEntity target) {
        return GameFunctions.isPlayerPlayingAndAlive(target)
                && GameFunctions.isPlayerAliveAndSurvival(target)
                && !SwallowedPlayerComponent.isPlayerSwallowed(target);
    }

    private static void handleSniffedTarget(
            ServerWorld world,
            ServerPlayerEntity hunter,
            DemonHunterSniffPlayerComponent component,
            ServerPlayerEntity target
    ) {
        UUID targetUuid = target.getUuid();
        boolean alreadyMarked = component.isSniffMarked(targetUuid);
        if (alreadyMarked) {
            component.revealSniffTarget(targetUuid);
            sendTargetMessage(target, "message.sparkstrength.demon_hunter_sniff.revealed_by_hunter");
            recordSniffReveal(world, hunter, target);
        } else {
            component.markSniffTarget(targetUuid);
            sendTargetMessage(target, "message.sparkstrength.demon_hunter_sniff.marked_by_hunter");
        }

        // 只对被嗅探且符合条件的目标本人播放提示音，避免其他玩家通过声音反查位置。
        target.playSoundToPlayer(SoundEvents.BLOCK_BELL_USE, SoundCategory.PLAYERS, 1.0F, 1.0F);
    }

    private static void sendHunterResult(ServerPlayerEntity hunter, int targetCount) {
        MutableText message = targetCount > 0
                ? Text.translatable("message.sparkstrength.demon_hunter_sniff.found", targetCount)
                : Text.translatable("message.sparkstrength.demon_hunter_sniff.none");
        hunter.sendMessage(message.withColor(Noellesroles.DEMON_HUNTER.color()), true);
    }

    private static void sendTargetMessage(ServerPlayerEntity target, String translationKey) {
        target.sendMessage(
                Text.translatable(translationKey).withColor(Noellesroles.DEMON_HUNTER.color()),
                true
        );
    }

    private static void recordSniffScan(ServerWorld world, ServerPlayerEntity hunter, int targetCount) {
        NbtCompound extra = new NbtCompound();
        extra.putInt("count", targetCount);
        GameRecordManager.recordGlobalEvent(
                world,
                targetCount > 0
                        ? SparkStrengthReplayFormatters.DEMON_HUNTER_SNIFF_FOUND
                        : SparkStrengthReplayFormatters.DEMON_HUNTER_SNIFF_NONE,
                hunter,
                extra
        );
    }

    private static void recordSniffReveal(ServerWorld world, ServerPlayerEntity hunter, ServerPlayerEntity target) {
        NbtCompound extra = new NbtCompound();
        extra.putUuid("target", target.getUuid());
        GameRecordManager.recordGlobalEvent(
                world,
                SparkStrengthReplayFormatters.DEMON_HUNTER_SNIFF_REVEALED,
                hunter,
                extra
        );
    }
}
