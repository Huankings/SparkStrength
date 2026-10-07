package dev.lambdaurora.lambdynlights.api.behavior;

import net.minecraft.util.math.BlockPos;

public interface DynamicLightBehavior {
    double lightAtPos(BlockPos pos, double falloffRatio);

    BoundingBox getBoundingBox();

    boolean hasChanged();

    default boolean isRemoved() {
        return false;
    }

    record BoundingBox(int startX, int startY, int startZ, int endX, int endY, int endZ) {
        public BoundingBox {
            int minX = Math.min(startX, endX);
            int minY = Math.min(startY, endY);
            int minZ = Math.min(startZ, endZ);
            int maxX = Math.max(startX, endX);
            int maxY = Math.max(startY, endY);
            int maxZ = Math.max(startZ, endZ);
            startX = minX;
            startY = minY;
            startZ = minZ;
            endX = maxX;
            endY = maxY;
            endZ = maxZ;
        }
    }
}
