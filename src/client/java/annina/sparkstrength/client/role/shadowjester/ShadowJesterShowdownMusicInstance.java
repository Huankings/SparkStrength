package annina.sparkstrength.client.role.shadowjester;

import annina.sparkstrength.SparkStrengthSounds;
import net.minecraft.client.sound.AbstractSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.TickableSoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.random.Random;

/**
 * 全场新版双影谢幕环境音的客户端循环实例。
 *
 * <p>音频资源来自 {@code assets/sparkstrength/sounds/ambient/shadow_jester.ogg}。
 * 淡出在实例内部完成，避免停止音效时产生突然截断。</p>
 */
public final class ShadowJesterShowdownMusicInstance
        extends AbstractSoundInstance
        implements TickableSoundInstance {
    public static final int FADE_TICKS = 20;

    private boolean fadingOut;
    private int fadeTicks;
    private boolean done;

    public ShadowJesterShowdownMusicInstance() {
        super(SparkStrengthSounds.SHADOW_JESTER, SoundCategory.AMBIENT, Random.create());
        this.repeat = true;
        this.repeatDelay = 0;
        this.attenuationType = SoundInstance.AttenuationType.NONE;
        this.relative = true;
        // 这里启动即满音量，避免 0 音量实例在部分客户端声音实现里被压成“听不见”的静音播放。
        this.volume = 1.0F;
        this.pitch = 1.0F;
    }

    @Override
    public boolean isDone() {
        return done;
    }

    @Override
    public void tick() {
        if (fadingOut) {
            fadeTicks++;
            this.volume = Math.max(0.0F, 1.0F - (float) fadeTicks / FADE_TICKS);
            if (fadeTicks >= FADE_TICKS) {
                done = true;
            }
        } else {
            // 新版谢幕只做淡出，不做淡入：开始时直接保持满音量，避免起始阶段被静音吞掉。
            this.volume = 1.0F;
            fadeTicks = 0;
        }
    }

    public void beginFadeOut() {
        fadingOut = true;
        fadeTicks = 0;
    }

    public void cancelFadeOut() {
        fadingOut = false;
        done = false;
        fadeTicks = 0;
        this.volume = 1.0F;
    }

    public void stopLoop() {
        done = true;
    }
}
