package annina.sparkstrength.role.timekeeper;

import annina.sparkstrength.SparkStrengthItems;
import annina.sparkstrength.replay.SparkStrengthReplayFormatters;
import annina.sparkstrength.compat.SparkTraitsCompat;
import annina.sparkstrength.component.demonhunter.DemonHunterSniffPlayerComponent;
import annina.sparkstrength.component.detective.CriminologistPlayerComponent;
import annina.sparkstrength.component.noisemaker.NoisemakerGlowUserComponent;
import annina.sparkstrength.component.phantom.PhantomBackpackUserComponent;
import annina.sparkstrength.component.professor.ProfessorSerumUserComponent;
import annina.sparkstrength.component.reporter.ReporterCommunicationComponent;
import annina.sparkstrength.component.timekeeper.TimekeeperWatchComponent;
import annina.sparkstrength.role.coroner.CoronerService;
import annina.sparkstrength.mixin.minecraft.ItemCooldownManagerAccessor;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.event.KillPlayer;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.Style;
import net.minecraft.text.TextColor;
import net.minecraft.server.world.ServerWorld;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.assassin.AssassinPlayerComponent;
import org.agmas.noellesroles.taotie.TaotiePlayerComponent;

import java.util.ArrayList;
import java.util.List;

/**
 * 计时员怀表的服务端逻辑。
 *
 * <p>所有权限、金币和冷却判断都在这里完成。客户端只负责发送模式切换包，
 * 因此即使客户端伪造模式序号，也无法绕过职业、存活、金币或怀表自身冷却验证。</p>
 */
public final class TimekeeperWatchService {
    private static boolean registered;

