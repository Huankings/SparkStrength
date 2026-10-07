package annina.sparkstrength.mixin.wathe;

import annina.sparkstrength.role.shadowjester.ShadowJesterShowdownService;
import com.llamalad7.mixinextras.sugar.Local;
import dev.doctor4t.wathe.util.GunShootPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * 只把新版双影谢幕期间影子小丑左轮的最终冷却改为 4 秒。
 *
 * <p>Wathe 的枪械接收器会在射击流程最后统一设置冷却，这里修改的是该次设置的
 * tick 参数，因此不会改变左轮本身的射击验证、命中判定或普通玩家冷却。</p>
 */
@Mixin(GunShootPayload.Receiver.class)
public abstract class ShadowJesterRevolverCooldownMixin {
    @ModifyArg(
            method = "receive(Ldev/doctor4t/wathe/util/GunShootPayload;Lnet/fabricmc/fabric/api/networking/v1/ServerPlayNetworking$Context;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/ItemCooldownManager;set(Lnet/minecraft/item/Item;I)V"
            ),
            index = 1,
            remap = false
    )
    private int sparkstrength$shortenShowdownRevolverCooldown(
            int originalCooldown,
            @Local(ordinal = 0) ServerPlayerEntity player
    ) {
        return ShadowJesterShowdownService.isShowdownRevolverUser(player)
                ? ShadowJesterShowdownService.REVOLVER_COOLDOWN_TICKS
                : originalCooldown;
    }
}
