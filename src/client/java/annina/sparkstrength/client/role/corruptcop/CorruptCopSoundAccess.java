package annina.sparkstrength.client.role.corruptcop;

import annina.sparkstrength.client.mixin.corruptcop.SoundManagerAccessor;
import annina.sparkstrength.client.mixin.corruptcop.SoundSystemAccessor;
import net.minecraft.client.sound.Channel;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.client.sound.SoundSystem;
import net.minecraft.client.sound.Source;

/**
 * Narrow bridge for pausing and resuming one Corrupt Cop client sound source.
 * 仅用于暂停与恢复黑警客户端音乐声源的窄桥接层。
 */
public final class CorruptCopSoundAccess {
    private CorruptCopSoundAccess() {
    }

    public static boolean pause(SoundManager manager, SoundInstance instance) {
        return runOnSource(manager, instance, Source::pause);
    }

    public static boolean resume(SoundManager manager, SoundInstance instance) {
        return runOnSource(manager, instance, Source::resume);
    }

    private static boolean runOnSource(
            SoundManager manager,
            SoundInstance instance,
            java.util.function.Consumer<Source> action
    ) {
        if (manager == null || instance == null) {
            return false;
        }
        SoundSystem soundSystem = ((SoundManagerAccessor) manager).sparkstrength$getSoundSystem();
        Channel.SourceManager sourceManager = ((SoundSystemAccessor) soundSystem).sparkstrength$getSources().get(instance);
        if (sourceManager == null || sourceManager.isStopped()) {
            return false;
        }
        sourceManager.run(action);
        return true;
    }
}