    private TimekeeperWatchService() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        // Wathe 的 AFTER 事件只在死亡实际完成后触发，正好对应“变为非存活玩家”。
        KillPlayer.AFTER.register((victim, killer, deathReason) -> rewardAliveTimekeepers(victim));
    }

    public static void switchMode(ServerPlayerEntity player, int modeOrdinal) {
        if (!isUsableTimekeeper(player)) {
            return;
        }

        ItemStack stack = player.getMainHandStack();
        if (!stack.isOf(SparkStrengthItems.dyingWatch())) {
            return;
        }

        TimekeeperWatchMode mode = TimekeeperWatchMode.fromOrdinal(modeOrdinal);
        setMode(stack, mode);
        sendActionbar(player, Text.translatable("message.sparkstrength.dying_watch.mode_changed", mode.text()));
    }

    public static boolean tryUse(net.minecraft.entity.player.PlayerEntity user, ItemStack stack, TimekeeperWatchMode mode) {
        if (!(user instanceof ServerPlayerEntity player)
                || !isUsableTimekeeper(player)
                || !stack.isOf(SparkStrengthItems.dyingWatch())) {
            return false;
        }

        TimekeeperWatchComponent watch = TimekeeperWatchComponent.KEY.get(player);
        boolean itemRefresh = mode == TimekeeperWatchMode.ITEM_REFRESH;
        if (watch.isOnCooldown(itemRefresh)) {
            int ticks = itemRefresh
                    ? watch.getItemRefreshCooldownTicks()
                    : watch.getAbilityRefreshCooldownTicks();
            sendActionbar(player, Text.translatable(
                    "message.sparkstrength.dying_watch.cooldown",
                    (ticks + 19) / 20
            ));
            return false;
        }

        int cost = itemRefresh
                ? TimekeeperConstants.ITEM_REFRESH_COST_COINS
                : TimekeeperConstants.ABILITY_REFRESH_COST_COINS;
        PlayerShopComponent shop = PlayerShopComponent.KEY.get(player);
        if (shop.getBalance() < cost) {
            sendActionbar(player, Text.translatable("message.sparkstrength.dying_watch.not_enough_money", cost));
            return false;
        }

        if (itemRefresh) {
            // 物品刷新也作用于怀表使用者自己；怀表自身的 40 秒计时由独立组件维护，不会被这里误清除。
            clearItemCooldowns(player);
        }

        List<ServerPlayerEntity> targets = findTargets(player);
        for (ServerPlayerEntity target : targets) {
            if (itemRefresh) {
                clearItemCooldowns(target);
                sendActionbar(target, Text.translatable("message.sparkstrength.dying_watch.item_refreshed"));
            } else {
                clearAbilityCooldowns(target);
                sendActionbar(target, Text.translatable("message.sparkstrength.dying_watch.ability_refreshed"));
            }
        }

        shop.addToBalance(-cost);
        watch.startCooldown(itemRefresh);
        sendActionbar(player, Text.translatable("message.sparkstrength.dying_watch.used", cost));
        if (player.getServerWorld() instanceof ServerWorld serverWorld) {
            net.minecraft.nbt.NbtCompound extra = new net.minecraft.nbt.NbtCompound();
            extra.putString("mode", itemRefresh ? "item_refresh" : "ability_refresh");
            GameRecordManager.recordGlobalEvent(
                    serverWorld,
                    SparkStrengthReplayFormatters.TIMEKEEPER_WATCH_USED,
                    player,
                    extra
            );
        }
        return true;
    }

    private static boolean isUsableTimekeeper(ServerPlayerEntity player) {
        GameWorldComponent game = GameWorldComponent.KEY.get(player.getServerWorld());
        boolean realTimekeeper = game.isRole(player, Noellesroles.TIMEKEEPER);
        boolean timekeeperDisguise = CoronerService.hasTimekeeperDisguise(player);
        return game.isRunning()
                && (realTimekeeper || timekeeperDisguise)
                && GameFunctions.isPlayerPlayingAndAlive(player);
    }

    private static void setMode(ItemStack stack, TimekeeperWatchMode mode) {
        stack.set(SparkStrengthItems.TIMEKEEPER_WATCH_MODE, mode.ordinal());
    }

    /**
     * 普通计时员刷新有效好人；如果计时员拥有 impostor，则目标反转为非善良杀手和其它 impostor 好人。
     */
    private static List<ServerPlayerEntity> findTargets(ServerPlayerEntity timekeeper) {
        GameWorldComponent game = GameWorldComponent.KEY.get(timekeeper.getServerWorld());
        boolean reverse = SparkTraitsCompat.hasImpostor(timekeeper);
        List<ServerPlayerEntity> targets = new ArrayList<>();

        for (ServerPlayerEntity target : timekeeper.getServerWorld().getPlayers()) {
            if (target.getUuid().equals(timekeeper.getUuid())
                    || !GameFunctions.isPlayerPlayingAndAlive(target)) {
                continue;
            }

            Role role = game.getRole(target);
            if (role == null) {
                continue;
            }

            if ((reverse && SparkTraitsCompat.isTimekeeperReverseTarget(role, target))
                    || (!reverse && SparkTraitsCompat.isTimekeeperNormalTarget(role, target))) {
                targets.add(target);
            }
        }
        return targets;
    }

    private static void clearItemCooldowns(ServerPlayerEntity player) {
        ItemCooldownManager cooldownManager = player.getItemCooldownManager();
        List<Item> coolingItems = new ArrayList<>(
                ((ItemCooldownManagerAccessor) (Object) cooldownManager).sparkstrength$getEntries().keySet()
        );
        for (Item item : coolingItems) {
            cooldownManager.remove(item);
        }
    }

    private static void clearAbilityCooldowns(ServerPlayerEntity player) {
        // Spark 版 NoellesRoles 的绝大多数主动技能共用这个组件。
        AbilityPlayerComponent.KEY.get(player).setCooldown(0);
        // 刺客和饕餮有独立于通用组件的技能冷却，必须额外清除。
        AssassinPlayerComponent.KEY.get(player).setCooldown(0);
        TaotiePlayerComponent.KEY.get(player).setSwallowCooldown(0);

        // 以下是 SparkStrength 自己增加的专属技能冷却，只清倒计时，不清追踪/标记等业务状态。
        NoisemakerGlowUserComponent.KEY.get(player).setCooldownTicks(0);
        PhantomBackpackUserComponent.KEY.get(player).setCooldownTicks(0);
        ProfessorSerumUserComponent.KEY.get(player).setCooldownTicks(0);
        ReporterCommunicationComponent.KEY.get(player).setCooldownTicks(0);
        CriminologistPlayerComponent.KEY.get(player).clearCriminologistCooldown();
        DemonHunterSniffPlayerComponent.KEY.get(player).clearSniffCooldown();
    }

    private static void rewardAliveTimekeepers(ServerPlayerEntity victim) {
        GameWorldComponent game = GameWorldComponent.KEY.get(victim.getServerWorld());
        for (ServerPlayerEntity player : victim.getServerWorld().getPlayers()) {
            if (player.getUuid().equals(victim.getUuid())
                    || !GameFunctions.isPlayerPlayingAndAlive(player)
                    || (!game.isRole(player, Noellesroles.TIMEKEEPER)
                    && !CoronerService.hasTimekeeperDisguise(player))) {
                continue;
            }
            PlayerShopComponent.KEY.get(player).addToBalance(TimekeeperConstants.DEATH_REWARD_COINS);
        }
    }

    public static void sendActionbar(ServerPlayerEntity player, Text text) {
        player.sendMessage(text.copy().setStyle(Style.EMPTY.withColor(TextColor.fromRgb(TimekeeperConstants.ROLE_COLOR))), true);
    }
}
