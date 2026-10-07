package dev.lambdaurora.lambdynlights.api;

import dev.lambdaurora.lambdynlights.api.item.ItemLightSourceManager;

public interface DynamicLightsInitializer {
    default void onInitializeDynamicLights(DynamicLightsContext context) {
        onInitializeDynamicLights(context.itemLightSourceManager());
    }

    @Deprecated(forRemoval = true)
    void onInitializeDynamicLights(ItemLightSourceManager itemLightSourceManager);
}
