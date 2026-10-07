package annina.sparkstrength.client.mixin.serialkiller;

import annina.sparkstrength.SparkStrengthItems;
import dev.doctor4t.wathe.cca.PlayerPsychoComponent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** 客户端快捷栏锁定，拦截数字键/滚轮最终写入 selectedSlot 的位置。 */
@Mixin(MinecraftClient.class)
public abstract class SerialKillerClientSlotLockMixin {
    /**
     * 客户端快捷栏切换存在本地预测：即使服务端拒绝 UpdateSelectedSlotC2SPacket，
     * 客户端仍可能已经把 selectedSlot 改掉。因此每 tick 强制恢复到连环手枪槽，
     * 作为数字键、滚轮和其它客户端输入路径之外的最终兜底。
     */
    @org.spongepowered.asm.mixin.injection.Inject(method = "tick", at = @org.spongepowered.asm.mixin.injection.At("HEAD"))
    private void sparkstrength$restoreSerialKillerSlot(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        MinecraftClient client = (MinecraftClient) (Object) this;
        PlayerEntity player = client.player;
        if (player == null || PlayerPsychoComponent.KEY.get(player).getPsychoTicks() <= 0) return;
        PlayerInventory inventory = player.getInventory();
        int pistolSlot = findSerialPistolSlot(inventory);
        if (pistolSlot >= 0 && inventory.selectedSlot != pistolSlot) {
            inventory.selectedSlot = pistolSlot;
        }
    }

    @Redirect(method = "handleInputEvents", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/PlayerInventory;selectedSlot:I", opcode = org.objectweb.asm.Opcodes.PUTFIELD))
    private void sparkstrength$lockSlot(PlayerInventory inventory, int value) {
        if (inventory.player != null
                && PlayerPsychoComponent.KEY.get(inventory.player).getPsychoTicks() > 0) {
            int pistolSlot = findSerialPistolSlot(inventory);
            if (pistolSlot >= 0 && value != pistolSlot) {
                // 数字键写槽也必须立即回到主手连环手枪，不依赖下一 tick 恢复。
                inventory.selectedSlot = pistolSlot;
                return;
            }
        }
        inventory.selectedSlot = value;
    }

    private static int findSerialPistolSlot(PlayerInventory inventory) {
        for (int slot = 0; slot < 9; slot++) {
            if (inventory.getStack(slot).isOf(SparkStrengthItems.serialPistol())) return slot;
        }
        return -1;
    }
}
