package annina.sparkstrength.client.role.shadowjester;

import annina.sparkstrength.SparkStrength;
import annina.sparkstrength.SparkStrengthSounds;
import annina.sparkstrength.component.shadowjester.ShadowJesterShowdownWorldComponent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.sound.SoundManager;

/**
 * 根据同步到客户端的世界组件播放全场新版双影谢幕环境音。
 *
 * <p>这里不检查玩家角色：用户要求全场玩家都能听到，因此每个客户端只要处于
 * 新版谢幕世界状态，就播放同一首相对位置的循环音效。</p>
 */
public final class ShadowJesterShowdownMusicController {
    private static ShadowJesterShowdownMusicInstance music;
    private static boolean missingSoundResourceWarned;
    /**
     * 服务器明确下发的音乐状态。
     *
     * <p>null 表示本局还没有收到明确包，此时回退到 CCA 世界组件；
     * true/false 表示以服务器指令为准，避免世界组件同步延迟造成播放状态丢失。</p>
     */
    private static Boolean serverRequestedActive;

    private ShadowJesterShowdownMusicController() {
    }

    public static void tick(MinecraftClient client) {
        if (client == null || client.world == null || client.player == null) {
            stopAndClear(client == null ? null : client.getSoundManager());
            return;
        }

        ClientPlayerEntity player = client.player;
        boolean active = serverRequestedActive != null
                ? serverRequestedActive
                : ShadowJesterShowdownWorldComponent.KEY.get(player.getWorld()).isActive();
        SoundManager soundManager = client.getSoundManager();

        if (active) {
            playOrResume(soundManager);
        } else {
            fadeOut(soundManager);
        }
    }

    private static void playOrResume(SoundManager soundManager) {
        if (soundManager == null || !hasMusicResource(soundManager)) {
            return;
        }

        if (music == null || !soundManager.isPlaying(music)) {
            music = new ShadowJesterShowdownMusicInstance();
            soundManager.play(music);
            return;
        }

        music.cancelFadeOut();
    }

    private static void fadeOut(SoundManager soundManager) {
        if (music == null || soundManager == null) {
            return;
        }

        if (!soundManager.isPlaying(music)) {
            clearState();
            return;
        }

        music.beginFadeOut();
        if (music.isDone()) {
            music.stopLoop();
            soundManager.stop(music);
            clearState();
        }
    }

    private static boolean hasMusicResource(SoundManager soundManager) {
        if (soundManager.get(SparkStrengthSounds.SHADOW_JESTER_ID) != null) {
            missingSoundResourceWarned = false;
            return true;
        }

        if (!missingSoundResourceWarned) {
            SparkStrength.LOGGER.warn(
                    "Missing client sound resource {}. Check assets/sparkstrength/sounds.json and the installed client jar.",
                    SparkStrengthSounds.SHADOW_JESTER_ID
            );
            missingSoundResourceWarned = true;
        }
        return false;
    }

    private static void stopAndClear(SoundManager soundManager) {
        if (music != null && soundManager != null) {
            music.stopLoop();
            soundManager.stop(music);
        }
        clearState();
    }

    public static void reset() {
        serverRequestedActive = null;
        clearState();
    }

    /**
     * 接收服务器明确下发的新版谢幕音乐状态。
     */
    public static void setServerRequestedActive(boolean active) {
        serverRequestedActive = active;
    }

    private static void clearState() {
        music = null;
    }
}
