package annina.sparkstrength.role.reporter;

/**
 * 记者背包通讯加强的数值。
 *
 * <p>这些数值按自改版接线员当前配置复刻，但放在 SparkStrength 自己的常量类中，
 * 后续如果只想调整记者加强，不会影响 NoellesRoles 原记者标记技能。</p>
 */
public final class ReporterCommunicationConstants {
    /** 接线持续时间：30 秒。 */
    public static final int CONNECTION_DURATION_TICKS = 30 * 20;
    /** 接线成功后的独立冷却：60 秒。 */
    public static final int CONNECTION_SUCCESS_COOLDOWN_TICKS = 60 * 20;
    /** 接线失败后的独立冷却：40 秒。 */
    public static final int CONNECTION_FAILURE_COOLDOWN_TICKS = 40 * 20;
    /** 广播持续时间：30 秒。 */
    public static final int BROADCAST_DURATION_TICKS = 30 * 20;
    /** 广播成功后的独立冷却：90 秒。 */
    public static final int BROADCAST_SUCCESS_COOLDOWN_TICKS = 90 * 20;
    /** 广播失败后的独立冷却：50 秒。 */
    public static final int BROADCAST_FAILURE_COOLDOWN_TICKS = 50 * 20;

    private ReporterCommunicationConstants() {
    }
}
