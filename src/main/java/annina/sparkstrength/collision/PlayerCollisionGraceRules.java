package annina.sparkstrength.collision;

/**
 * SparkStrength 的开局玩家碰撞保护规则。
 *
 * <p>Wathe 的玩家实体碰撞是按 tick 判断的，因此这里统一把配置写成 tick，
 * 同时保留秒数常量作为最直观的调节入口。后续只需要修改
 * {@link #START_NO_COLLISION_SECONDS}，就可以调整每局开头取消 Wathe 玩家实体墙的时长。</p>
 */
public final class PlayerCollisionGraceRules {
    /**
     * 每局正式进入 ACTIVE 后，暂时不启用 Wathe 玩家实体墙的秒数。
     *
     * <p>当前需求为 30 秒。这里不读取 Wathe 的游戏倒计时，避免改局内时间、暂停时间或其他
     * 模式设置时意外改变开局保护期。</p>
     */
    public static final int START_NO_COLLISION_SECONDS = 30;

    /** Minecraft 默认每秒 20 tick；碰撞判断使用世界 tick，所以预先换算一次。 */
    public static final long START_NO_COLLISION_TICKS = START_NO_COLLISION_SECONDS * 20L;

    private PlayerCollisionGraceRules() {
    }
}
