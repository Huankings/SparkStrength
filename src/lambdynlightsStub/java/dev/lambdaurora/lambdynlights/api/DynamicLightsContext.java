package dev.lambdaurora.lambdynlights.api;

import dev.lambdaurora.lambdynlights.api.behavior.DynamicLightBehaviorManager;
import dev.lambdaurora.lambdynlights.api.item.ItemLightSourceManager;

public interface DynamicLightsContext {
    ItemLightSourceManager itemLightSourceManager();

    DynamicLightBehaviorManager dynamicLightBehaviorManager();
}
