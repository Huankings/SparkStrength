package annina.sparkstrength.client.mixin.corruptcop;

import net.minecraft.client.sound.SoundManager;
import net.minecraft.client.sound.SoundSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes Minecraft's sound system only for Corrupt Cop music pause/resume.
 * 仅为黑警音乐暂停与恢复暴露 Minecraft 声音系统。
 */
@Mixin(SoundManager.class)
public interface SoundManagerAccessor {
    @Accessor("soundSystem")
    SoundSystem sparkstrength$getSoundSystem();
}
