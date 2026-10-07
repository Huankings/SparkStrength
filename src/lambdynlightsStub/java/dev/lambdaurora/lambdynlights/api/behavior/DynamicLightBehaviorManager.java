package dev.lambdaurora.lambdynlights.api.behavior;

import org.jetbrains.annotations.Nullable;

public interface DynamicLightBehaviorManager {
    void add(DynamicLightBehavior source);

    boolean remove(@Nullable DynamicLightBehavior source);
}
