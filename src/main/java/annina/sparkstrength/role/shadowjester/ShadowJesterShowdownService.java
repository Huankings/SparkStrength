package annina.sparkstrength.role.shadowjester;

import annina.sparkstrength.compat.SparkTraitsCompat;
import annina.sparkstrength.component.shadowjester.ShadowJesterShowdownWorldComponent;
import annina.sparkstrength.network.shadowjester.SyncShadowJesterShowdownMusicS2CPacket;
import annina.sparkstrength.replay.SparkStrengthReplayFormatters;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.event.CheckWinCondition;
import dev.doctor4t.wathe.api.event.GameEvents;
import dev.doctor4t.wathe.api.event.KillPlayer;
import dev.doctor4t.wathe.api.event.ShouldPunishGunShooter;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheItems;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.shadowjester.ShadowJesterPlayerComponent;
import org.agmas.noellesroles.vulture.VulturePlayerComponent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * SparkStrength 新版影子小丑“双影谢幕”规则。
 *
 * <p>这里刻意不调用 NoellesRoles 的 {@code activateShowdown}，也不写入
 * NoellesRoles 的 {@code showdownActive} 字段，从而保留原有“和杀手战斗”的旧谢幕。
 * 本服务只负责新增的“杀手全部死亡后清场”模式。</p>
 */
public final class ShadowJesterShowdownService {
    public static final int REVOLVER_COOLDOWN_TICKS = 80; // 4 秒
    private static boolean winPriorityListenerRegistered;

    private ShadowJesterShowdownService() {
    }

    public static void register() {
        // 结盟补偿由 NoellesRoles 握手 Mixin 调用；死亡和胜利逻辑在这里统一注册。
        KillPlayer.AFTER.register(ShadowJesterShowdownService::afterKill);
        ShouldPunishGunShooter.EVENT.register(ShadowJesterShowdownService::shouldPunishGunShooter);
        GameEvents.ON_FINISH_INITIALIZE.register((world, game) -> {
            if (world instanceof ServerWorld serverWorld) {
                stopShowdownMusic(serverWorld);
                ShadowJesterShowdownWorldComponent.KEY.get(serverWorld).clearRoundState();
            }
        });
        GameEvents.ON_FINISH_FINALIZE.register((world, game) -> {
            if (world instanceof ServerWorld serverWorld) {
                stopShowdownMusic(serverWorld);
                ShadowJesterShowdownWorldComponent.KEY.get(serverWorld).clearRoundState();
            }
        });
    }

    /**
     * 注册新版谢幕的高优先级胜利监听器。
     *
     * <p>由 NoellesRoles#registerEvents 的 HEAD 注入调用，确保本监听器先于
     * NoellesRoles 自身的饕餮、小丑、黑警、病原体、生存大师以及旧版影子小丑
     * 胜利检查执行。方法带有幂等保护，避免初始化流程重复调用时重复注册。</p>
     */
    public static void registerWinPriorityListener() {
        if (winPriorityListenerRegistered) {
            return;
        }
        winPriorityListenerRegistered = true;
        CheckWinCondition.EVENT.register(ShadowJesterShowdownService::checkWin);
    }

    /**
     * NoellesRoles 结盟握手完成后调用：
     * 申请方补锁具，且双方都获得本能透视。
     *
     * @param accepter 当前完成同意动作的影子小丑
     * @param proposer 先发起结盟的影子小丑
     */
    public static void enhanceAlliance(ServerPlayerEntity accepter, @Nullable ServerPlayerEntity proposer) {
        if (accepter == null || proposer == null) {
            return;
        }

        ShadowJesterPlayerComponent accepterComponent =
                ShadowJesterPlayerComponent.KEY.get(accepter);
        ShadowJesterPlayerComponent proposerComponent =
                ShadowJesterPlayerComponent.KEY.get(proposer);

        // 双方都打开本能透视；申请方原有透视仍保留，同意方在此处补上。
        proposerComponent.setInstinctVision(true);
        accepterComponent.setInstinctVision(true);

        // 只给申请方补一个锁具，避免重复握手或异常重放造成多个锁具。
        giveIfMissing(proposer, WatheItems.LOCKPICK);
    }

