package annina.sparkstrength.role.noisemaker;

import dev.doctor4t.wathe.game.GameConstants;

/**
 * 大嗓门增强相关数值。
 *
 * <p>统一放在这里，后续要平衡时不用去翻服务端逻辑和 UI 代码。</p>
 */
public final class NoisemakerGlowConstants {
    public static final int GLOW_DURATION_TICKS = GameConstants.getInTicks(0, 30);
    public static final int COOLDOWN_TICKS = GameConstants.getInTicks(1, 20);
    public static final int KILLER_GLOW_TICKS = GameConstants.getInTicks(0, 15);

    private NoisemakerGlowConstants() {
    }
}
