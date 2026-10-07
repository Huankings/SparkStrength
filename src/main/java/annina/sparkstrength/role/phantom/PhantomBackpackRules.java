package annina.sparkstrength.role.phantom;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.game.GameConstants;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * 幽灵背包隐身加强的稳定数值与职业判定。
 *
 * <p>这里的冷却不读取 NoellesRoles 的 {@code AbilityPlayerComponent}，
 * 因为用户确认“幽灵原按键隐身”和“背包隐身其他玩家”必须是两套独立冷却。</p>
 */
public final class PhantomBackpackRules {
    public static final Identifier PHANTOM_ID = Identifier.of("noellesroles", "phantom");

    /** 背包按钮给目标上的隐身持续时间：30 秒，和旧版幽灵自身隐身一致。 */
    public static final int INVISIBILITY_DURATION_TICKS = GameConstants.getInTicks(0, 30);
    /** 背包按钮独立冷却：1 分 30 秒。 */
    public static final int COOLDOWN_TICKS = GameConstants.getInTicks(1, 30);

    private PhantomBackpackRules() {
    }

    public static boolean isPhantom(@Nullable Role role) {
        return role != null && PHANTOM_ID.equals(role.identifier());
    }
}