    /**
     * 由 ShadowJesterPlayerComponent#setAllied(boolean) 的 Mixin 调用。
     *
     * <p>NoellesRoles 在同意结盟时会先把申请方、再把同意方写成 allied。
     * 因此只有在当前玩家和互相绑定的搭档都已经 allied 后才执行强化，
     * 从而避免在半完成状态下提前发放锁具或透视。</p>
     */
    public static void enhanceAllianceAfterStateChange(ServerPlayerEntity player) {
        if (player == null
                || !(player.getWorld() instanceof ServerWorld world)) {
            return;
        }

        ShadowJesterPlayerComponent playerComponent =
                ShadowJesterPlayerComponent.KEY.get(player);
        if (!playerComponent.isAllied() || playerComponent.getPartnerUuid() == null) {
            return;
        }

        PlayerEntity partnerEntity = world.getPlayerByUuid(playerComponent.getPartnerUuid());
        if (!(partnerEntity instanceof ServerPlayerEntity partner)) {
            return;
        }

        ShadowJesterPlayerComponent partnerComponent =
                ShadowJesterPlayerComponent.KEY.get(partner);
        if (!partnerComponent.isAllied()
                || !player.getUuid().equals(partnerComponent.getPartnerUuid())) {
            return;
        }

        // 当前玩家是后完成 allied 写入的一方，也就是原规则中的同意方；
        // partner 是先发起方，因此只给 partner 补锁具，双方补齐本能透视。
        enhanceAlliance(player, partner);
    }

    /**
     * 判断一名玩家是否属于 SparkTraits 计算后的有效杀手阵营。
     *
     * <p>善良词条会把原始杀手排除；内鬼词条会把原始好人翻为杀手。
     * SparkTraits 缺失时，兼容层自动回退到 Wathe 原始角色阵营。</p>
     */
    public static boolean isEffectiveKiller(GameWorldComponent game, PlayerEntity player) {
        if (game == null || player == null) {
            return false;
        }
        return SparkTraitsCompat.isEffectiveKiller(game.getRole(player), player);
    }

    /**
     * 返回仍存活的、已经结盟且互相指向的影子小丑二人组。
     */
    public static @Nullable List<ServerPlayerEntity> findLivingBoundPair(
            ServerWorld world,
            GameWorldComponent game
    ) {
        for (UUID uuid : game.getAllWithRole(Noellesroles.SHADOW_JESTER)) {
            PlayerEntity first = world.getPlayerByUuid(uuid);
            if (!(first instanceof ServerPlayerEntity firstPlayer)
                    || !GameFunctions.isPlayerPlayingAndAlive(firstPlayer)) {
                continue;
            }

            ShadowJesterPlayerComponent firstComponent =
                    ShadowJesterPlayerComponent.KEY.get(firstPlayer);
            if (!firstComponent.isAllied() || firstComponent.getPartnerUuid() == null) {
                continue;
            }

            PlayerEntity second = world.getPlayerByUuid(firstComponent.getPartnerUuid());
            if (!(second instanceof ServerPlayerEntity secondPlayer)
                    || !GameFunctions.isPlayerPlayingAndAlive(secondPlayer)
                    || !game.isRole(secondPlayer, Noellesroles.SHADOW_JESTER)) {
                continue;
            }

            ShadowJesterPlayerComponent secondComponent =
                    ShadowJesterPlayerComponent.KEY.get(secondPlayer);
            if (!secondComponent.isAllied()
                    || !firstPlayer.getUuid().equals(secondComponent.getPartnerUuid())) {
                continue;
            }

            return List.of(firstPlayer, secondPlayer);
        }
        return null;
    }

    /**
     * 在最后一名有效杀手死亡后启动新版谢幕。
     */
    private static void afterKill(
            ServerPlayerEntity victim,
            @Nullable ServerPlayerEntity killer,
            Identifier deathReason
    ) {
        if (!(victim.getWorld() instanceof ServerWorld world)) {
            return;
        }

        GameWorldComponent game = GameWorldComponent.KEY.get(world);
        ShadowJesterShowdownWorldComponent showdown =
                ShadowJesterShowdownWorldComponent.KEY.get(world);

        // 双方命运绑定：NoellesRoles 会在一名影子小丑死亡后强制击杀另一名。
        // 这里在两次 AFTER 回调都结束后清理世界状态，使环境音淡出并恢复普通胜利判定。
        if (showdown.isActive() && !hasLivingShowdownJesters(world, showdown)) {
            stopShowdownMusic(world);
            showdown.clear();
            return;
        }

        if (showdown.isActive()
                || !game.isRunning()
                || !isEffectiveKiller(game, victim)
                || hasLivingEffectiveKiller(world, game)) {
            return;
        }

        List<ServerPlayerEntity> pair = findLivingBoundPair(world, game);
        // NoellesRoles 原有的“与杀手战斗”双影谢幕必须独立运行。
        // 如果旧机制已经激活，说明当前结盟影子小丑正在进行原版终局对决，
        // 此时不能因为某名杀手死亡而再次启动 SparkStrength 的新版清场谢幕。
        if (pair == null || isLegacyShowdownActive(pair)) {
            return;
        }

        startShowdown(world, pair);
    }

