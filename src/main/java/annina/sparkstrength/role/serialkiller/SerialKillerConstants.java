package annina.sparkstrength.role.serialkiller;

/** 连环杀手强化的可调数值集中处。所有时间均使用 Minecraft tick。 */
public final class SerialKillerConstants {
    private SerialKillerConstants() {}

    /** 两把专用枪的射程。 */
    public static final double PISTOL_RANGE_BLOCKS = 30.0D;
    /** 两把枪各自独立的开火冷却：2 秒。 */
    public static final int PISTOL_COOLDOWN_TICKS = 20 * 2;
    /** 连环杀手成功击杀目标后，原版刀与左轮均清零。 */
    public static final int RESET_COOLDOWN_TICKS = 0;
}
