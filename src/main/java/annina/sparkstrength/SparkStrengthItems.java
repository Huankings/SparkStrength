package annina.sparkstrength;

import annina.sparkstrength.item.CaptureDeviceItem;
import annina.sparkstrength.item.CapsuleItem;
import annina.sparkstrength.item.CoronerBodyBagItem;
import annina.sparkstrength.item.FlashlightItem;
import annina.sparkstrength.item.MorphDeviceItem;
import annina.sparkstrength.item.MorphReagentItem;
import annina.sparkstrength.item.PowerRestorationItem;
import annina.sparkstrength.item.ProfessorSerumItem;
import annina.sparkstrength.item.TabletItem;
import annina.sparkstrength.item.TimekeeperWatchItem;
import annina.sparkstrength.item.SerialPistolItem;
import annina.sparkstrength.role.professor.ProfessorSerumType;
import com.mojang.serialization.Codec;
import net.minecraft.component.ComponentType;
import net.minecraft.item.Item;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class SparkStrengthItems {
    public static final Identifier CAPSULE_ID = SparkStrength.id("capsule");
    public static final Identifier FLASHLIGHT_ID = SparkStrength.id("flashlight");
    public static final Identifier TABLET_ID = SparkStrength.id("tablet");
    public static final Identifier INVISIBILITY_SERUM_ID = SparkStrength.id("invisibility_serum");
    public static final Identifier DOORPASSING_POTION_ID = SparkStrength.id("doorpassing_potion");
    public static final Identifier SEDATIVE_ID = SparkStrength.id("sedative");
    public static final Identifier TRUTH_SERUM_ID = SparkStrength.id("truth_serum");
    public static final Identifier CAPTURE_DEVICE_ID = SparkStrength.id("capture_device");
    public static final Identifier POWER_RESTORATION_ID = SparkStrength.id("power_restoration");
    public static final Identifier MORPH_REAGENT_ID = SparkStrength.id("morph_reagent");
    public static final Identifier MORPH_DEVICE_ID = SparkStrength.id("morph_device");
    public static final Identifier CORONER_BODY_BAG_ID = SparkStrength.id("coroner_body_bag");
    public static final Identifier DYING_WATCH_ID = SparkStrength.id("dying_watch");
    public static final Identifier SERIAL_PISTOL_ID = SparkStrength.id("serial_pistol");
    public static final Identifier SERIAL_LEFT_PISTOL_ID = SparkStrength.id("serial_left_pistol");

    /** 怀表模式数据组件：0=刷新物品冷却，1=刷新技能冷却。 */
    public static final ComponentType<Integer> TIMEKEEPER_WATCH_MODE = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            SparkStrength.id("timekeeper_watch_mode"),
            ComponentType.<Integer>builder()
                    .codec(Codec.INT)
                    .packetCodec(PacketCodecs.INTEGER)
                    .build()
    );
    private static Item capsule;
    private static Item flashlight;
    private static Item tablet;
    private static Item invisibilitySerum;
    private static Item doorpassingPotion;
    private static Item sedative;
    private static Item truthSerum;
    private static Item captureDevice;
    private static Item powerRestoration;
    private static Item morphReagent;
    private static Item morphDevice;
    private static Item coronerBodyBag;
    private static Item dyingWatch;
    private static Item serialPistol;
    private static Item serialLeftPistol;
    private static boolean registered;

    private SparkStrengthItems() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        capsule = Registry.register(
                Registries.ITEM,
                CAPSULE_ID,
                new CapsuleItem(new Item.Settings().maxCount(1))
        );
        flashlight = Registry.register(
                Registries.ITEM,
                FLASHLIGHT_ID,
                new FlashlightItem(new Item.Settings().maxCount(1))
        );
        tablet = Registry.register(
                Registries.ITEM,
                TABLET_ID,
                new TabletItem(new Item.Settings().maxCount(1))
        );
        invisibilitySerum = Registry.register(
                Registries.ITEM,
                INVISIBILITY_SERUM_ID,
                new ProfessorSerumItem(new Item.Settings().maxCount(1), ProfessorSerumType.INVISIBILITY)
        );
        doorpassingPotion = Registry.register(
                Registries.ITEM,
                DOORPASSING_POTION_ID,
                new ProfessorSerumItem(new Item.Settings().maxCount(1), ProfessorSerumType.DOORPASSING)
        );
        sedative = Registry.register(
                Registries.ITEM,
                SEDATIVE_ID,
                new ProfessorSerumItem(new Item.Settings().maxCount(1), ProfessorSerumType.SEDATIVE)
        );
        truthSerum = Registry.register(
                Registries.ITEM,
                TRUTH_SERUM_ID,
                new ProfessorSerumItem(new Item.Settings().maxCount(1), ProfessorSerumType.TRUTH)
        );
        captureDevice = Registry.register(
                Registries.ITEM,
                CAPTURE_DEVICE_ID,
                new CaptureDeviceItem(new Item.Settings().maxCount(1))
        );
        powerRestoration = Registry.register(
                Registries.ITEM,
                POWER_RESTORATION_ID,
                new PowerRestorationItem(new Item.Settings().maxCount(1))
        );
        morphReagent = Registry.register(
                Registries.ITEM,
                MORPH_REAGENT_ID,
                new MorphReagentItem(new Item.Settings().maxCount(1))
        );
        morphDevice = Registry.register(
                Registries.ITEM,
                MORPH_DEVICE_ID,
                new MorphDeviceItem(new Item.Settings().maxCount(1))
        );
        coronerBodyBag = Registry.register(
                Registries.ITEM,
                CORONER_BODY_BAG_ID,
                new CoronerBodyBagItem(new Item.Settings().maxCount(1))
        );
        // 计时员濒毁怀表：只保留两种刷新冷却模式，状态由 SparkStrength 数据组件保存。
        dyingWatch = Registry.register(
                Registries.ITEM,
                DYING_WATCH_ID,
                new TimekeeperWatchItem(new Item.Settings()
                        .maxCount(1)
                        .component(TIMEKEEPER_WATCH_MODE, 0))
        );
        serialPistol = Registry.register(
                Registries.ITEM,
                SERIAL_PISTOL_ID,
                new SerialPistolItem(new Item.Settings().maxCount(1), false)
        );
        serialLeftPistol = Registry.register(
                Registries.ITEM,
                SERIAL_LEFT_PISTOL_ID,
                new SerialPistolItem(new Item.Settings().maxCount(1), true)
        );
        registered = true;
    }

    public static Item capsule() {
        if (capsule == null) {
            throw new IllegalStateException("SparkStrength items are not registered yet");
        }
        return capsule;
    }

    public static Item flashlight() {
        if (flashlight == null) {
            throw new IllegalStateException("SparkStrength items are not registered yet");
        }
        return flashlight;
    }

    public static Item tablet() {
        if (tablet == null) {
            throw new IllegalStateException("SparkStrength items are not registered yet");
        }
        return tablet;
    }

    public static Item invisibilitySerum() {
        if (invisibilitySerum == null) {
            throw new IllegalStateException("SparkStrength items are not registered yet");
        }
        return invisibilitySerum;
    }

    public static Item doorpassingPotion() {
        if (doorpassingPotion == null) {
            throw new IllegalStateException("SparkStrength items are not registered yet");
        }
        return doorpassingPotion;
    }

    public static Item sedative() {
        if (sedative == null) {
            throw new IllegalStateException("SparkStrength items are not registered yet");
        }
        return sedative;
    }

    public static Item truthSerum() {
        if (truthSerum == null) {
            throw new IllegalStateException("SparkStrength items are not registered yet");
        }
        return truthSerum;
    }

    public static Item captureDevice() {
        if (captureDevice == null) {
            throw new IllegalStateException("SparkStrength items are not registered yet");
        }
        return captureDevice;
    }

    public static Item powerRestoration() {
        if (powerRestoration == null) {
            throw new IllegalStateException("SparkStrength items are not registered yet");
        }
        return powerRestoration;
    }

    public static Item morphReagent() {
        if (morphReagent == null) {
            throw new IllegalStateException("SparkStrength items are not registered yet");
        }
        return morphReagent;
    }

    public static Item morphDevice() {
        if (morphDevice == null) {
            throw new IllegalStateException("SparkStrength items are not registered yet");
        }
        return morphDevice;
    }

    public static Item coronerBodyBag() {
        if (coronerBodyBag == null) {
            throw new IllegalStateException("SparkStrength items are not registered yet");
        }
        return coronerBodyBag;
    }

    public static Item dyingWatch() {
        if (dyingWatch == null) {
            throw new IllegalStateException("SparkStrength items are not registered yet");
        }
        return dyingWatch;
    }

    public static Item serialPistol() {
        if (serialPistol == null) throw new IllegalStateException("SparkStrength items are not registered yet");
        return serialPistol;
    }

    public static Item serialLeftPistol() {
        if (serialLeftPistol == null) throw new IllegalStateException("SparkStrength items are not registered yet");
        return serialLeftPistol;
    }
}
