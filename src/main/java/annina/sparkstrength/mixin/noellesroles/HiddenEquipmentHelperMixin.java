package annina.sparkstrength.mixin.noellesroles;

import annina.sparkstrength.SparkStrengthItems;
import annina.sparkstrength.role.coroner.CoronerService;
import annina.sparkstrength.role.engineer.EngineerCaptureReport;
import annina.sparkstrength.role.engineer.EngineerRules;
import annina.sparkstrength.role.professor.ProfessorSerumRules;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.util.HiddenEquipmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 *
 * 将 SparkStrength 平板、工程师捕捉装置、教授试剂和变形怪道具加入 NoellesRoles 的隐藏装备过滤器。
 */
@Mixin(value = HiddenEquipmentHelper.class, remap = false)
public abstract class HiddenEquipmentHelperMixin {
    @Inject(method = "shouldHideItem", at = @At("HEAD"), cancellable = true, remap = false)
    private static void sparkstrength$hideTablet(
            ItemStack stack,
            PlayerEntity holder,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (stack.isOf(SparkStrengthItems.tablet())) {
            cir.setReturnValue(true);
            return;
        }

        if (stack.isOf(SparkStrengthItems.dyingWatch())) {
            // 濒毁怀表属于计时员私有能力道具，手持时对其他存活玩家隐藏。
            cir.setReturnValue(true);
            return;
        }

        if (EngineerCaptureReport.isCaptureReport(stack)) {
            // 捕捉报告属于工程师私有信息；NoellesRoles 的装备包过滤会让其他存活玩家看不到手持报告。
            cir.setReturnValue(true);
            return;
        }

        if (stack.isOf(SparkStrengthItems.captureDevice())
                && (EngineerRules.isEngineer(GameWorldComponent.KEY.get(holder.getWorld()).getRole(holder))
                || CoronerService.hasEngineerDisguise(holder))) {
            // 捕捉装置手持时只对别人隐藏；放置后的实体可见性由实体渲染器按观察者身份判断。
            cir.setReturnValue(true);
            return;
        }

        if ((stack.isOf(SparkStrengthItems.invisibilitySerum())
                || stack.isOf(SparkStrengthItems.doorpassingPotion())
                || stack.isOf(SparkStrengthItems.sedative())
                || stack.isOf(SparkStrengthItems.truthSerum()))
                && (ProfessorSerumRules.isProfessor(GameWorldComponent.KEY.get(holder.getWorld()).getRole(holder))
                || CoronerService.hasProfessorDisguise(holder))) {
            // 用户需求是“教授手持四种试剂时不可见”，所以这里额外校验持有者确实是教授。
            cir.setReturnValue(true);
            return;
        }

        if (stack.isOf(SparkStrengthItems.morphReagent()) || stack.isOf(SparkStrengthItems.morphDevice())) {
            // 变形试剂和遥控器是 Morphling 的核心情报道具；只要被装备隐藏系统扫描到就不展示给其他存活玩家。
            cir.setReturnValue(true);
            return;
        }

        if (stack.isOf(ModItems.BASE_SPIRIT) && CoronerService.hasBartenderDisguise(holder)) {
            // 验尸官伪装酒保时，基酒手持隐藏规则与真实酒保一致。
            cir.setReturnValue(true);
            return;
        }

        if (stack.isOf(ModItems.TIMED_BOMB) && CoronerService.hasBomberDisguise(holder)) {
            // 验尸官伪装炸弹客时，未放置的定时炸弹不暴露给其他玩家。
            cir.setReturnValue(true);
        }
    }
}
