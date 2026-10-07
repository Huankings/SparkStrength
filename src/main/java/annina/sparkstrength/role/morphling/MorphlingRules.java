package annina.sparkstrength.role.morphling;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.game.GameConstants;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * 变形怪增强的集中规则表。
 *
 * <p>所有会影响玩法手感或金币收益的数值都放在这里，后续如果要微调售价、
 * 变形时间、奖励金币或本能高亮优先级，不需要再翻服务端逻辑和客户端渲染逻辑。</p>
 */
public final class MorphlingRules {
    public static final Identifier MORPHLING_ID = Identifier.of("noellesroles", "morphling");

    public static final double REAGENT_TARGET_RANGE = 2.5D;
    public static final int REAGENT_ACTIVE_DURATION_TICKS = GameConstants.getInTicks(0, 60);
    public static final int MORPH_REAGENT_PRICE = 10;

    public static final int SELF_MORPH_KILL_REWARD = 30;
    public static final int SELF_MORPH_TARGET_KILL_REWARD = 60;
    public static final int OTHER_MARK_EVENT_REWARD = 50;

    public static final String MORPH_REAGENT_ENTRY_ID = "sparkstrength_morph_reagent";
    public static final int MARK_HIGHLIGHT_PRIORITY = 240;

    private MorphlingRules() {
    }

    public static boolean isMorphling(@Nullable Role role) {
        return role != null && MORPHLING_ID.equals(role.identifier());
    }
}
