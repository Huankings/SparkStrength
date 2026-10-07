package annina.sparkstrength.mixin.wathe;

import dev.doctor4t.wathe.game.gamemode.MurderGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * 通过 SparkStrength 覆盖 Wathe 杀手被动收入的余额上限。
 *
 * <p>Wathe 的 {@code GameConstants.KILLER_PASSIVE_MONEY_CAP} 会在编译时内联到
 * {@code MurderGameMode.tickServerGameLoop}。这里仅替换该方法中的 200，
 * 不直接修改 Wathe 源码，也不会影响击杀奖励、队友奖励或其他金币逻辑。</p>
 *
 * <p>{@code PlayerShopComponent} 使用 {@code int} 保存玩家余额，因此这里使用
 * {@link Integer#MAX_VALUE} 作为可表示的自然边界，实际效果是取消原本的 200
 * 金币限制：杀手个人余额达到 200 后仍会继续获得每 10 秒的被动收入。</p>
 */
@Mixin(MurderGameMode.class)
public abstract class KillerPassiveMoneyCapMixin {
    @ModifyConstant(
            method = "tickServerGameLoop",
            constant = @Constant(intValue = 200)
    )
    private static int sparkstrength$removeKillerPassiveMoneyCap(int originalCap) {
        // 原始 200 是编译期常量；替换为 int 可表示的最大值，取消实际游戏中的 200 金币限制。
        return Integer.MAX_VALUE;
    }
}
