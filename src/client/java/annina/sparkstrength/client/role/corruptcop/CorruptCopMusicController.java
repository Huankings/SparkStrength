package annina.sparkstrength.client.role.corruptcop;

import annina.sparkstrength.SparkStrength;
import annina.sparkstrength.SparkStrengthSounds;
import annina.sparkstrength.component.corruptcop.CorruptCopAbilityComponent;
import annina.sparkstrength.role.corruptcop.CorruptCopMusicRules;
import annina.sparkstrength.role.corruptcop.CorruptCopRules;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.sound.SoundManager;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;

/**
 * Drives the owner-only Corrupt Cop music from server-synced component state.
 * 根据服务端同步的组件状态驱动黑警仅本人可听音乐。
 */
public final class CorruptCopMusicController {
    private static CorruptCopMusicInstance music;
    private static boolean paused;
    private static int pausedTicks;
    private static boolean missingSoundResourceWarned;

    private CorruptCopMusicController() {
    }

    public static void tick(MinecraftClient client) {
        if (client == null || client.player == null || client.world == null) {
            stopAndClear(client);
            return;
        }

        ClientPlayerEntity player = client.player;
        if (!isEligiblePlayer(player)) {
            stopAndClear(client);
            return;
        }

        if (CorruptCopAbilityComponent.KEY.get(player).isActive()) {
            playOrResume(client.getSoundManager());
            return;
        }

        pauseOrExpire(client.getSoundManager());
    }

    public static int remainingResumeSeconds() {
        return CorruptCopMusicRules.remainingResumeSeconds(paused && music != null, pausedTicks);
    }

    private static boolean isEligiblePlayer(ClientPlayerEntity player) {
        Role role = GameWorldComponent.KEY.get(player.getWorld()).getRole(player);
        return CorruptCopRules.isCorruptCop(role)
                && GameFunctions.isPlayerPlayingAndAlive(player)
                && !SwallowedPlayerComponent.isPlayerSwallowed(player);
    }

    private static void playOrResume(SoundManager soundManager) {
        if (!hasMusicResource(soundManager)) {
            return;
        }
        if (music == null || !soundManager.isPlaying(music)) {
            music = new CorruptCopMusicInstance();
            paused = false;
            pausedTicks = 0;
            soundManager.play(music);
            return;
        }

        if (paused) {
            CorruptCopSoundAccess.resume(soundManager, music);
            paused = false;
            pausedTicks = 0;
        }
    }

    private static boolean hasMusicResource(SoundManager soundManager) {
        if (soundManager.get(SparkStrengthSounds.MUSIC_TAKEDISKRUSH_ID) != null) {
            missingSoundResourceWarned = false;
            return true;
        }

        if (!missingSoundResourceWarned) {
            // The event exists, but the client resource pack did not expose the sound set.
            // 声音事件已注册，但客户端资源包没有暴露对应的声音集合。
            SparkStrength.LOGGER.warn(
                    "Missing client sound resource {}. Check assets/sparkstrength/sounds.json and the installed client jar.",
                    SparkStrengthSounds.MUSIC_TAKEDISKRUSH_ID
            );
            missingSoundResourceWarned = true;
        }
        return false;
    }

    private static void pauseOrExpire(SoundManager soundManager) {
        if (music == null) {
            paused = false;
            pausedTicks = 0;
            return;
        }
        if (!soundManager.isPlaying(music)) {
            clearState();
            return;
        }

        if (!paused) {
            paused = true;
            pausedTicks = 1;
        } else {
            pausedTicks++;
        }
        if (CorruptCopMusicRules.shouldReassertRetainedPause(paused, soundManager.isPlaying(music))) {
            CorruptCopSoundAccess.pause(soundManager, music);
        }

        if (CorruptCopMusicRules.shouldDiscardPausedTrack(pausedTicks)) {
            stopAndClear(soundManager);
        }
    }

    private static void stopAndClear(MinecraftClient client) {
        if (client == null) {
            clearState();
            return;
        }
        stopAndClear(client.getSoundManager());
    }

    private static void stopAndClear(SoundManager soundManager) {
        if (music != null && soundManager != null) {
            music.stopLoop();
            soundManager.stop(music);
        }
        clearState();
    }

    private static void clearState() {
        music = null;
        paused = false;
        pausedTicks = 0;
    }
}
