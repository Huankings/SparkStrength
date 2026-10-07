package annina.sparkstrength.mixin.veteran;

import annina.sparkstrength.role.coroner.CoronerService;
import annina.sparkstrength.role.veteran.VeteranRules;
import dev.doctor4t.wathe.cca.GameWorldComponent;
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
 * 服务端侧阻止老兵进入 Wathe 原版举刀流程。
 *
 * <p>上一层客户端 mixin 只负责“自己本机不蓄力、不播举刀声、直接发刺杀包”。
 * 但右键物品仍会同步给服务端，服务端如果继续执行 {@link KnifeItem#use}，
 * 就会调用 {@code PlayerEntity#playSound} 并把举刀准备声广播给附近其他玩家。
 * 这里在服务端直接消费老兵的用刀动作，避免任何玩家听到老兵举刀声。</p>
 */
@Mixin(KnifeItem.class)
public abstract class VeteranServerKnifeUseMixin {
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void sparkstrength$veteranSilentServerUse(
            World world,
            PlayerEntity user,
            Hand hand,
            CallbackInfoReturnable<TypedActionResult<ItemStack>> cir
    ) {
        if (world.isClient) {
            return;
        }

        GameWorldComponent game = GameWorldComponent.KEY.get(user.getWorld());
        if (!VeteranRules.isVeteran(game.getRole(user)) && !CoronerService.hasInstantSilentKnifeDisguise(user)) {
            return;
        }

        // 老兵的实际刺杀由客户端即时发送 KnifeStabPayload 后交给 VeteranKnifeService 处理；
        // 服务端这里只负责拦住原版 use，防止它设置蓄力状态或广播举刀准备声。
        cir.setReturnValue(TypedActionResult.consume(user.getStackInHand(hand)));
    }
}
