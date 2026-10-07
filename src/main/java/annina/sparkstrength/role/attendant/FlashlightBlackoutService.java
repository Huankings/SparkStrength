package annina.sparkstrength.role.attendant;

import annina.sparkstrength.item.FlashlightItem;
import dev.doctor4t.wathe.api.event.BlackoutEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Lets an actively held flashlight suppress only the current blackout blindness refresh.
 * 让手持开启的手电筒只压制当前熄灯刷新出来的失明。
 */
public final class FlashlightBlackoutService {
    private static final int BLACKOUT_BLINDNESS_DURATION_TOLERANCE_TICKS = 2;
    public static final int RECENT_BLACKOUT_RECORD_TOLERANCE_TICKS = 5;
    private static final Map<UUID, RememberedBlackoutBlindness> RECENT_BLACKOUT_BLINDNESS = new HashMap<>();
    private static boolean registered;

    private FlashlightBlackoutService() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        BlackoutEffect.BEFORE.register(FlashlightBlackoutService::beforeBlackoutEffect);
    }

    private static BlackoutEffect.BlackoutResult beforeBlackoutEffect(ServerPlayerEntity player, int durationTicks) {
        if (!shouldCancelBlackoutBlindness(FlashlightItem.isHeldOn(player))) {
            // Remember the exact blackout refresh so a later flashlight-on toggle can clear only that blindness.
            // 记录本次熄灯刷新的时长，后续打开手电筒时只清除这一类失明。
            rememberBlackoutBlindness(player, durationTicks);
            return null;
        }

        removeCurrentBlackoutBlindness(player, durationTicks);
        RECENT_BLACKOUT_BLINDNESS.remove(player.getUuid());
        return BlackoutEffect.BlackoutResult.cancel();
    }

    static boolean shouldCancelBlackoutBlindness(boolean holdingLitFlashlight) {
        return holdingLitFlashlight;
    }

    static boolean matchesCurrentBlackoutBlindness(int blindnessDurationTicks, int blackoutDurationTicks) {
        return Math.abs(blindnessDurationTicks - blackoutDurationTicks) <= BLACKOUT_BLINDNESS_DURATION_TOLERANCE_TICKS;
    }

    static boolean shouldClearRememberedBlackoutBlindness(
            boolean flashlightTurnedOn,
            boolean holdingLitFlashlight,
            boolean hasBlindness,
            int blindnessDurationTicks,
            int rememberedBlackoutDurationTicks,
            long rememberedAgeTicks
    ) {
        return flashlightTurnedOn
                && holdingLitFlashlight
                && hasBlindness
                && rememberedAgeTicks <= RECENT_BLACKOUT_RECORD_TOLERANCE_TICKS
                && matchesCurrentBlackoutBlindness(blindnessDurationTicks, rememberedBlackoutDurationTicks);
    }

    static boolean shouldForgetRememberedBlackoutBlindness(long rememberedAgeTicks) {
        return rememberedAgeTicks > RECENT_BLACKOUT_RECORD_TOLERANCE_TICKS;
    }

    public static void onFlashlightToggled(ServerPlayerEntity player, boolean turnedOn) {
        RememberedBlackoutBlindness remembered = RECENT_BLACKOUT_BLINDNESS.get(player.getUuid());
        if (remembered == null) {
            return;
        }
        long rememberedAgeTicks = player.getServerWorld().getTime() - remembered.worldTime();
        if (shouldForgetRememberedBlackoutBlindness(rememberedAgeTicks)) {
            RECENT_BLACKOUT_BLINDNESS.remove(player.getUuid());
            return;
        }

        StatusEffectInstance blindness = player.getStatusEffect(StatusEffects.BLINDNESS);
        if (shouldClearRememberedBlackoutBlindness(
                turnedOn,
                FlashlightItem.isHeldOn(player),
                blindness != null,
                blindness == null ? 0 : blindness.getDuration(),
                remembered.durationTicks(),
                rememberedAgeTicks
        )) {
            // Clear only blindness that still matches the recent blackout refresh, not unrelated blindness sources.
            // 只清除仍匹配最近熄灯刷新的失明，不碰其他来源的失明。
            player.removeStatusEffect(StatusEffects.BLINDNESS);
            RECENT_BLACKOUT_BLINDNESS.remove(player.getUuid());
        }
    }

    private static void rememberBlackoutBlindness(ServerPlayerEntity player, int durationTicks) {
        RECENT_BLACKOUT_BLINDNESS.put(
                player.getUuid(),
                new RememberedBlackoutBlindness(durationTicks, player.getServerWorld().getTime())
        );
    }

    private static void removeCurrentBlackoutBlindness(ServerPlayerEntity player, int durationTicks) {
        StatusEffectInstance blindness = player.getStatusEffect(StatusEffects.BLINDNESS);
        if (blindness != null && matchesCurrentBlackoutBlindness(blindness.getDuration(), durationTicks)) {
            player.removeStatusEffect(StatusEffects.BLINDNESS);
        }
    }

    private record RememberedBlackoutBlindness(int durationTicks, long worldTime) {
    }
}
