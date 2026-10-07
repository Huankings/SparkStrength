package annina.sparkstrength.client.role.corruptcop;

import annina.sparkstrength.SparkStrengthSounds;
import net.minecraft.client.sound.AbstractSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.TickableSoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.random.Random;

/**
 * Local-only looping music for the Corrupt Cop active state.
 * 黑警主动技能开启状态的本地循环音乐实例。
 */
public final class CorruptCopMusicInstance extends AbstractSoundInstance implements TickableSoundInstance {
    private boolean done;

    public CorruptCopMusicInstance() {
        // Match manual acceptance: /playsound sparkstrength:music.takediskrush ambient @s.
        // 对齐手动验收：/playsound sparkstrength:music.takediskrush ambient @s。
        super(SparkStrengthSounds.MUSIC_TAKEDISKRUSH, SoundCategory.AMBIENT, Random.create());
        this.repeat = true;
        this.repeatDelay = 0;
        this.attenuationType = SoundInstance.AttenuationType.NONE;
        this.relative = true;
        this.volume = 1.0F;
        this.pitch = 1.0F;
    }

    @Override
    public boolean isDone() {
        return done;
    }

    @Override
    public void tick() {
    }

    public void stopLoop() {
        done = true;
    }
}
