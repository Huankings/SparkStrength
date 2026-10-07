package annina.sparkstrength.role.toxicologist;

import annina.sparkstrength.component.toxicologist.ToxicologistAntidoteComponent;
import annina.sparkstrength.mixin.minecraft.ItemCooldownEntryAccessor;
import annina.sparkstrength.mixin.minecraft.ItemCooldownManagerAccessor;
import annina.sparkstrength.role.coroner.CoronerService;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.event.PlayerPoisoned;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerPoisonComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.agmas.noellesroles.ModItems;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 毒理学家解毒剂加强服务。
 *
 * <p>这里统一处理三件事：
 * 1. 玩家从无毒状态进入中毒状态时，给所有存活毒理学家累计 120 秒解毒剂冷却抵扣；
 * 2. 毒理学家成功治疗中毒玩家后获得 25 金币；
 * 3. 抵扣可以在无冷却时积攒，在解毒剂进入冷却后自动消费，剩余抵扣继续保留到下次。</p>
 */
public final class ToxicologistAntidoteService {
    private static final Map<UUID, PendingPoisonCount> PENDING_POISON_COUNTS = new HashMap<>();
    private static boolean registered;

    private ToxicologistAntidoteService() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        PlayerPoisoned.BEFORE.register(ToxicologistAntidoteService::beforePlayerPoisoned);
        PlayerPoisoned.AFTER.register(ToxicologistAntidoteService::afterPlayerPoisoned);
    }

    private static @Nullable PlayerPoisoned.PoisonResult beforePlayerPoisoned(
            PlayerEntity target,
            int ticks,
            UUID poisoner
    ) {
        // 每次投毒流程开始都清理旧记录，避免取消/失败的投毒污染下一次成功投毒。
        PENDING_POISON_COUNTS.remove(target.getUuid());

        if (!(target instanceof ServerPlayerEntity) || ticks <= 0 || !isAliveGamePlayer(target)) {
            return null;
        }

        PlayerPoisonComponent poisonComponent = PlayerPoisonComponent.KEY.get(target);
        if (poisonComponent.poisonTicks > 0) {
            // 需求确认：只统计“从未中毒/已清毒 -> 中毒”的新中毒，刷新或加速已有中毒不累计。
            return null;
        }

        PENDING_POISON_COUNTS.put(target.getUuid(), new PendingPoisonCount(target.getWorld().getTime()));
        return null;
    }

    private static void afterPlayerPoisoned(PlayerEntity target, int ticks, UUID poisoner) {
        if (!(target instanceof ServerPlayerEntity serverTarget) || ticks <= 0) {
            return;
        }

        PendingPoisonCount pending = PENDING_POISON_COUNTS.remove(target.getUuid());
        if (pending == null || pending.worldTime() != target.getWorld().getTime()) {
            return;
        }

        if (!isAliveGamePlayer(target)) {
            return;
        }

        grantReductionToAliveToxicologists(serverTarget.getServerWorld());
    }

    /**
     * 解毒剂成功治疗中毒玩家后由 mixin 调用。
     */
    public static void afterAntidoteCure(ServerPlayerEntity toxicologist) {
        if (!isRewardableToxicologist(toxicologist)) {
            return;
        }

        PlayerShopComponent.KEY.get(toxicologist).addToBalance(ToxicologistCapsuleRules.ANTIDOTE_CURE_REWARD);
        applyStoredReduction(toxicologist);
    }

    /**
     * 商店“解毒剂冷却刷新”的点击生效逻辑。
     *
     * @return true 表示确实刷新了冷却，Wathe 才会扣金币；无冷却时返回 false，避免误扣。
     */
    public static boolean refreshAntidoteCooldown(PlayerEntity buyer) {
        if (!(buyer instanceof ServerPlayerEntity serverBuyer) || !isRewardableToxicologist(serverBuyer)) {
            return false;
        }

        Item antidote = ModItems.ANTIDOTE;
        ItemCooldownManager cooldownManager = serverBuyer.getItemCooldownManager();
        if (!cooldownManager.isCoolingDown(antidote)) {
            return false;
        }

        cooldownManager.remove(antidote);
        return true;
    }

    public static void clearPlayer(ServerPlayerEntity player) {
        ToxicologistAntidoteComponent.KEY.get(player).clear();
    }

    private static void grantReductionToAliveToxicologists(ServerWorld world) {
        GameWorldComponent game = GameWorldComponent.KEY.get(world);
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (!isAliveGamePlayer(player)
                    || !(ToxicologistCapsuleRules.isToxicologist(game.getRole(player))
                    || CoronerService.hasToxicologistDisguise(player))) {
                continue;
            }

            ToxicologistAntidoteComponent.KEY.get(player).addStoredReductionTicks(
                    ToxicologistCapsuleRules.ANTIDOTE_REDUCTION_PER_NEW_POISON_TICKS
            );
            applyStoredReduction(player);
        }
    }

    private static boolean isRewardableToxicologist(ServerPlayerEntity player) {
        if (!isAliveGamePlayer(player)) {
            return false;
        }

        GameWorldComponent game = GameWorldComponent.KEY.get(player.getWorld());
        Role role = game.getRole(player);
        return ToxicologistCapsuleRules.isToxicologist(role) || CoronerService.hasToxicologistDisguise(player);
    }

    private static boolean isAliveGamePlayer(PlayerEntity player) {
        return GameFunctions.isPlayerAliveAndSurvival(player)
                && GameFunctions.isPlayerPlayingAndAlive(player);
    }

    private static void applyStoredReduction(ServerPlayerEntity toxicologist) {
        ToxicologistAntidoteComponent component = ToxicologistAntidoteComponent.KEY.get(toxicologist);
        if (!component.hasStoredReduction()) {
            return;
        }

        Item antidote = ModItems.ANTIDOTE;
        ItemCooldownManager cooldownManager = toxicologist.getItemCooldownManager();
        int remainingCooldownTicks = remainingCooldownTicks(cooldownManager, antidote);
        if (remainingCooldownTicks <= 0) {
            // 当前没有冷却时不消费抵扣，等待下一次解毒剂成功使用后再自动生效。
            return;
        }

        int reductionTicks = component.getStoredReductionTicks();
        if (reductionTicks >= remainingCooldownTicks) {
            cooldownManager.remove(antidote);
            component.setStoredReductionTicks(reductionTicks - remainingCooldownTicks);
            return;
        }

        cooldownManager.set(antidote, remainingCooldownTicks - reductionTicks);
        component.clear();
    }

    private static int remainingCooldownTicks(ItemCooldownManager cooldownManager, Item item) {
        Object entry = ((ItemCooldownManagerAccessor) cooldownManager).sparkstrength$getEntries().get(item);
        if (entry == null) {
            return 0;
        }

        int endTick = ((ItemCooldownEntryAccessor) entry).sparkstrength$getEndTick();
        int currentTick = ((ItemCooldownManagerAccessor) cooldownManager).sparkstrength$getTick();
        return Math.max(0, endTick - currentTick);
    }

    private record PendingPoisonCount(long worldTime) {
    }
}
