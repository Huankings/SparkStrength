package annina.sparkstrength.client.mixin.serialkiller;

import annina.sparkstrength.SparkStrengthItems;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 连环杀手副手枪的第三人称托举动作。
 *
 * <p>Wathe 原版只根据主手枪械抬起主手臂。连环杀手的左持手枪是真正放在
 * 副手槽中的独立物品，因此这里补充抬起“主手相反侧”的手臂；左右手玩家
 * 都按 Minecraft 的 MainArm 映射处理，避免把副手动作固定错误到左臂。</p>
 */
@Mixin(BipedEntityModel.class)
public abstract class SerialKillerBipedEntityModelMixin<T extends LivingEntity> {
    @Shadow @Final public ModelPart leftArm;
    @Shadow @Final public ModelPart rightArm;
    @Shadow @Final public ModelPart head;

    @Inject(method = "positionLeftArm", at = @At("TAIL"))
    private void sparkstrength$holdSerialOffhandLeftArm(T entity, CallbackInfo ci) {
        // MainArm.RIGHT 时，OFF_HAND 对应左臂。
        if (entity.getMainArm() == Arm.RIGHT && isHoldingSerialOffhand(entity)) {
            holdGun(leftArm, head, false);
        }
    }

    @Inject(method = "positionRightArm", at = @At("TAIL"))
    private void sparkstrength$holdSerialOffhandRightArm(T entity, CallbackInfo ci) {
        // MainArm.LEFT 时，OFF_HAND 对应右臂。
        if (entity.getMainArm() != Arm.RIGHT && isHoldingSerialOffhand(entity)) {
            holdGun(rightArm, head, true);
        }
    }

    @Unique
    private static boolean isHoldingSerialOffhand(LivingEntity entity) {
        ItemStack offhand = entity.getOffHandStack();
        return offhand.isOf(SparkStrengthItems.serialLeftPistol());
    }

    @Unique
    private static void holdGun(ModelPart arm, ModelPart head, boolean rightArm) {
        arm.yaw = (rightArm ? -0.3F : 0.3F) + head.yaw;
        arm.pitch = (float) (-Math.PI / 2) + head.pitch + 0.1F;
    }
}
