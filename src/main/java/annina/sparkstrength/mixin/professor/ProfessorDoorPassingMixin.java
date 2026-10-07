package annina.sparkstrength.mixin.professor;

import annina.sparkstrength.component.professor.ProfessorSerumTargetComponent;
import dev.doctor4t.wathe.block.DoorPartBlock;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.block.BlockState;
import net.minecraft.block.EntityShapeContext;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 穿门试剂：让目标在持续时间内忽略 Wathe 门的方块碰撞。
 *
 * <p>Wathe 门真正挡人的地方是 {@link DoorPartBlock#getCollisionShape}，
 * 因此这里只在碰撞形状查询时对带有效果的存活玩家返回空形状。
 * 门的渲染、开关状态、交互和射线检测都不改。</p>
 */
@Mixin(DoorPartBlock.class)
public abstract class ProfessorDoorPassingMixin {
    @Inject(method = "getCollisionShape", at = @At("HEAD"), cancellable = true)
    private void sparkstrength$passThroughDoor(
            BlockState state,
            BlockView world,
            BlockPos pos,
            ShapeContext context,
            CallbackInfoReturnable<VoxelShape> cir
    ) {
        if (!(context instanceof EntityShapeContext entityShapeContext)) {
            return;
        }

        Entity entity = entityShapeContext.getEntity();
        if (entity instanceof PlayerEntity player
                && GameFunctions.isPlayerPlayingAndAlive(player)
                && ProfessorSerumTargetComponent.KEY.get(player).hasDoorpassing()) {
            cir.setReturnValue(VoxelShapes.empty());
        }
    }
}
