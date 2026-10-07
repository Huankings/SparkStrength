package annina.sparkstrength.role.recaller;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.game.GameConstants;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * 回溯者增强的纯规则与数值配置。
 *
 * <p>这里集中保存职业 ID、商店条目 ID、商品价格和被动收入参数，
 * 后续需要调数值时优先改这个类，避免把魔法数字散落到服务逻辑里。</p>
 */
public final class RecallerShopRules {
    public static final Identifier RECALLER_ID = Identifier.of("noellesroles", "recaller");

    /** 回溯者商店：末影珍珠条目 ID。 */
    public static final String ENDER_PEARL_ENTRY_ID = "sparkstrength_recaller_ender_pearl";
    /** 回溯者商店：紫颂果条目 ID。 */
    public static final String CHORUS_FRUIT_ENTRY_ID = "sparkstrength_recaller_chorus_fruit";

    /** 回溯者商店中末影珍珠的单个购买价格。 */
    public static final int ENDER_PEARL_PRICE = 125;
    /** 回溯者商店中紫颂果的单个购买价格。 */
    public static final int CHORUS_FRUIT_PRICE = 35;

    /** 回溯者被动收入间隔：每 10 秒结算一次。 */
    public static final int PASSIVE_INCOME_INTERVAL_TICKS = GameConstants.getInTicks(0, 10);
    /** 回溯者每次被动收入结算获得的金币。 */
    public static final int PASSIVE_INCOME_AMOUNT = 5;
    /**
     * 回溯者被动收入余额上限。
     *
     * <p>当该值为负数时表示没有上限；为非负数时，被动收入最多把余额补到这个值，
     * 不会因为一次结算而越过上限。</p>
     */
    public static final int PASSIVE_INCOME_BALANCE_CAP = -1;

    private RecallerShopRules() {
    }

    public static boolean isRecaller(@Nullable Role role) {
        return role != null && RECALLER_ID.equals(role.identifier());
    }

    public static boolean canUseRecallerShop(@Nullable Role role) {
        return isRecaller(role);
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
}
