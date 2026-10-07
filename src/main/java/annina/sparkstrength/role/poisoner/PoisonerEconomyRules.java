package annina.sparkstrength.role.poisoner;

import dev.doctor4t.wathe.api.Role;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * 毒师金币增强的纯规则集中处。
 *
 * <p>把奖励数值和角色 ID 放在这里，后续如果想调整“每次成功中毒奖励多少金币”，
 * 只需要改 {@link #POISON_REWARD}，不用进入事件监听逻辑里找硬编码。</p>
 */
public final class PoisonerEconomyRules {
    public static final Identifier POISONER_ID = Identifier.of("noellesroles", "poisoner");

    /** 目标从未中毒/已清毒状态成功进入中毒状态时，毒师获得的金币。 */
    public static final int POISON_REWARD = 75;

    private PoisonerEconomyRules() {
    }

    public static boolean isPoisoner(@Nullable Role role) {
        return role != null && POISONER_ID.equals(role.identifier());
    }
}
