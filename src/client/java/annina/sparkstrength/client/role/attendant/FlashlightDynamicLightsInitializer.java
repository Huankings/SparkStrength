package annina.sparkstrength.client.role.attendant;

import annina.sparkstrength.item.FlashlightItem;
import dev.lambdaurora.lambdynlights.api.DynamicLightsContext;
import dev.lambdaurora.lambdynlights.api.DynamicLightsInitializer;
import dev.lambdaurora.lambdynlights.api.behavior.DynamicLightBehaviorManager;
import dev.lambdaurora.lambdynlights.api.item.ItemLightSourceManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Registers the SparkStrength flashlight as a directional LambDynamicLights source.
 * 将 SparkStrength 手电筒注册为 LambDynamicLights 的方向性动态光源。
 */
public final class FlashlightDynamicLightsInitializer implements DynamicLightsInitializer {
    private static final Map<UUID, FlashlightLineLightBehavior> BEHAVIORS = new HashMap<>();
    private static DynamicLightBehaviorManager manager;
    private static boolean tickRegistered;

    @Override
    public synchronized void onInitializeDynamicLights(DynamicLightsContext context) {
        // The new LambDynamicLights API gives access to custom behavior lights through this context.
        // 新版 LambDynamicLights 通过 context 暴露自定义动态光行为管理器。
        manager = context.dynamicLightBehaviorManager();
        registerTickHandler();
    }

    @Override
    @SuppressWarnings("removal")
    public void onInitializeDynamicLights(ItemLightSourceManager itemLightSourceManager) {
        // Keep the legacy entrypoint harmless; the tick handler is idempotent and waits for the new manager.
        // 兼容旧入口：tick 注册是幂等的，并会等待新版 manager 初始化完成。
        registerTickHandler();
    }

    private static synchronized void registerTickHandler() {
        if (tickRegistered) {
            return;
        }
        tickRegistered = true;
        ClientTickEvents.END_CLIENT_TICK.register(FlashlightDynamicLightsInitializer::tick);
    }

    private static void tick(MinecraftClient client) {
        if (manager == null) {
            return;
        }
        if (client.world == null) {
            clearBehaviors();
            return;
        }

        Set<UUID> visiblePlayers = new HashSet<>();
        for (PlayerEntity player : client.world.getPlayers()) {
            UUID uuid = player.getUuid();
            visiblePlayers.add(uuid);
            // Any player can emit light with a lit flashlight; no attendant role check belongs here.
            // 任何玩家拿到已开启手电筒都能发光，这里不检查乘务员身份。
            if (FlashlightItem.isHeldOn(player)) {
                ensureBehavior(uuid, player);
            } else {
                removeBehavior(uuid);
            }
        }

        BEHAVIORS.keySet().removeIf(uuid -> {
            if (visiblePlayers.contains(uuid)) {
                return false;
            }
            manager.remove(BEHAVIORS.get(uuid));
            return true;
        });
    }

    private static void ensureBehavior(UUID uuid, PlayerEntity player) {
        if (BEHAVIORS.containsKey(uuid)) {
            return;
        }
        FlashlightLineLightBehavior behavior = new FlashlightLineLightBehavior(player);
        BEHAVIORS.put(uuid, behavior);
        manager.add(behavior);
    }

    private static void removeBehavior(UUID uuid) {
        FlashlightLineLightBehavior behavior = BEHAVIORS.remove(uuid);
        if (behavior != null) {
            manager.remove(behavior);
        }
    }

    private static void clearBehaviors() {
        for (FlashlightLineLightBehavior behavior : BEHAVIORS.values()) {
            manager.remove(behavior);
        }
        BEHAVIORS.clear();
    }
}
