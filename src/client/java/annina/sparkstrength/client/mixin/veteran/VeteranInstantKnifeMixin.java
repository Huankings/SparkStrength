package annina.sparkstrength.client.mixin.veteran;

import annina.sparkstrength.role.coroner.CoronerService;
import annina.sparkstrength.role.veteran.VeteranRules;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.item.KnifeItem;
import dev.doctor4t.wathe.item.RevolverItem;
import dev.doctor4t.wathe.util.KnifeStabPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * 老兵匕首客户端无蓄力。
 *
 * <p>原版 KnifeItem#use 会设置当前使用手并播放“举刀准备”音效，松开后才发刺杀包。
 * 老兵加强要求和清道夫一样无蓄力、无举刀声，所以客户端在 use 开头直接找目标并发包，
 * 然后取消原逻辑。服务端还会收到一次右键物品包；那部分由 common 侧
 * {@code VeteranServerKnifeUseMixin} 继续拦截，避免附近其他玩家听到服务端广播的举刀声。</p>
 */
@Mixin(KnifeItem.class)
public abstract class VeteranInstantKnifeMixin {
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void sparkstrength$veteranInstantKnifeUse(
            World world,
            PlayerEntity user,
            Hand hand,
            CallbackInfoReturnable<TypedActionResult<ItemStack>> cir
    ) {
        if (!world.isClient) {
            return;
        }

        GameWorldComponent game = GameWorldComponent.KEY.get(user.getWorld());
        if (!VeteranRules.isVeteran(game.getRole(user)) && !CoronerService.hasInstantSilentKnifeDisguise(user)) {
            return;
        }

        ItemStack itemStack = user.getStackInHand(hand);
        HitResult collision = KnifeItem.getKnifeTarget(user);
        if (collision instanceof EntityHitResult entityHitResult) {
            ClientPlayNetworking.send(new KnifeStabPayload(entityHitResult.getEntity().getId()));
        } else if (collision instanceof BlockHitResult blockHitResult) {
            Optional<PlayerEntity> sleepingPlayer = RevolverItem.findSleepingPlayerOnBed(world, blockHitResult);
            sleepingPlayer.ifPresent(target -> ClientPlayNetworking.send(new KnifeStabPayload(target.getId())));
        }

        cir.setReturnValue(TypedActionResult.success(itemStack));
    }
}
