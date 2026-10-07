package annina.sparkstrength.tablet;

import dev.doctor4t.wathe.game.GameConstants;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Pure constants and calculations for the tablet network.
 * 平板网络使用的纯常量和计算逻辑。
 */
public final class TabletRules {
    public static final int HOTBAR_START_SLOT = 0;
    public static final int HOTBAR_END_SLOT = 8;
    public static final int CHAT_MESSAGE_MAX_LENGTH = 120;
    public static final int DEFAULT_EMERGENCY_MEETING_CHANCES = 1;
    public static final int DEFAULT_MEETING_DURATION_TICKS = GameConstants.getInTicks(1, 40);
    public static final int MEETING_DURATION_TICKS = DEFAULT_MEETING_DURATION_TICKS;
    public static final int MEETING_COOLDOWN_TICKS = GameConstants.getInTicks(1, 0);
    public static final int SUSPECT_REVEAL_INTERVAL_TICKS = GameConstants.getInTicks(0, 45);
    public static final int SUSPECT_REVEAL_TICKS = GameConstants.getInTicks(0, 5);

    private TabletRules() {
    }

    public static boolean meetsTwoThirds(int approvals, int electorate) {
        return electorate > 0 && approvals > 0 && approvals * 3 >= electorate * 2;
    }

    public static int requiredTwoThirds(int electorate) {
        if (electorate <= 0) {
            return 0;
        }
        return (int) Math.ceil(electorate * 2 / 3.0);
    }

    public static int secondsCeil(int ticks) {
        return (int) Math.ceil(Math.max(0, ticks) / 20.0);
    }

    public static int ticksFromSeconds(int seconds) {
        return GameConstants.getInTicks(seconds / 60, seconds % 60);
    }

    public static Optional<UUID> uniqueHighestVote(Map<UUID, Integer> voteCounts) {
        UUID winner = null;
        int best = 0;
        boolean tied = false;
        for (Map.Entry<UUID, Integer> entry : voteCounts.entrySet()) {
            int votes = entry.getValue();
            if (votes <= 0) {
                continue;
            }
            if (votes > best) {
                winner = entry.getKey();
                best = votes;
                tied = false;
            } else if (votes == best) {
                tied = true;
            }
        }
        return winner == null || tied ? Optional.empty() : Optional.of(winner);
    }
}
