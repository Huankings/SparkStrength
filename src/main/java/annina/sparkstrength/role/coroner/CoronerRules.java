package annina.sparkstrength.role.coroner;

import dev.doctor4t.wathe.api.Faction;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.game.GameConstants;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * 验尸官增强的稳定规则与数值配置。
 *
 * <p>这里集中保存职业 ID、商品价格、金币收益和“尸体身份 -> 临时能力”的判定。
 * 服务端、客户端渲染和 mixin 都只读这一层，避免同一批职业 ID 散落到多个文件里。</p>
 */
public final class CoronerRules {
    public static final Identifier CORONER_ID = Identifier.of("noellesroles", "coroner");
    public static final Identifier CORONER_BODY_BAG_ID = Identifier.of("sparkstrength", "coroner_body_bag");

    public static final Identifier WATHE_NO_ROLE_ID = Identifier.of("wathe", "no_role");
    public static final Identifier WATHE_VETERAN_ID = Identifier.of("wathe", "veteran");
    public static final Identifier WATHE_VIGILANTE_ID = Identifier.of("wathe", "vigilante");
    public static final Identifier NOELLES_CONDUCTOR_ID = Identifier.of("noellesroles", "conductor");
    public static final Identifier NOELLES_WAITER_ID = Identifier.of("noellesroles", "waiter");
    public static final Identifier NOELLES_CORRUPT_COP_ID = Identifier.of("noellesroles", "corrupt_cop");
    public static final Identifier NOELLES_SCAVENGER_ID = Identifier.of("noellesroles", "scavenger");
    public static final Identifier NOELLES_POISONER_ID = Identifier.of("noellesroles", "poisoner");
    public static final Identifier NOELLES_TOXICOLOGIST_ID = Identifier.of("noellesroles", "toxicologist");
    public static final Identifier NOELLES_PROFESSOR_ID = Identifier.of("noellesroles", "professor");
    public static final Identifier NOELLES_REPORTER_ID = Identifier.of("noellesroles", "reporter");
    public static final Identifier NOELLES_ATTENDANT_ID = Identifier.of("noellesroles", "attendant");
    public static final Identifier NOELLES_SURVIVAL_MASTER_ID = Identifier.of("noellesroles", "survival_master");
    public static final Identifier NOELLES_TIMEKEEPER_ID = Identifier.of("noellesroles", "time_keeper");
    public static final Identifier NOELLES_UNDERCOVER_ID = Identifier.of("noellesroles", "undercover");
    public static final Identifier NOELLES_NOISEMAKER_ID = Identifier.of("noellesroles", "noisemaker");
    public static final Identifier NOELLES_SILENCER_ID = Identifier.of("noellesroles", "silencer");
    public static final Identifier NOELLES_RECALLER_ID = Identifier.of("noellesroles", "recaller");
    public static final Identifier NOELLES_BARTENDER_ID = Identifier.of("noellesroles", "bartender");
    public static final Identifier NOELLES_DEMON_HUNTER_ID = Identifier.of("noellesroles", "demon_hunter");
    public static final Identifier NOELLES_ENGINEER_ID = Identifier.of("noellesroles", "engineer");
    public static final Identifier NOELLES_BOMBER_ID = Identifier.of("noellesroles", "bomber");
    public static final Identifier SPARKWITCH_KIDNAPPER_ID = Identifier.of("sparkwitch", "kidnapper");

    /** 验尸官商店：采尸袋条目 ID。 */
    public static final String BODY_BAG_ENTRY_ID = "sparkstrength_coroner_body_bag";
    /** 采尸袋价格。 */
    public static final int BODY_BAG_PRICE = 100;

    /** 验尸官靠近每具尸体时获得的金币。 */
    public static final int BODY_PROXIMITY_REWARD = 50;
    /** 靠近尸体领奖距离，单位为格。 */
    public static final double BODY_PROXIMITY_RANGE = 2.0D;
    public static final double BODY_PROXIMITY_RANGE_SQUARED =
            BODY_PROXIMITY_RANGE * BODY_PROXIMITY_RANGE;

    /** 验尸官被动收入间隔：完全照搬回溯者，每 10 秒结算一次。 */
    public static final int PASSIVE_INCOME_INTERVAL_TICKS = GameConstants.getInTicks(0, 10);
    /** 验尸官每次被动收入结算获得的金币：完全照搬回溯者。 */
    public static final int PASSIVE_INCOME_AMOUNT = 5;
    /** 被动收入余额上限：-1 表示无上限，完全照搬回溯者。 */
    public static final int PASSIVE_INCOME_BALANCE_CAP = -1;

    private CoronerRules() {
    }

    public static boolean isCoroner(@Nullable Role role) {
        return role != null && CORONER_ID.equals(role.identifier());
    }

    public static boolean canUseCoronerShop(@Nullable Role role) {
        return isCoroner(role);
    }

    public static int passiveIncomeToAdd(int currentBalance) {
        if (PASSIVE_INCOME_AMOUNT <= 0) {
            return 0;
        }
        if (PASSIVE_INCOME_BALANCE_CAP < 0) {
            return PASSIVE_INCOME_AMOUNT;
        }
        return Math.max(0, Math.min(PASSIVE_INCOME_AMOUNT, PASSIVE_INCOME_BALANCE_CAP - currentBalance));
    }

