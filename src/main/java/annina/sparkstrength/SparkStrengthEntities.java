package annina.sparkstrength;

import annina.sparkstrength.entity.CapsuleEntity;
import annina.sparkstrength.entity.CaptureDeviceEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class SparkStrengthEntities {
    public static final Identifier CAPSULE_ID = SparkStrength.id("capsule");
    public static final Identifier CAPTURE_DEVICE_ID = SparkStrength.id("capture_device");
    private static EntityType<CapsuleEntity> capsule;
    private static EntityType<CaptureDeviceEntity> captureDevice;
    private static boolean registered;

    private SparkStrengthEntities() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        capsule = Registry.register(
                Registries.ENTITY_TYPE,
                CAPSULE_ID,
                EntityType.Builder.<CapsuleEntity>create(CapsuleEntity::new, SpawnGroup.MISC)
                        .dimensions(0.25F, 0.25F)
                        .maxTrackingRange(4)
                        .trackingTickInterval(10)
                        .build(CAPSULE_ID.toString())
        );
        captureDevice = Registry.register(
                Registries.ENTITY_TYPE,
                CAPTURE_DEVICE_ID,
                EntityType.Builder.<CaptureDeviceEntity>create(CaptureDeviceEntity::new, SpawnGroup.MISC)
                        .dimensions(0.35F, 0.08F)
                        .maxTrackingRange(8)
                        .trackingTickInterval(10)
                        .build(CAPTURE_DEVICE_ID.toString())
        );
        registered = true;
    }

    public static EntityType<CapsuleEntity> capsule() {
        if (capsule == null) {
            throw new IllegalStateException("SparkStrength entities are not registered yet");
        }
        return capsule;
    }

    public static EntityType<CaptureDeviceEntity> captureDevice() {
        if (captureDevice == null) {
            throw new IllegalStateException("SparkStrength entities are not registered yet");
        }
        return captureDevice;
    }
}
