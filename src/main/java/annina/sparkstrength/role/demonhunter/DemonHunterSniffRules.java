package annina.sparkstrength.role.demonhunter;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.util.ShopEntry;
import dev.doctor4t.wathe.util.ShopUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * 猎魔人“嗅探”能力的纯规则和可调数值。
 *
 * <p>这里不保存任何运行态，只放职业 ID、半径、冷却、透视时间、颜色等常量，
 * 以及“这个目标是否属于可嗅探疯魔玩家”的统一判定。后续如果要调数值，
 * 优先改这个类，不需要钻进服务端逻辑里逐行找魔法数字。</p>
 */
public final class DemonHunterSniffRules {
    public static final Identifier DEMON_HUNTER_ID = Identifier.of("noellesroles", "demon_hunter");
    public static final Identifier JESTER_ID = Identifier.of("noellesroles", "jester");

    public static final String PSYCHO_MODE_ENTRY_ID = "psycho_mode";
    public static final double SNIFF_RADIUS = 5.5D;
    public static final double SNIFF_RADIUS_SQUARED = SNIFF_RADIUS * SNIFF_RADIUS;
    public static final int INITIAL_COOLDOWN_TICKS = GameConstants.getInTicks(1, 0);
    public static final int COOLDOWN_TICKS = GameConstants.getInTicks(1, 30);
    public static final int REVEAL_TICKS = GameConstants.getInTicks(0, 20);
    public static final int HIGHLIGHT_COLOR = 0xC70000;
    public static final int HIGHLIGHT_PRIORITY = -100;

    private DemonHunterSniffRules() {
    }

    public static boolean isDemonHunter(@Nullable Role role) {
        return role != null && DEMON_HUNTER_ID.equals(role.identifier());
    }

    public static boolean isSniffableFrenzyCandidate(PlayerEntity target, @Nullable Role role) {
        if (role == null) {
            return false;
        }

        // 小丑不是杀手阵营，但它的疯魔是被动触发；需求明确要求把小丑纳入嗅探标记。
        if (JESTER_ID.equals(role.identifier())) {
            return true;
        }

        if (!role.canUseKiller()) {
            return false;
        }

        // “能否开疯魔”不维护职业白名单，而是读取最终商店条目。
        // 这样炸弹客、强盗、毒师、清道夫等移除 psycho_mode 的职业会自然被排除；
        // 静语者保留 psycho_mode，所以会按用户确认继续被嗅探标记和二次透视。
        return ShopUtils.getShopEntriesForPlayer(target).stream()
                .map(ShopEntry::id)
                .anyMatch(PSYCHO_MODE_ENTRY_ID::equals);
    }
}
