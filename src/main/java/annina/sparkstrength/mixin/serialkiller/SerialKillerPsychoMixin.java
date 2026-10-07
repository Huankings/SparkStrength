package annina.sparkstrength.mixin.serialkiller;

import annina.sparkstrength.SparkStrengthItems;
import dev.doctor4t.wathe.api.event.PsychoModeEvents;
import dev.doctor4t.wathe.api.event.PsychoType;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerPsychoComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.Hand;
import org.agmas.noellesroles.Noellesroles;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 旧版 Wathe 没有 PsychoModeProfile；这里仅对连环杀手分流 start/stop，
 * 其它职业继续使用 Wathe 原版球棒疯魔，避免改变全局行为。
 */
@Mixin(PlayerPsychoComponent.class)
public abstract class SerialKillerPsychoMixin {
    @Shadow @Final private PlayerEntity player;
    @Shadow public int psychoTicks;
    @Shadow public int armour;
    @Shadow public abstract void sync();
    @Shadow public abstract void setPsychoTicks(int ticks);

    /**
     * Wathe 原版只会在 clientTick 中把疯魔者切回球棒；连环杀手已经不再持有球棒，
     * 因此必须在组件 tick 中对主手连环手枪做同等的强制选槽。这样即使客户端本地
     * 预测切槽，下一帧也会恢复；服务端也会持续纠正任何绕过普通选槽包的修改。
     */
    @Inject(method = "clientTick", at = @At("HEAD"))
    private void sparkstrength$forceSerialPistolClientSlot(CallbackInfo ci) {
        if (!isSerialKiller(player) || this.psychoTicks <= 0) return;
        int pistolSlot = findSerialPistolSlot(player.getInventory());
        if (pistolSlot >= 0 && player.getInventory().selectedSlot != pistolSlot) {
            player.getInventory().selectedSlot = pistolSlot;
        }
    }

    @Inject(method = "serverTick", at = @At("HEAD"))
    private void sparkstrength$forceSerialPistolServerSlot(CallbackInfo ci) {
        if (!isSerialKiller(player) || this.psychoTicks <= 0) return;
        if (!(player instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer)) return;
        int pistolSlot = findSerialPistolSlot(player.getInventory());
        if (pistolSlot >= 0 && player.getInventory().selectedSlot != pistolSlot) {
            player.getInventory().selectedSlot = pistolSlot;
            serverPlayer.networkHandler.sendPacket(new UpdateSelectedSlotS2CPacket(pistolSlot));
        }
    }

    @Inject(method = "startPsycho(Ldev/doctor4t/wathe/api/event/PsychoType;)Z", at = @At("HEAD"), cancellable = true)
    private void sparkstrength$startSerialKiller(PsychoType type, CallbackInfoReturnable<Boolean> cir) {
        if (player.getWorld().isClient || !isSerialKiller(player)) return;

        // 真实副手必须为空；主手枪必须能放入一个快捷栏空槽，否则不启动疯魔。
        if (!player.getOffHandStack().isEmpty()) {
            cir.setReturnValue(false);
            return;
        }
        int hotbarSlot = findEmptyHotbarSlot(player);
        if (hotbarSlot < 0) {
            cir.setReturnValue(false);
            return;
        }

        player.getInventory().setStack(hotbarSlot, SparkStrengthItems.serialPistol().getDefaultStack());
        player.getInventory().setStack(40, SparkStrengthItems.serialLeftPistol().getDefaultStack());
        player.getInventory().selectedSlot = hotbarSlot;
        this.psychoTicks = dev.doctor4t.wathe.game.GameConstants.PSYCHO_TIMER;
        this.armour = dev.doctor4t.wathe.game.GameConstants.PSYCHO_MODE_ARMOUR;
        setPsychoTicks(this.psychoTicks);
        GameWorldComponent game = GameWorldComponent.KEY.get(player.getWorld());
        if (type.tracksCounter) game.setPsychosActive(game.getPsychosActive() + 1);
        if (player instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer) {
            PsychoModeEvents.ON_PSYCHO_START.invoker().onPsychoStart(serverPlayer, type);
        }
        player.playerScreenHandler.sendContentUpdates();
        // selectedSlot 是玩家实体字段，直接在服务端赋值不会自动产生快捷栏同步包。
        // 商店启动疯魔时必须主动通知客户端切到连环手枪，否则客户端仍停留在购买前的物品。
        if (player instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer) {
            serverPlayer.networkHandler.sendPacket(new UpdateSelectedSlotS2CPacket(hotbarSlot));
        }
        cir.setReturnValue(true);
    }

    @Inject(method = "stopPsycho", at = @At("HEAD"))
    private void sparkstrength$removeSerialKillerWeapons(CallbackInfo ci) {
        if (!isSerialKiller(player)) return;
        player.getInventory().remove(stack -> stack.isOf(SparkStrengthItems.serialPistol())
                || stack.isOf(SparkStrengthItems.serialLeftPistol()), Integer.MAX_VALUE,
                player.playerScreenHandler.getCraftingInput());
        if (player.getOffHandStack().isOf(SparkStrengthItems.serialLeftPistol())) {
            player.setStackInHand(Hand.OFF_HAND, ItemStack.EMPTY);
        }
    }

    @Unique private static boolean isSerialKiller(PlayerEntity player) {
        return GameWorldComponent.KEY.get(player.getWorld()).isRole(player, Noellesroles.SERIAL_KILLER);
    }

    @Unique private static int findEmptyHotbarSlot(PlayerEntity player) {
        for (int slot = 0; slot < 9; slot++) if (player.getInventory().getStack(slot).isEmpty()) return slot;
        return -1;
    }

    @Unique private static int findSerialPistolSlot(PlayerInventory inventory) {
        for (int slot = 0; slot < 9; slot++) {
            if (inventory.getStack(slot).isOf(SparkStrengthItems.serialPistol())) return slot;
        }
        return -1;
    }
}
