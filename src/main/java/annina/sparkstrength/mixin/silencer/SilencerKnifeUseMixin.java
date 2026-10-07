package annina.sparkstrength.mixin.silencer;

import annina.sparkstrength.role.silencer.SilencerQuietService;
import dev.doctor4t.wathe.item.KnifeItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 静语者使用 Wathe 匕首举刀时不播放准备音效。
 *
 * <p>这里不把静语者改成清道夫/老兵那种瞬发刀；仍然设置当前手、保留原版蓄力和松手刺杀，
 * 只是在 {@link KnifeItem#use} 开头复制原动作并跳过 {@code user.playSound(...)}。</p>
 */
@Mixin(KnifeItem.class)
public abstract class SilencerKnifeUseMixin {
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void sparkstrength$silencerSilentKnifePrepare(
            World world,
            PlayerEntity user,
            Hand hand,
            CallbackInfoReturnable<TypedActionResult<ItemStack>> cir
    ) {
        if (!SilencerQuietService.usesSilencerQuietWeapons(user)) {
            return;
        }

        ItemStack itemStack = user.getStackInHand(hand);
        user.setCurrentHand(hand);
        cir.setReturnValue(TypedActionResult.consume(itemStack));
    }
}
