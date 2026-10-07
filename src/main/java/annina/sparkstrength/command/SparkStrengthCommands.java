package annina.sparkstrength.command;

import annina.sparkstrength.mixin.wathe.GameWorldComponentAccessor;
import annina.sparkstrength.compat.SparkTraitsCompat;
import annina.sparkstrength.component.tablet.TabletWorldComponent;
import annina.sparkstrength.tablet.TabletRules;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

import java.util.HashSet;
import java.util.UUID;

/**
 * Registers SparkStrength administrator commands.
 * 注册 SparkStrength 管理命令。
 */
public final class SparkStrengthCommands {
    private static final int DEFAULT_COMMAND_LEVEL = 2;

    /**
     * 调试开关：执行 setPlayerAlive 时是否同时把目标切换为冒险模式。
     *
     * <p>默认关闭，命令只修改 Wathe 的 deadPlayers 逻辑标记，保留目标当前的创造/旁观模式。
     * 如果后续测试需要让目标立即拥有普通玩家的可行动模式，把这里改为 true 即可。</p>
     */
    private static final boolean SET_ALIVE_CHANGES_TO_ADVENTURE = false;

    private SparkStrengthCommands() {
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("sparkstrength:emergencyMeetingChances")
                .requires(source -> source.hasPermissionLevel(DEFAULT_COMMAND_LEVEL))
                .then(CommandManager.argument("num", IntegerArgumentType.integer(0))
                        .executes(context -> setEmergencyMeetingChances(
                                context.getSource(),
                                IntegerArgumentType.getInteger(context, "num")
                        ))));
        // Keep the requested misspelled command as an alias beside the canonical name.
        // 保留需求中写出的拼写作为别名，同时提供正确拼写。
        registerVoteTimeCommand(dispatcher, "sparkstrength:voteTime");
        registerVoteTimeCommand(dispatcher, "sparkstength:voteTime");

        registerPlayerStateCommand(dispatcher, "sparkstrength:setPlayerAlive", true);
        registerPlayerStateCommand(dispatcher, "sparkstrength:setPlayerDead", false);
    }

    private static int setEmergencyMeetingChances(ServerCommandSource source, int chances) {
        for (ServerWorld world : source.getServer().getWorlds()) {
            TabletWorldComponent.KEY.get(world).setEmergencyMeetingChances(chances);
        }
        source.sendFeedback(
                () -> Text.translatable("commands.sparkstrength.emergency_meeting_chances.success", chances),
                true
        );
        return chances;
    }

    private static void registerVoteTimeCommand(CommandDispatcher<ServerCommandSource> dispatcher, String literal) {
        dispatcher.register(CommandManager.literal(literal)
                .requires(source -> source.hasPermissionLevel(DEFAULT_COMMAND_LEVEL))
                .then(CommandManager.argument("sec", IntegerArgumentType.integer(1, Integer.MAX_VALUE / 20))
                        .executes(context -> setVoteTime(
                                context.getSource(),
                                IntegerArgumentType.getInteger(context, "sec")
                        ))));
    }

    private static int setVoteTime(ServerCommandSource source, int seconds) {
        int ticks = TabletRules.ticksFromSeconds(seconds);
        for (ServerWorld world : source.getServer().getWorlds()) {
            TabletWorldComponent.KEY.get(world).setMeetingDurationTicks(ticks);
        }
        source.sendFeedback(
                () -> Text.translatable("commands.sparkstrength.vote_time.success", seconds),
                true
        );
        return seconds;
    }

    /**
     * 注册用于测试 Wathe 存活状态的两个管理员命令。
     *
     * <p>这里使用在线玩家参数，而不是直接调用真正的 killPlayer：调试命令只修改
     * deadPlayers，不生成尸体、不清空背包、不发放击杀奖励，也不触发死亡事件。</p>
     */
    private static void registerPlayerStateCommand(
            CommandDispatcher<ServerCommandSource> dispatcher,
            String literal,
            boolean alive
    ) {
        dispatcher.register(CommandManager.literal(literal)
                .requires(source -> source.hasPermissionLevel(DEFAULT_COMMAND_LEVEL))
                .then(CommandManager.argument("player", EntityArgumentType.player())
                        .executes(context -> setPlayerState(
                                context.getSource(),
                                EntityArgumentType.getPlayer(context, "player"),
                                alive
                        ))));
    }

    /**
     * 修改指定玩家的 Wathe 逻辑存活状态。
     *
     * <p>命令允许在对局未开始时执行，方便提前准备测试数据；此时会额外提示“当前不在对局”，
     * 因为 isPlayerPlayingAndAlive 仍然会被 isRunning 条件判定为 false。玩家必须已经拥有 Wathe
     * 职业，否则修改 deadPlayers 没有任何实际效果，也会造成调试误判。</p>
     */
    private static int setPlayerState(
            ServerCommandSource source,
            ServerPlayerEntity target,
            boolean alive
    ) {
        GameWorldComponent game = GameWorldComponent.KEY.get(target.getWorld());
        UUID uuid = target.getUuid();

        if (!game.hasAnyRole(uuid)) {
            source.sendError(Text.translatable("commands.sparkstrength.player_state.no_role", target.getName()));
            return 0;
        }

        HashSet<UUID> deadPlayers = ((GameWorldComponentAccessor) game).sparkstrength$getDeadPlayers();
        boolean currentlyAlive = !deadPlayers.contains(uuid);

        if (currentlyAlive == alive) {
            if (alive && SET_ALIVE_CHANGES_TO_ADVENTURE) {
                // 即使目标原本已经是逻辑存活状态，开启开关后执行命令仍会统一切换为冒险模式。
                target.changeGameMode(net.minecraft.world.GameMode.ADVENTURE);
            }
            source.sendFeedback(
                    () -> Text.translatable(
                            alive
                                    ? "commands.sparkstrength.player_state.already_alive"
                                    : "commands.sparkstrength.player_state.already_dead",
                            target.getName()
                    ),
                    false
            );
        } else if (alive) {
            // 只移除 Wathe 的死亡 UUID。这里不恢复背包、尸体或死亡时的其他状态。
            deadPlayers.remove(uuid);
            game.sync();

            if (SET_ALIVE_CHANGES_TO_ADVENTURE) {
                // 可选调试行为：开启常量后，逻辑复活时一并切换到冒险模式。
                target.changeGameMode(net.minecraft.world.GameMode.ADVENTURE);
            }

            source.sendFeedback(
                    () -> Text.translatable("commands.sparkstrength.player_alive.success", target.getName()),
                    true
            );
        } else {
            // 只加入 Wathe 的死亡 UUID，不调用真正的 killPlayer，避免产生死亡副作用。
            game.markPlayerDead(uuid);
            game.sync();
            source.sendFeedback(
                    () -> Text.translatable("commands.sparkstrength.player_dead.success", target.getName()),
                    true
            );
        }

        if (!alive) {
            // SparkTraits 的准心词条显示使用死亡快照；可选桥接只记录词条，不触发真实死亡流程。
            // 即使目标已经是非存活状态，重复执行命令也会补写快照，方便修复旧状态或重新同步。
            SparkTraitsCompat.snapshotDeathTraitsForDebug(target);
        }

        // 通知可选的 SparkTraits 重新同步词条可见性；未安装时该桥接会安全跳过。
        SparkTraitsCompat.syncTraitVisibility(target);

        if (!game.isRunning()) {
            source.sendFeedback(
                    () -> Text.translatable("commands.sparkstrength.player_state.game_not_running"),
                    false
            );
        }

        return 1;
    }
}
