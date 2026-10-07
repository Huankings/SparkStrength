package annina.sparkstrength.tablet;

import java.util.Objects;
import java.util.Optional;

public final class TabletUiSession {
    private Section section = Section.CONNECTIONS;
    private String draft;
    private int meetingFirstRow;
    private int suspectFirstRow;

    public TabletUiSession(String initialDraft) {
        draft = initialDraft == null ? "" : initialDraft;
    }

    public Section section() {
        return section;
    }

    public void select(Section nextSection) {
        section = Objects.requireNonNull(nextSection, "nextSection");
    }

    public String draft() {
        return draft;
    }

    public void updateDraft(String nextDraft) {
        draft = nextDraft == null ? "" : nextDraft;
    }

    public Optional<String> submitDraft() {
        String message = draft.trim();
        draft = "";
        return message.isEmpty() ? Optional.empty() : Optional.of(message);
    }

    public int meetingFirstRow() {
        return meetingFirstRow;
    }

    public void scrollMeeting(double verticalAmount, int totalRows, int visibleRows) {
        meetingFirstRow = TabletMeetingScrollRules.scrollFirstRow(
                meetingFirstRow,
                verticalAmount,
                totalRows,
                visibleRows
        );
    }

    public void applyMeetingSnapshot(boolean active, int totalRows, int visibleRows) {
        meetingFirstRow = TabletMeetingScrollRules.firstRowAfterSnapshot(
                meetingFirstRow,
                active,
                totalRows,
                visibleRows
        );
    }

    public int suspectFirstRow() {
        return suspectFirstRow;
    }

    public void scrollSuspects(double verticalAmount, int totalRows, int visibleRows) {
        suspectFirstRow = TabletMeetingScrollRules.scrollFirstRow(
                suspectFirstRow,
                verticalAmount,
                totalRows,
                visibleRows
        );
    }

    public void applySuspectSnapshot(int totalRows, int visibleRows) {
        suspectFirstRow = TabletMeetingScrollRules.clampFirstRow(
                suspectFirstRow,
                totalRows,
                visibleRows
        );
    }

    public enum Section {
        CONNECTIONS("screen.sparkstrength.tablet.tab.connections"),
        CHAT("screen.sparkstrength.tablet.tab.chat"),
        MEETING("screen.sparkstrength.tablet.tab.meeting"),
        SUSPECTS("screen.sparkstrength.tablet.tab.suspects");

        private final String translationKey;

        Section(String translationKey) {
            this.translationKey = translationKey;
        }

        public String translationKey() {
            return translationKey;
        }
    }
}