    /**
     * 判断结盟双方是否已经进入 NoellesRoles 原有的双影谢幕。
     *
     * <p>这个状态属于 NoellesRoles 的旧机制；新版谢幕使用独立的世界组件，
     * 两者必须明确区分，避免同一局同时开启两套终局规则。</p>
     */
    private static boolean isLegacyShowdownActive(List<ServerPlayerEntity> pair) {
        for (ServerPlayerEntity player : pair) {
            if (ShadowJesterPlayerComponent.KEY.get(player).isShowdownActive()) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasLivingEffectiveKiller(ServerWorld world, GameWorldComponent game) {
        for (UUID uuid : game.getAllPlayers()) {
            PlayerEntity player = world.getPlayerByUuid(uuid);
            if (GameFunctions.isPlayerPlayingAndAlive(player)
                    && isEffectiveKiller(game, player)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasLivingShowdownJesters(
            ServerWorld world,
            ShadowJesterShowdownWorldComponent showdown
    ) {
        UUID firstUuid = showdown.getFirstJester();
        UUID secondUuid = showdown.getSecondJester();
        return firstUuid != null
                && secondUuid != null
                && GameFunctions.isPlayerPlayingAndAlive(world.getPlayerByUuid(firstUuid))
                && GameFunctions.isPlayerPlayingAndAlive(world.getPlayerByUuid(secondUuid));
    }

    private static void startShowdown(ServerWorld world, List<ServerPlayerEntity> pair) {
        ShadowJesterShowdownWorldComponent component =
                ShadowJesterShowdownWorldComponent.KEY.get(world);
        component.start(pair.get(0).getUuid(), pair.get(1).getUuid());
        startShowdownMusic(world);

        for (ServerPlayerEntity player : pair) {
            ShadowJesterPlayerComponent shadow =
                    ShadowJesterPlayerComponent.KEY.get(player);

            // 新版谢幕只使用本能透视和左轮，不触碰旧谢幕的 showdownActive/realKnife。
            shadow.setInstinctVision(true);
            removeAll(player, WatheItems.KNIFE);
            removeAll(player, WatheItems.DERRINGER);

            // 只清理需求明确要求收走的刀和德林加；其他普通物品全部保留。
            // 三件新版装备若玩家已有则不重复发放，确保不会因为重入得到多把。
            giveIfMissing(player, WatheItems.REVOLVER);
            giveIfMissing(player, WatheItems.LOCKPICK);
            giveIfMissing(player, WatheItems.CROWBAR);
            player.getItemCooldownManager().remove(WatheItems.REVOLVER);
        }

        for (ServerPlayerEntity player : world.getPlayers()) {
            player.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.TitleS2CPacket(
                    Text.translatable("title.sparkstrength.shadow_jester_showdown")
                            .formatted(Formatting.DARK_PURPLE, Formatting.BOLD)
            ));
            player.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.SubtitleS2CPacket(
                    Text.translatable("subtitle.sparkstrength.shadow_jester_showdown")
            ));
            player.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket(
                    10, 100, 10
            ));
        }

        dev.doctor4t.wathe.record.GameRecordManager.recordGlobalEvent(
                world,
                SparkStrengthReplayFormatters.SHADOW_JESTER_SHOWDOWN_STARTED,
                null,
                null
        );
    }

    /**
     * 向当前世界所有在线客户端明确发送“开始播放”状态。
     */
    private static void startShowdownMusic(ServerWorld world) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            ServerPlayNetworking.send(
                    player,
                    new SyncShadowJesterShowdownMusicS2CPacket(true)
            );
        }
    }

