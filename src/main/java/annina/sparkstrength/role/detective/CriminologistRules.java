package annina.sparkstrength.role.detective;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.game.GameConstants;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * Pure rules for the Detective's SparkStrength criminologist skill.
 * 侦探“犯罪学家”第二技能的纯规则。
 *
 * <p>SparkStrength 明确依赖 NoellesRoles，所以这里不是软依赖；
 * 仍使用稳定角色 ID 判断，是为了让测试和规则计算不需要初始化 NoellesRoles 的大入口类。</p>
 */
public final class CriminologistRules {
    public static final Identifier DETECTIVE_ID = Identifier.of("noellesroles", "detective");

    public static final int COST = 150;
    public static final int INITIAL_COOLDOWN_TICKS = GameConstants.getInTicks(1, 0);
    public static final int COOLDOWN_TICKS = GameConstants.getInTicks(2, 0);
    public static final int REVEAL_INTERVAL_TICKS = GameConstants.getInTicks(0, 30);
    public static final int REVEAL_TICKS = GameConstants.getInTicks(0, 5);
    public static final int HIGHLIGHT_COLOR = 0xFF3030;

    private CriminologistRules() {
    }

    public static boolean isDetective(@Nullable Role role) {
        return role != null && DETECTIVE_ID.equals(role.identifier());
    }
}
