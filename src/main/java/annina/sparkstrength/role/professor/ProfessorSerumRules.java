package annina.sparkstrength.role.professor;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.game.GameConstants;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * 教授增强的纯规则集中处。
 *
 * <p>所有数值都放在这里，后续调时长、价格、冷却、透视颜色和优先级时，
 * 不需要去翻服务端逻辑、UI 逻辑或 mixin 注入点。</p>
 */
public final class ProfessorSerumRules {
    public static final Identifier PROFESSOR_ID = Identifier.of("noellesroles", "professor");

    public static final double FEED_RANGE = 1.5D;
    public static final int REMOTE_SUCCESS_COOLDOWN_TICKS = GameConstants.getInTicks(1, 30);
    public static final int REMOTE_FAIL_COOLDOWN_TICKS = GameConstants.getInTicks(0, 40);

    public static final int INVISIBILITY_DURATION_TICKS = GameConstants.getInTicks(0, 15);
    public static final int DOORPASSING_DURATION_TICKS = GameConstants.getInTicks(0, 20);
    public static final int SEDATIVE_DURATION_TICKS = GameConstants.getInTicks(0, 45);

    public static final int SEDATIVE_PRICE = 50;
    public static final int DOORPASSING_PRICE = 75;
    public static final int INVISIBILITY_PRICE = 125;
    public static final int TRUTH_PRICE = 125;
    public static final int REFRESH_COOLDOWN_PRICE = 50;

    public static final String SEDATIVE_ENTRY_ID = "sparkstrength_professor_sedative";
    public static final String DOORPASSING_ENTRY_ID = "sparkstrength_professor_doorpassing_potion";
    public static final String INVISIBILITY_ENTRY_ID = "sparkstrength_professor_invisibility_serum";
    public static final String TRUTH_ENTRY_ID = "sparkstrength_professor_truth_serum";
    public static final String REFRESH_COOLDOWN_ENTRY_ID = "sparkstrength_professor_refresh_cooldown";

    /** 试剂本能高亮要压过 NoellesRoles 普通职业高亮和 Wathe 默认杀手本能。 */
    public static final int SERUM_HIGHLIGHT_PRIORITY = 250;
    /** 隐身试剂的跳过结果必须高于其它 HIGH 监听器，避免同优先级时被先注册监听器占住。 */
    public static final int INVISIBILITY_SKIP_PRIORITY = 300;

    public static final int INVISIBILITY_HIGHLIGHT_COLOR = 0x7FD8FF;
    public static final int DOORPASSING_HIGHLIGHT_COLOR = 0x4B0082;
    public static final int SEDATIVE_HIGHLIGHT_COLOR = 0xFFD84A;

    private ProfessorSerumRules() {
    }

    public static boolean isProfessor(@Nullable Role role) {
        return role != null && PROFESSOR_ID.equals(role.identifier());
    }

    public static int durationTicks(ProfessorSerumType type) {
        return switch (type) {
            case INVISIBILITY -> INVISIBILITY_DURATION_TICKS;
            case DOORPASSING -> DOORPASSING_DURATION_TICKS;
            case SEDATIVE -> SEDATIVE_DURATION_TICKS;
            case TRUTH -> 0;
        };
    }

    public static int price(ProfessorSerumType type) {
        return switch (type) {
            case SEDATIVE -> SEDATIVE_PRICE;
            case DOORPASSING -> DOORPASSING_PRICE;
            case INVISIBILITY -> INVISIBILITY_PRICE;
            case TRUTH -> TRUTH_PRICE;
        };
    }

    public static String shopEntryId(ProfessorSerumType type) {
        return switch (type) {
            case SEDATIVE -> SEDATIVE_ENTRY_ID;
            case DOORPASSING -> DOORPASSING_ENTRY_ID;
            case INVISIBILITY -> INVISIBILITY_ENTRY_ID;
            case TRUTH -> TRUTH_ENTRY_ID;
        };
    }
}
