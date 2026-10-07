package annina.sparkstrength.role.timekeeper;

import net.minecraft.text.Text;

/**
 * SparkStrength 计时员怀表只保留两个“刷新冷却”模式。
 * 自改版 NoellesRoles 中的时间回溯、损坏和精致状态不在本移植范围内。
 */
public enum TimekeeperWatchMode {
    ITEM_REFRESH("item_refresh", "item.sparkstrength.dying_watch.mode.item_refresh"),
    ABILITY_REFRESH("ability_refresh", "item.sparkstrength.dying_watch.mode.ability_refresh");

    private final String id;
    private final String translationKey;

    TimekeeperWatchMode(String id, String translationKey) {
        this.id = id;
        this.translationKey = translationKey;
    }

    public String id() {
        return id;
    }

    public Text text() {
        return Text.translatable(translationKey);
    }

    public TimekeeperWatchMode next() {
        return this == ITEM_REFRESH ? ABILITY_REFRESH : ITEM_REFRESH;
    }

    public static TimekeeperWatchMode fromOrdinal(int ordinal) {
        return ordinal == ABILITY_REFRESH.ordinal() ? ABILITY_REFRESH : ITEM_REFRESH;
    }
}
