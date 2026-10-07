package annina.sparkstrength.role.economy;

import annina.sparkstrength.role.detective.CriminologistRules;
import annina.sparkstrength.role.coroner.CoronerRules;
import annina.sparkstrength.role.engineer.EngineerRules;
import annina.sparkstrength.role.professor.ProfessorSerumRules;
import annina.sparkstrength.role.toxicologist.ToxicologistCapsuleRules;
import annina.sparkstrength.role.veteran.VeteranRules;
import annina.sparkstrength.tablet.TabletShopRules;
import dev.doctor4t.wathe.api.Role;
import org.jetbrains.annotations.Nullable;

/**
 * Economy rules shared by the NoellesRoles enhancements.
 * NoellesRoles 增强共享的金币经济规则。
 *
 * <p>金币不是某一个道具或技能的内部状态，所以集中在 economy 包里；
 * 但它只引用各角色自己的规则类，不再把所有角色常量塞回一个 Noelles 总规则类。</p>
 */
public final class RoleEconomyRules {
    public static final int INITIAL_GOOD_ROLE_MONEY = 0;
    public static final int TASK_MONEY_REWARD = 50;

    private RoleEconomyRules() {
    }

    public static boolean isGoodMoneyRole(@Nullable Role role) {
        return CriminologistRules.isDetective(role)
                || CoronerRules.isCoroner(role)
                || ToxicologistCapsuleRules.isToxicologist(role)
                || ProfessorSerumRules.isProfessor(role)
                || EngineerRules.isEngineer(role)
                || VeteranRules.isVeteran(role)
                || TabletShopRules.canBuyTabletRole(role);
    }

    public static boolean shouldInitializeGoodMoney(@Nullable Role role) {
        return isGoodMoneyRole(role);
    }

    public static boolean earnsTaskMoney(@Nullable Role role) {
        return isGoodMoneyRole(role);
    }
}
