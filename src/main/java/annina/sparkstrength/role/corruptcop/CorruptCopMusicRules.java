package annina.sparkstrength.role.corruptcop;

/**
 * Timing rules for the Corrupt Cop's owner-only music loop.
 * 黑警主动技能仅本人可听循环音乐的计时规则。
 */
public final class CorruptCopMusicRules {
    public static final int RESUME_WINDOW_TICKS = 200;

    private CorruptCopMusicRules() {
    }

    public static boolean shouldRetainPausedTrack(int inactiveTicks) {
        return inactiveTicks < RESUME_WINDOW_TICKS;
    }

    public static boolean shouldDiscardPausedTrack(int inactiveTicks) {
        return !shouldRetainPausedTrack(inactiveTicks);
    }

    public static boolean shouldReassertRetainedPause(boolean retainedPausedTrack, boolean soundPlaying) {
        return retainedPausedTrack && soundPlaying;
    }

    public static int remainingResumeSeconds(boolean retainedTrack, int inactiveTicks) {
        return retainedTrack ? remainingResumeSeconds(inactiveTicks) : 0;
    }

    public static int remainingResumeSeconds(int inactiveTicks) {
        int remainingTicks = Math.max(0, RESUME_WINDOW_TICKS - inactiveTicks);
        return (remainingTicks + 19) / 20;
    }
}
