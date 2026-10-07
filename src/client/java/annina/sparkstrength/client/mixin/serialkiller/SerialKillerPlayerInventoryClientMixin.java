package annina.sparkstrength.client.mixin.serialkiller;

import annina.sparkstrength.SparkStrengthItems;
import dev.doctor4t.wathe.cca.PlayerPsychoComponent;
import net.minecraft.entity.player.PlayerInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 客户端滚轮选槽锁定。
 *
 * <p>滚轮切槽由 PlayerInventory.scrollInHotbar 直接修改 selectedSlot，
 * 不经过 MinecraftClient.handleInputEvents 的字段写入点，因此必须在这里
 * 立即取消滚轮切换，避免出现一帧切到其它物品的视觉闪烁。</p>
 */
@Mixin(PlayerInventory.class)
public abstract class SerialKillerPlayerInventoryClientMixin {
    @Inject(method = "scrollInHotbar", at = @At("HEAD"), cancellable = true)
    private void sparkstrength$lockSerialPistolScroll(double scrollAmount, CallbackInfo ci) {
        PlayerInventory inventory = (PlayerInventory) (Object) this;
        if (inventory.player == null || PlayerPsychoComponent.KEY.get(inventory.player).getPsychoTicks() <= 0) {
            return;
        }

        int pistolSlot = findSerialPistolSlot(inventory);
        if (pistolSlot < 0) return;

        // 立即保持主手连环手枪槽，不等待 MinecraftClient.tick 的兜底恢复。
        inventory.selectedSlot = pistolSlot;
        ci.cancel();
    }

    @Unique
    private static int findSerialPistolSlot(PlayerInventory inventory) {
        for (int slot = 0; slot < 9; slot++) {
            if (inventory.getStack(slot).isOf(SparkStrengthItems.serialPistol())) return slot;
        }
        return -1;
    }
}