    /**
     * 向当前世界所有在线客户端明确发送“结束并淡出”状态。
     */
    private static void stopShowdownMusic(ServerWorld world) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            ServerPlayNetworking.send(
                    player,
                    new SyncShadowJesterShowdownMusicS2CPacket(false)
            );
        }
    }

    /**
     * 新版谢幕的高优先级胜利检查。
     *
     * <p>由于该监听器在 NoellesRoles 注册自身监听器之前注册，这里必须显式复刻
     * 秃鹫的最高优先级规则：先返回已吃满尸体的秃鹫胜利，再处理时间结束和新版
     * 影子小丑清场规则。旧版与杀手战斗的双影谢幕不使用本组件，因此仍交给
     * NoellesRoles 原有监听器处理。</p>
     */
    public static @Nullable CheckWinCondition.WinResult checkWin(
            ServerWorld world,
            GameWorldComponent game,
            GameFunctions.WinStatus currentStatus
    ) {
        ServerPlayerEntity winningVulture = findWinningVulture(world, game);
        if (winningVulture != null) {
            return CheckWinCondition.WinResult.neutralWin(winningVulture);
        }

        ShadowJesterShowdownWorldComponent component =
                ShadowJesterShowdownWorldComponent.KEY.get(world);
        if (!component.isActive()) {
            return null;
        }

        // 时间结束胜利按用户要求正常放行，不被新版谢幕阻止。
        if (currentStatus == GameFunctions.WinStatus.TIME) {
            return CheckWinCondition.WinResult.allow(GameFunctions.WinStatus.TIME);
        }

        if (!hasLivingShowdownJesters(world, component)) {
            stopShowdownMusic(world);
            component.clear();
            return null;
        }

        List<ServerPlayerEntity> livingOthers = new ArrayList<>();
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (!GameFunctions.isPlayerPlayingAndAlive(player)) {
                continue;
            }
            if (player.getUuid().equals(component.getFirstJester())
                    || player.getUuid().equals(component.getSecondJester())) {
                continue;
            }
            livingOthers.add(player);
        }

        if (livingOthers.isEmpty()) {
            PlayerEntity firstEntity = world.getPlayerByUuid(component.getFirstJester());
            PlayerEntity secondEntity = world.getPlayerByUuid(component.getSecondJester());
            if (firstEntity instanceof ServerPlayerEntity first
                    && secondEntity instanceof ServerPlayerEntity second) {
                stopShowdownMusic(world);
                return CheckWinCondition.WinResult.neutralWin(first, List.of(second));
            }
        }

        // 覆盖好人、杀手及所有普通中立胜利，但不影响上面的秃鹫和时间优先级。
        return CheckWinCondition.WinResult.block();
    }

    /**
     * 复刻 NoellesRoles 原有的秃鹫胜利判定，确保高优先级监听器不会把它拦截。
     */
    private static @Nullable ServerPlayerEntity findWinningVulture(
            ServerWorld world,
            GameWorldComponent game
    ) {
        for (UUID uuid : game.getAllWithRole(Noellesroles.VULTURE)) {
            PlayerEntity player = world.getPlayerByUuid(uuid);
            if (player instanceof ServerPlayerEntity vulture
                    && GameFunctions.isPlayerPlayingAndAlive(vulture)
                    && VulturePlayerComponent.KEY.get(vulture).hasWon()) {
                return vulture;
            }
        }
        return null;
    }

    /**
     * 新版谢幕期间，左轮命中原始好人时取消 Wathe 的掉枪/反噬/惩罚。
     */
    private static @Nullable ShouldPunishGunShooter.PunishResult shouldPunishGunShooter(
            PlayerEntity shooter,
            PlayerEntity victim
    ) {
        if (!(shooter instanceof ServerPlayerEntity serverShooter)
                || !(shooter.getWorld() instanceof ServerWorld world)) {
            return null;
        }
        if (!ShadowJesterShowdownWorldComponent.KEY.get(world).isActive()) {
            return null;
        }
        if (!isShowdownJester(world, serverShooter)
                || !shooter.getMainHandStack().isOf(WatheItems.REVOLVER)) {
            return null;
        }
        return ShouldPunishGunShooter.PunishResult.cancel();
    }

    /**
     * 判断玩家是否是当前新版谢幕的两名影子小丑之一。
     * 供枪械 Mixin 和惩罚事件共同使用，确保 4 秒冷却与不掉枪条件完全一致。
     */
    public static boolean isShowdownJester(ServerPlayerEntity player) {
        if (player == null || !(player.getWorld() instanceof ServerWorld world)) {
            return false;
        }
        return isShowdownJester(world, player);
    }

    /**
     * 判断当前手持物是否是新版谢幕要缩短冷却的左轮。
     */
    public static boolean isShowdownRevolverUser(ServerPlayerEntity player) {
        return isShowdownJester(player)
                && player.getMainHandStack().isOf(WatheItems.REVOLVER);
    }

    private static boolean isShowdownJester(ServerWorld world, ServerPlayerEntity player) {
        ShadowJesterShowdownWorldComponent component =
                ShadowJesterShowdownWorldComponent.KEY.get(world);
        return component.isActive()
                && (player.getUuid().equals(component.getFirstJester())
                || player.getUuid().equals(component.getSecondJester()));
    }

    private static void removeAll(ServerPlayerEntity player, Item item) {
        player.getInventory().remove(stack -> stack.isOf(item), -1, player.getInventory());
    }

    private static void giveIfMissing(ServerPlayerEntity player, Item item) {
        if (!player.getInventory().contains(stack -> stack.isOf(item))) {
            player.giveItemStack(item.getDefaultStack());
        }
    }
}
