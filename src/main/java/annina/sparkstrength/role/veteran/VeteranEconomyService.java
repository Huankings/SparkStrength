package annina.sparkstrength.role.veteran;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.event.KillPlayer;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.game.GameConstants;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 老兵刀人后的额外金币奖励。
 *
 * <p>奖励挂在 {@code KillPlayer.AFTER} 上：只有死亡流程真正完成后才发钱，
 * 防止疯魔盾、死亡取消等情况也给老兵奖励。</p>
 */
public final class VeteranEconomyService {
    /**
     * 死亡前保存的老兵刀击杀上下文。
     *
     * <p>SparkTraits 会在 KillPlayer.AFTER 中清理死者的当前词条，
     * 所以 AFTER 阶段不能再通过 hasActiveTrait 反推有效阵营。
     * 这里按死者 UUID 暂存死亡前的有效好人判定，等确认死亡后再结算金币。</p>
     */
    private static final Map<UUID, PendingKnifeKill> PENDING_KILLS = new HashMap<>();

    private VeteranEconomyService() {
    }

    /** 注册老兵刀收益所需的死亡前、死亡后两个事件阶段。 */
    public static void register() {
        KillPlayer.BEFORE.register(VeteranEconomyService::beforeKill);
        KillPlayer.AFTER.register(VeteranEconomyService::afterKill);
    }

    /**
     * 在目标死亡前保存有效阵营。
     *
     * <p>只记录老兵作为直接击杀者、死因为 knife 的情况，避免影响其它职业或其它死因。
     * 返回 null 表示不拦截 Wathe 原本的死亡流程。</p>
     */
    private static @Nullable KillPlayer.KillResult beforeKill(
            ServerPlayerEntity victim,
            @Nullable ServerPlayerEntity killer,
            Identifier deathReason
    ) {
        if (killer == null || !GameConstants.DeathReasons.KNIFE.equals(deathReason)) {
            return null;
        }

        GameWorldComponent game = GameWorldComponent.KEY.get(killer.getWorld());
        if (!VeteranRules.isVeteran(game.getRole(killer))) {
            return null;
        }

        // 必须在死亡流程开始前读取词条；死亡后的 AFTER 监听器会清空 active traits。
        boolean victimWasEffectiveCivilian =
                VeteranRules.isEffectiveCivilian(game.getRole(victim), victim);
        PENDING_KILLS.put(
                victim.getUuid(),
                new PendingKnifeKill(killer.getUuid(), victimWasEffectiveCivilian)
        );
        return null;
    }

    public static void afterKill(
            ServerPlayerEntity victim,
            @Nullable ServerPlayerEntity killer,
            Identifier deathReason
    ) {
        PendingKnifeKill pending = PENDING_KILLS.remove(victim.getUuid());
        if (killer == null || !GameConstants.DeathReasons.KNIFE.equals(deathReason)) {
            return;
        }

        GameWorldComponent game = GameWorldComponent.KEY.get(killer.getWorld());
        Role killerRole = game.getRole(killer);
        if (!VeteranRules.isVeteran(killerRole)) {
            return;
        }

        Role victimRole = game.getRole(victim);
        // 正常路径使用死亡前快照；没有快照时保留兼容回退，避免外部直接调用 killPlayer 导致异常。
        boolean victimWasEffectiveCivilian = pending != null && killer.getUuid().equals(pending.killerUuid())
                ? pending.victimWasEffectiveCivilian()
                : VeteranRules.isEffectiveCivilian(victimRole, victim);
        int reward = victimWasEffectiveCivilian
                ? VeteranRules.INNOCENT_KILL_REWARD
                : VeteranRules.NON_INNOCENT_KILL_REWARD;
        PlayerShopComponent.KEY.get(killer).addToBalance(reward);
    }

    /** 清理回合结束时尚未被消费的快照，防止状态泄漏到下一回合。 */
    public static void clearRoundState() {
        PENDING_KILLS.clear();
    }

    private record PendingKnifeKill(UUID killerUuid, boolean victimWasEffectiveCivilian) {
    }
}
