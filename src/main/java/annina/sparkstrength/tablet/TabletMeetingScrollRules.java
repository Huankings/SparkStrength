package annina.sparkstrength.tablet;

/**
 * Keeps the emergency-meeting viewport within the available vote targets.
 * 将紧急会议视窗限制在现有投票目标范围内。
 */
public final class TabletMeetingScrollRules {
    private TabletMeetingScrollRules() {
    }

    public static int clampFirstRow(int firstVisibleRow, int totalRows, int visibleRows) {
        int maxFirstRow = Math.max(0, totalRows - visibleRows);
        return Math.max(0, Math.min(firstVisibleRow, maxFirstRow));
    }

    public static int scrollFirstRow(int firstVisibleRow, double verticalAmount, int totalRows, int visibleRows) {
        int rowDelta = verticalAmount > 0.0D ? -1 : verticalAmount < 0.0D ? 1 : 0;
        return clampFirstRow(firstVisibleRow + rowDelta, totalRows, visibleRows);
    }

    public static int firstRowAfterSnapshot(
            int firstVisibleRow,
            boolean meetingActive,
            int totalRows,
            int visibleRows
    ) {
        return meetingActive ? clampFirstRow(firstVisibleRow, totalRows, visibleRows) : 0;
    }
}
