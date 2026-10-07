package annina.sparkstrength.role.attendant;

/**
 * Pure moonlight-lamp style cone-light math for SparkStrength flashlights.
 * SparkStrength 手电筒使用的月光灯风格锥形光纯规则。
 */
public final class FlashlightLineLightRules {
    public static final double RANGE_BLOCKS = 32.0;
    public static final double LUMINANCE = 15.0;
    public static final double DIRECT_BLOCKER_LUMINANCE = 15.0;
    public static final double INNER_CONE_RADIANS = 0.5;
    public static final double OUTER_CONE_RADIANS = 0.7;
    public static final double WALL_PADDING_BLOCKS = 0.75;

    private FlashlightLineLightRules() {
    }

    public static double lightAt(
            double sourceX,
            double sourceY,
            double sourceZ,
            double directionX,
            double directionY,
            double directionZ,
            int blockX,
            int blockY,
            int blockZ
    ) {
        return lightAt(
                sourceX,
                sourceY,
                sourceZ,
                directionX,
                directionY,
                directionZ,
                blockX,
                blockY,
                blockZ,
                RANGE_BLOCKS,
                INNER_CONE_RADIANS,
                OUTER_CONE_RADIANS,
                LUMINANCE
        );
    }

    public static double lightAt(
            double sourceX,
            double sourceY,
            double sourceZ,
            double directionX,
            double directionY,
            double directionZ,
            int blockX,
            int blockY,
            int blockZ,
            double rangeBlocks,
            boolean directBlockerHit
    ) {
        double light = lightAt(
                sourceX,
                sourceY,
                sourceZ,
                directionX,
                directionY,
                directionZ,
                blockX,
                blockY,
                blockZ,
                rangeBlocks
        );
        return directBlockerHit ? Math.max(light, DIRECT_BLOCKER_LUMINANCE) : light;
    }

    public static double lightAt(
            double sourceX,
            double sourceY,
            double sourceZ,
            double directionX,
            double directionY,
            double directionZ,
            int blockX,
            int blockY,
            int blockZ,
            double rangeBlocks
    ) {
        return lightAt(
                sourceX,
                sourceY,
                sourceZ,
                directionX,
                directionY,
                directionZ,
                blockX,
                blockY,
                blockZ,
                rangeBlocks,
                INNER_CONE_RADIANS,
                OUTER_CONE_RADIANS,
                LUMINANCE
        );
    }

    public static double effectiveRangeAfterHit(double hitDistanceBlocks) {
        return Math.max(0.0, Math.min(RANGE_BLOCKS, hitDistanceBlocks + WALL_PADDING_BLOCKS));
    }

    static double lightAt(
            double sourceX,
            double sourceY,
            double sourceZ,
            double directionX,
            double directionY,
            double directionZ,
            int blockX,
            int blockY,
            int blockZ,
            double range,
            double innerConeRadians,
            double outerConeRadians,
            double luminance
    ) {
        double targetX = blockX + 0.5;
        double targetY = blockY + 0.5;
        double targetZ = blockZ + 0.5;
        double deltaX = targetX - sourceX;
        double deltaY = targetY - sourceY;
        double deltaZ = targetZ - sourceZ;
        double distanceSquared = deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
        double rangeSquared = range * range;
        if (distanceSquared > rangeSquared || distanceSquared <= 0.0) {
            return 0.0;
        }

        double directionLength = Math.sqrt(directionX * directionX + directionY * directionY + directionZ * directionZ);
        if (directionLength <= 0.0) {
            return 0.0;
        }
        double normalizedX = directionX / directionLength;
        double normalizedY = directionY / directionLength;
        double normalizedZ = directionZ / directionLength;
        double projection = normalizedX * deltaX + normalizedY * deltaY + normalizedZ * deltaZ;
        if (projection <= 0.0) {
            return 0.0;
        }

        double innerCos = Math.cos(innerConeRadians);
        double outerCos = Math.cos(outerConeRadians);
        double angleCos = projection / Math.sqrt(distanceSquared);
        if (angleCos < outerCos) {
            return 0.0;
        }

        double coneFactor = angleCos >= innerCos
                ? 1.0
                : (angleCos - outerCos) / (innerCos - outerCos);
        double distanceFactor = 1.0 - distanceSquared / rangeSquared;
        return Math.max(luminance * coneFactor * distanceFactor, 0.0);
    }
}
