package annina.sparkstrength.mixin.coroner;

import annina.sparkstrength.role.coroner.CoronerService;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.agmas.noellesroles.item.IronManVialItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 验尸官购买/获得的铁人药剂保留在背包，但离开教授伪装后不能继续触发教授能力。
 */
@Mixin(IronManVialItem.class)
public abstract class CoronerIronManVialItemMixin {
    @Inject(method = "useOnEntity", at = @At("HEAD"), cancellable = true)
    private void sparkstrength$coronerOnlyUsesIronManVialWhileProfessor(
            ItemStack stack,
            PlayerEntity user,
            LivingEntity entity,
            Hand hand,
            CallbackInfoReturnable<ActionResult> cir
    ) {
        if (!CoronerService.canUseProfessorDisguiseItem(user)) {
            cir.setReturnValue(ActionResult.PASS);
        }
    }
}
