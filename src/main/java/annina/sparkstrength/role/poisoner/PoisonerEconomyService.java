package annina.sparkstrength.role.poisoner;

import dev.doctor4t.wathe.api.event.PlayerPoisoned;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerPoisonComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 毒师投毒成功后的金币奖励。
 *
 * <p>奖励挂在 Wathe 的 {@link PlayerPoisoned} 事件上，而不是分别改毒针、毒气、
 * 毒药瓶、蝎子等道具代码。这样所有最终走 {@code setPoisonTicks(...)} 的投毒来源
 * 都能统一生效，同时也不会漏掉后续新增的标准投毒入口。</p>
 */
public final class PoisonerEconomyService {
    private static final Map<UUID, PendingPoisonReward> PENDING_REWARDS = new HashMap<>();
    private static boolean registered;

    private PoisonerEconomyService() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        PlayerPoisoned.BEFORE.register(PoisonerEconomyService::beforePlayerPoisoned);
        PlayerPoisoned.AFTER.register(PoisonerEconomyService::afterPlayerPoisoned);
    }

    private static @Nullable PlayerPoisoned.PoisonResult beforePlayerPoisoned(
            PlayerEntity target,
            int ticks,
            UUID poisoner
    ) {
        // 每次进入投毒流程都先移除目标的旧记录，避免“上一次被取消的投毒”污染下一次判断。
        PENDING_REWARDS.remove(target.getUuid());

        if (!(target instanceof ServerPlayerEntity) || poisoner == null || ticks <= 0) {
            return null;
        }

        // 这里定义“每中毒一次”的口径：
        // 只有目标在本次投毒前没有正在生效的毒，才认为是一次新的中毒。
        // 如果毒气/毒针只是刷新或加速一个已经中毒的目标，就不记录待发奖。
        PlayerPoisonComponent poisonComponent = PlayerPoisonComponent.KEY.get(target);
        if (poisonComponent.poisonTicks > 0) {
            return null;
        }

        // 奖励只统计当前游戏里的全局存活玩家；旁观、创造、已标记死亡或未分配角色都不算。
        if (!isAliveGamePlayer(target)) {
            return null;
        }

        // BEFORE 与 AFTER 在同一次 setPoisonTicks 调用中紧挨着触发。
        // 记录 worldTime 与 poisoner，用于 AFTER 阶段确认这条记录确实属于同一次投毒。
        PENDING_REWARDS.put(target.getUuid(), new PendingPoisonReward(poisoner, target.getWorld().getTime()));
        return null;
    }

    private static void afterPlayerPoisoned(PlayerEntity target, int ticks, UUID poisoner) {
        if (!(target instanceof ServerPlayerEntity serverTarget) || poisoner == null || ticks <= 0) {
            return;
        }

        PendingPoisonReward pending = PENDING_REWARDS.remove(target.getUuid());
        if (pending == null) {
            return;
        }

        // 如果 AFTER 阶段拿到的投毒者或 tick 与 BEFORE 不一致，就说明不是同一次投毒，不发奖。
        if (!pending.poisoner().equals(poisoner) || pending.worldTime() != target.getWorld().getTime()) {
            return;
        }

        // AFTER 已经代表毒状态成功写入；这里再检查一次目标仍处于有效游戏存活状态，防止边界事件误发。
        if (!isAliveGamePlayer(target)) {
            return;
        }

        ServerPlayerEntity poisonerPlayer = serverTarget.getServer().getPlayerManager().getPlayer(poisoner);
        if (!isRewardablePoisoner(serverTarget, poisonerPlayer)) {
            return;
        }

        PlayerShopComponent.KEY.get(poisonerPlayer).addToBalance(PoisonerEconomyRules.POISON_REWARD);
    }

    private static boolean isAliveGamePlayer(PlayerEntity player) {
        return GameFunctions.isPlayerAliveAndSurvival(player)
                && GameFunctions.isPlayerPlayingAndAlive(player);
    }

    private static boolean isRewardablePoisoner(
            ServerPlayerEntity target,
            @Nullable ServerPlayerEntity poisoner
    ) {
        if (poisoner == null || !isAliveGamePlayer(poisoner)) {
            return false;
        }

        GameWorldComponent game = GameWorldComponent.KEY.get(target.getWorld());
        if (!game.hasAnyRole(poisoner.getUuid()) || game.isPlayerDead(poisoner.getUuid())) {
            return false;
        }

        // 用目标所在世界的 GameWorldComponent 判定角色，确保奖励属于同一局游戏里的毒师身份。
        return PoisonerEconomyRules.isPoisoner(game.getRole(poisoner.getUuid()));
    }

    private record PendingPoisonReward(UUID poisoner, long worldTime) {
    }
}
