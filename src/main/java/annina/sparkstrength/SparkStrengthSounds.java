package annina.sparkstrength;

import dev.doctor4t.ratatouille.util.registrar.SoundEventRegistrar;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

/**
 * Registers SparkStrength sound events through Wathe's ambience registrar path.
 * 通过 Wathe 环境音使用的 registrar 链路注册 SparkStrength 声音事件。
 */
public final class SparkStrengthSounds {
    private static final SoundEventRegistrar REGISTRAR = new SoundEventRegistrar(SparkStrength.MOD_ID);

    public static final Identifier MUSIC_TAKEDISKRUSH_ID = SparkStrength.id("music.takediskrush");
    public static final SoundEvent MUSIC_TAKEDISKRUSH = REGISTRAR.create("music.takediskrush");
    public static final Identifier SHADOW_JESTER_ID = SparkStrength.id("ambient.shadow_jester");
    public static final SoundEvent SHADOW_JESTER = REGISTRAR.create("ambient.shadow_jester");

    private SparkStrengthSounds() {
    }

    public static void initialize() {
        REGISTRAR.registerEntries();
        if (!Registries.SOUND_EVENT.containsId(MUSIC_TAKEDISKRUSH_ID)) {
            SparkStrength.LOGGER.warn("SparkStrength sound event {} was not registered.", MUSIC_TAKEDISKRUSH_ID);
        }
        if (!Registries.SOUND_EVENT.containsId(SHADOW_JESTER_ID)) {
            SparkStrength.LOGGER.warn("SparkStrength sound event {} was not registered.", SHADOW_JESTER_ID);
        }
    }
}
