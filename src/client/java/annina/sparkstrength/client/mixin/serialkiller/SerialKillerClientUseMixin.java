package annina.sparkstrength.client.mixin.serialkiller;

import annina.sparkstrength.SparkStrengthItems;
import dev.doctor4t.wathe.cca.PlayerPsychoComponent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Minecraft 默认右键会先处理实体/方块交互，再调用物品 use。主手枪进入冷却时，
 * 只有前面的交互都没有接受，才尝试真正的副手枪，避免右键开门等操作被副手射击抢走。
 */
@Mixin(MinecraftClient.class)
public abstract class SerialKillerClientUseMixin {
    @Shadow public ClientPlayerEntity player;

    @Inject(
            method = "doItemUse",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;interactItem(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Hand;)Lnet/minecraft/util/ActionResult;",
                    ordinal = 0
            ),
            cancellable = true
    )
    private void sparkstrength$useOffhandAfterNormalInteraction(CallbackInfo ci) {
        if (player == null || PlayerPsychoComponent.KEY.get(player).getPsychoTicks() <= 0) return;
        ItemStack main = player.getMainHandStack();
        ItemStack off = player.getOffHandStack();
        if (!main.isOf(SparkStrengthItems.serialPistol()) || !off.isOf(SparkStrengthItems.serialLeftPistol())) return;
        if (!player.getItemCooldownManager().isCoolingDown(main.getItem())) return;
        // 能走到 interactItem 说明实体/方块交互没有接受；此时才把同一次右键交给副手枪。
        MinecraftClient client = (MinecraftClient) (Object) this;
        client.interactionManager.interactItem(player, Hand.OFF_HAND);
        ci.cancel();
    }
}
