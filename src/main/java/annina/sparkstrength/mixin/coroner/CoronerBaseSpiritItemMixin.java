package annina.sparkstrength.mixin.coroner;

import annina.sparkstrength.role.coroner.CoronerService;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.ClickType;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.agmas.noellesroles.item.BaseSpiritItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 验尸官从酒保伪装商店买到的基酒会保留，但只有处于酒保伪装时才允许调制和饮用。
 */
@Mixin(BaseSpiritItem.class)
public abstract class CoronerBaseSpiritItemMixin {
    @Inject(method = "onClicked", at = @At("HEAD"), cancellable = true)
    private void sparkstrength$coronerOnlyMixesWhileBartender(
            ItemStack stack,
            ItemStack otherStack,
            Slot slot,
            ClickType clickType,
            PlayerEntity player,
            StackReference cursorStackReference,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!CoronerService.canUseBartenderDisguiseItem(player)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "finishUsing", at = @At("HEAD"), cancellable = true)
    private void sparkstrength$coronerOnlyDrinksBaseSpiritWhileBartender(
            ItemStack stack,
            World world,
            LivingEntity user,
            CallbackInfoReturnable<ItemStack> cir
    ) {
        if (user instanceof PlayerEntity player && !CoronerService.canUseBartenderDisguiseItem(player)) {
            cir.setReturnValue(stack);
        }
    }
}
