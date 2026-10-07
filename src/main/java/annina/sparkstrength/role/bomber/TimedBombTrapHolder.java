package annina.sparkstrength.role.bomber;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 托盘和床共用的定时炸弹陷阱状态访问接口。
 *
 * <p>Spark 使用的 wathe 1.5.6 还没有自改版里的 TrayEffect/BedEffect 公共接口，
 * 因此这里用 mixin 给对应方块实体补一个“预埋炸弹主人 UUID”字段。
 * 交互逻辑只依赖这个接口，避免把托盘和床的具体 mixin 字段名散落到业务代码里。</p>
 */
public interface TimedBombTrapHolder {
    boolean sparkstrength$hasTimedBombTrap();

    @Nullable
    UUID sparkstrength$getTimedBombTrapOwner();

    void sparkstrength$setTimedBombTrapOwner(@Nullable UUID owner);
}
