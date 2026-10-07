package annina.sparkstrength.mixin.serialkiller;

import annina.sparkstrength.SparkStrengthItems;
import dev.doctor4t.wathe.cca.PlayerPsychoComponent;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import org.agmas.noellesroles.Noellesroles;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 服务端兜底：疯魔期间不能通过伪造选槽包切走连环手枪。 */
@Mixin(ServerPlayNetworkHandler.class)
public abstract class SerialKillerServerSlotLockMixin {
    @Shadow @Final public net.minecraft.server.network.ServerPlayerEntity player;

    @Inject(method = "onUpdateSelectedSlot", at = @At("HEAD"), cancellable = true)
    private void sparkstrength$lockSerialKillerSlot(UpdateSelectedSlotC2SPacket packet, CallbackInfo ci) {
        PlayerInventory inventory = player.getInventory();
        if (!GameWorldComponent.KEY.get(player.getWorld()).isRole(player, Noellesroles.SERIAL_KILLER)
                || PlayerPsychoComponent.KEY.get(player).getPsychoTicks() <= 0) {
            return;
        }

        int pistolSlot = findSerialPistolSlot(inventory);
        if (pistolSlot < 0) return;

        // 服务端以连环手枪所在槽为唯一合法主手槽，不依赖客户端当前 selectedSlot 是否已经同步。
        if (inventory.selectedSlot != pistolSlot) {
            inventory.selectedSlot = pistolSlot;
        }
        if (packet.getSelectedSlot() != pistolSlot) {
            ci.cancel();
            player.networkHandler.sendPacket(new UpdateSelectedSlotS2CPacket(pistolSlot));
        }
    }

    private static int findSerialPistolSlot(PlayerInventory inventory) {
        for (int slot = 0; slot < 9; slot++) {
            if (inventory.getStack(slot).isOf(SparkStrengthItems.serialPistol())) return slot;
        }
        return -1;
    }
}