    public static @Nullable Role resolveRole(@Nullable Identifier roleId) {
        if (roleId == null || WATHE_NO_ROLE_ID.equals(roleId)) {
            return null;
        }
        return WatheRoles.getRole(roleId);
    }

    public static boolean isKillerFaction(@Nullable Role role) {
        return role != null && role.getFaction() == Faction.KILLER;
    }

    public static boolean isVeteran(@Nullable Role role) {
        return hasRoleId(role, WATHE_VETERAN_ID);
    }

    public static boolean isVigilante(@Nullable Role role) {
        return hasRoleId(role, WATHE_VIGILANTE_ID);
    }

    public static boolean isConductor(@Nullable Role role) {
        return hasRoleId(role, NOELLES_CONDUCTOR_ID);
    }

    public static boolean isWaiter(@Nullable Role role) {
        return hasRoleId(role, NOELLES_WAITER_ID);
    }

    public static boolean isCorruptCop(@Nullable Role role) {
        return hasRoleId(role, NOELLES_CORRUPT_COP_ID);
    }

    public static boolean isScavenger(@Nullable Role role) {
        return hasRoleId(role, NOELLES_SCAVENGER_ID);
    }

    public static boolean isPoisoner(@Nullable Role role) {
        return hasRoleId(role, NOELLES_POISONER_ID);
    }

    public static boolean isToxicologist(@Nullable Role role) {
        return hasRoleId(role, NOELLES_TOXICOLOGIST_ID);
    }

    public static boolean isProfessor(@Nullable Role role) {
        return hasRoleId(role, NOELLES_PROFESSOR_ID);
    }

    public static boolean isReporter(@Nullable Role role) {
        return hasRoleId(role, NOELLES_REPORTER_ID);
    }

    public static boolean isAttendant(@Nullable Role role) {
        return hasRoleId(role, NOELLES_ATTENDANT_ID);
    }

    public static boolean isSurvivalMaster(@Nullable Role role) {
        return hasRoleId(role, NOELLES_SURVIVAL_MASTER_ID);
    }

    public static boolean isTimekeeper(@Nullable Role role) {
        return hasRoleId(role, NOELLES_TIMEKEEPER_ID);
    }

    public static boolean isUndercover(@Nullable Role role) {
        return hasRoleId(role, NOELLES_UNDERCOVER_ID);
    }

    public static boolean isNoisemaker(@Nullable Role role) {
        return hasRoleId(role, NOELLES_NOISEMAKER_ID);
    }

    public static boolean isSilencer(@Nullable Role role) {
        return hasRoleId(role, NOELLES_SILENCER_ID);
    }

    public static boolean isRecaller(@Nullable Role role) {
        return hasRoleId(role, NOELLES_RECALLER_ID);
    }

    public static boolean isBartender(@Nullable Role role) {
        return hasRoleId(role, NOELLES_BARTENDER_ID);
    }

    public static boolean isDemonHunter(@Nullable Role role) {
        return hasRoleId(role, NOELLES_DEMON_HUNTER_ID);
    }

    public static boolean isEngineer(@Nullable Role role) {
        return hasRoleId(role, NOELLES_ENGINEER_ID);
    }

    public static boolean isBomber(@Nullable Role role) {
        return hasRoleId(role, NOELLES_BOMBER_ID);
    }

    public static boolean isSparkWitchKidnapper(@Nullable Role role) {
        return hasRoleId(role, SPARKWITCH_KIDNAPPER_ID);
    }

    public static boolean grantsDagger(@Nullable Role role) {
        /*
         * 需求：杀手阵营尸体默认给匕首，老兵尸体也给匕首。
         * 毒师是杀手阵营里的特例：尸体身份应发毒针，不再按杀手阵营默认发匕首。
         */
        return (isKillerFaction(role) && !isPoisoner(role) && !isBomber(role)) || isVeteran(role);
    }

    public static boolean grantsRevolver(@Nullable Role role) {
        // 需求：义警和黑警尸体给左轮。黑警额外按中立尸体处理，不给匕首。
        return isVigilante(role) || isCorruptCop(role);
    }

    public static boolean grantsConductorMasterKey(@Nullable Role role) {
        return isConductor(role);
    }

    public static boolean grantsNeutralMasterKey(@Nullable Role role) {
        // 用户确认：中立万能钥匙给所有 role.isNeutral() 的尸体身份。
        return role != null && role.isNeutral();
    }

    public static boolean grantsWaiterDoublePickup(@Nullable Role role) {
        return isWaiter(role);
    }

    public static boolean grantsPoisonNeedle(@Nullable Role role) {
        return isPoisoner(role);
    }

    public static boolean grantsTimedBomb(@Nullable Role role) {
        return isBomber(role);
    }

    public static boolean grantsInstantSilentKnife(@Nullable Role role) {
        /*
         * 验尸官从老兵/清道夫尸体身份借来的匕首都走无蓄力、无举刀声、无刺杀声路径。
         * 老兵是原本需求，清道夫是用户后续确认的杀手阵营特例。
         */
        return isVeteran(role) || isScavenger(role);
    }

    private static boolean hasRoleId(@Nullable Role role, Identifier roleId) {
        return role != null && roleId.equals(role.identifier());
    }
}
