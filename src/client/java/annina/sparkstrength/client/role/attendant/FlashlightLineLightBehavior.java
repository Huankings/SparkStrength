package annina.sparkstrength.client.role.attendant;

import annina.sparkstrength.role.attendant.FlashlightLineLightRules;
import annina.sparkstrength.item.FlashlightItem;
import dev.lambdaurora.lambdynlights.api.behavior.DynamicLightBehavior;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.Objects;

/**
 * Moonlight-lamp style directional light that follows a lit held flashlight.
 * 跟随已开启手电筒的月光灯风格方向性光源。
 */
public final class FlashlightLineLightBehavior implements DynamicLightBehavior {
    private static final double BOUNDING_MARGIN_BLOCKS = 4.0;
    private static final float ROTATION_EPSILON_DEGREES = 0.2F;
    private static final double POSITION_EPSILON_BLOCKS = 0.05;
    private static final long IDLE_BEAM_REFRESH_TICKS = 5L;
    private static final double RANGE_EPSILON_BLOCKS = 0.05;

    private final PlayerEntity player;
    private float lastYaw;
    private float lastPitch;
    private Vec3d lastPos;
    private Vec3d eyePos;
    private Vec3d direction;
    private double effectiveRangeBlocks;
    private BlockPos blockingBlockPos;
    private long lastBeamRefreshWorldTime;

    public FlashlightLineLightBehavior(PlayerEntity player) {
        this.player = player;
        this.lastYaw = player.getYaw();
        this.lastPitch = player.getPitch();
        this.lastPos = player.getPos();
        updateSnapshot();
    }

    @Override
    public double lightAtPos(BlockPos pos, double falloffRatio) {
        if (!FlashlightItem.isHeldOn(player)) {
            return 0.0;
        }
        double light = FlashlightLineLightRules.lightAt(
                eyePos.x,
                eyePos.y,
                eyePos.z,
                direction.x,
                direction.y,
                direction.z,
                pos.getX(),
                pos.getY(),
                pos.getZ(),
                effectiveRangeBlocks,
                pos.equals(blockingBlockPos)
        );
        return light;
    }

    @Override
    public BoundingBox getBoundingBox() {
        Vec3d end = eyePos.add(direction.multiply(effectiveRangeBlocks));
        int startX = MathHelper.floor(Math.min(eyePos.x, end.x) - BOUNDING_MARGIN_BLOCKS);
        int startY = MathHelper.floor(Math.min(eyePos.y, end.y) - BOUNDING_MARGIN_BLOCKS);
        int startZ = MathHelper.floor(Math.min(eyePos.z, end.z) - BOUNDING_MARGIN_BLOCKS);
        int endX = MathHelper.floor(Math.max(eyePos.x, end.x) + BOUNDING_MARGIN_BLOCKS);
        int endY = MathHelper.floor(Math.max(eyePos.y, end.y) + BOUNDING_MARGIN_BLOCKS);
        int endZ = MathHelper.floor(Math.max(eyePos.z, end.z) + BOUNDING_MARGIN_BLOCKS);
        return new BoundingBox(startX, startY, startZ, endX, endY, endZ);
    }

    @Override
    public boolean hasChanged() {
        float yaw = player.getYaw();
        float pitch = player.getPitch();
        Vec3d pos = player.getPos();
        boolean changed = Math.abs(MathHelper.wrapDegrees(yaw - lastYaw)) > ROTATION_EPSILON_DEGREES
                || Math.abs(pitch - lastPitch) > ROTATION_EPSILON_DEGREES
                || pos.distanceTo(lastPos) > POSITION_EPSILON_BLOCKS;
        long worldTime = player.getWorld().getTime();
        boolean refreshDue = worldTime - lastBeamRefreshWorldTime >= IDLE_BEAM_REFRESH_TICKS;
        if (!changed && !refreshDue) {
            return false;
        }

        if (changed) {
            lastYaw = yaw;
            lastPitch = pitch;
            lastPos = pos;
        }
        double previousRange = effectiveRangeBlocks;
        BlockPos previousBlockingBlockPos = blockingBlockPos;
        updateSnapshot();
        return changed
                || Math.abs(effectiveRangeBlocks - previousRange) > RANGE_EPSILON_BLOCKS
                || !Objects.equals(blockingBlockPos, previousBlockingBlockPos);
    }

    @Override
    public boolean isRemoved() {
        return player.isRemoved() || !FlashlightItem.isHeldOn(player);
    }

    private void updateSnapshot() {
        eyePos = player.getEyePos();
        direction = player.getRotationVector().normalize();
        BeamSnapshot snapshot = computeBeamSnapshot();
        effectiveRangeBlocks = snapshot.effectiveRangeBlocks();
        blockingBlockPos = snapshot.blockingBlockPos();
        lastBeamRefreshWorldTime = player.getWorld().getTime();
    }

    private BeamSnapshot computeBeamSnapshot() {
        // Match a handheld beam without raycasting every queried block.
        // 用一次中心线检测模拟手持光束，避免对每个被查询方块都做 raycast。
        Vec3d end = eyePos.add(direction.multiply(FlashlightLineLightRules.RANGE_BLOCKS));
        HitResult hit = player.getWorld().raycast(new RaycastContext(
                eyePos,
                end,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                player
        ));
        if (hit.getType() != HitResult.Type.BLOCK || !(hit instanceof BlockHitResult)) {
            return new BeamSnapshot(FlashlightLineLightRules.RANGE_BLOCKS, null);
        }
        BlockHitResult blockHit = (BlockHitResult) hit;
        return new BeamSnapshot(
                FlashlightLineLightRules.effectiveRangeAfterHit(eyePos.distanceTo(hit.getPos())),
                blockHit.getBlockPos()
        );
    }

    private record BeamSnapshot(double effectiveRangeBlocks, BlockPos blockingBlockPos) {
    }
}
