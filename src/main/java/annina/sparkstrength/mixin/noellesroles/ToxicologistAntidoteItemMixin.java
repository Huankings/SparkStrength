package annina.sparkstrength.mixin.noellesroles;

import annina.sparkstrength.role.toxicologist.ToxicologistAntidoteService;
import annina.sparkstrength.role.coroner.CoronerService;
import dev.doctor4t.wathe.cca.PlayerPoisonComponent;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.agmas.noellesroles.item.AntidoteItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

/**
 * 接入 NoellesRoles 解毒剂的成功治疗结果。
 *
 * <p>NoellesRoles 的解毒剂没有事件钩子，成功路径是在 finishUsing 里直接 reset 毒组件并设置冷却。
 * 这里在方法开头记录“目标治疗前确实中毒”，再在方法尾部确认毒已被清掉，避免治疗失败、
 * 距离过远、目标不存在或非中毒目标也触发毒理学家奖励和冷却抵扣。</p>
 */
@Mixin(AntidoteItem.class)
public abstract class ToxicologistAntidoteItemMixin {
    @Unique
    private static final ThreadLocal<Boolean> SPARKSTRENGTH_CURED_POISONED_TARGET =
            ThreadLocal.withInitial(() -> false);
    @Unique
    private static final ThreadLocal<UUID> SPARKSTRENGTH_CURE_TARGET =
            new ThreadLocal<>();

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void sparkstrength$coronerOnlyUsesAntidoteWhileToxicologist(
            World world,
            net.minecraft.entity.player.PlayerEntity user,
            Hand hand,
            CallbackInfoReturnable<TypedActionResult<ItemStack>> cir
    ) {
        if (!CoronerService.canUseToxicologistDisguiseItem(user)) {
            cir.setReturnValue(TypedActionResult.pass(user.getStackInHand(hand)));
        }
    }

    @Inject(method = "useOnEntity", at = @At("HEAD"), cancellable = true)
    private void sparkstrength$coronerOnlyUsesAntidoteOnEntityWhileToxicologist(
            ItemStack stack,
            net.minecraft.entity.player.PlayerEntity user,
            LivingEntity entity,
            Hand hand,
            CallbackInfoReturnable<ActionResult> cir
    ) {
        if (!CoronerService.canUseToxicologistDisguiseItem(user)) {
            cir.setReturnValue(ActionResult.PASS);
        }
    }

    @Inject(method = "finishUsing", at = @At("HEAD"))
    private void sparkstrength$capturePoisonedTargetBeforeCure(
            ItemStack stack,
            World world,
            LivingEntity user,
            CallbackInfoReturnable<ItemStack> cir
    ) {
        SPARKSTRENGTH_CURED_POISONED_TARGET.set(false);
        SPARKSTRENGTH_CURE_TARGET.remove();
        if (world.isClient || !(user instanceof ServerPlayerEntity)) {
            return;
        }

        UUID targetUuid = sparkstrength$getTargetUuid(stack);
        if (targetUuid == null || !(world.getPlayerByUuid(targetUuid) instanceof ServerPlayerEntity target)) {
            return;
        }

        PlayerPoisonComponent poisonComponent = PlayerPoisonComponent.KEY.get(target);
        SPARKSTRENGTH_CURED_POISONED_TARGET.set(poisonComponent.poisonTicks > 0);
        if (poisonComponent.poisonTicks > 0) {
            SPARKSTRENGTH_CURE_TARGET.set(targetUuid);
        }
    }

    @Inject(method = "finishUsing", at = @At("RETURN"))
    private void sparkstrength$rewardToxicologistAfterCure(
            ItemStack stack,
            World world,
            LivingEntity user,
            CallbackInfoReturnable<ItemStack> cir
    ) {
        try {
            if (world.isClient || !(user instanceof ServerPlayerEntity serverUser)) {
                return;
            }
            if (!SPARKSTRENGTH_CURED_POISONED_TARGET.get()) {
                return;
            }
            UUID targetUuid = SPARKSTRENGTH_CURE_TARGET.get();
            if (targetUuid == null || !(world.getPlayerByUuid(targetUuid) instanceof ServerPlayerEntity target)) {
                return;
            }

            PlayerPoisonComponent poisonComponent = PlayerPoisonComponent.KEY.get(target);
            if (poisonComponent.poisonTicks > 0) {
                return;
            }

            ToxicologistAntidoteService.afterAntidoteCure(serverUser);
        } finally {
            SPARKSTRENGTH_CURED_POISONED_TARGET.remove();
            SPARKSTRENGTH_CURE_TARGET.remove();
        }
    }

    @Unique
    private static UUID sparkstrength$getTargetUuid(ItemStack stack) {
        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData == null) {
            return null;
        }

        NbtCompound nbt = customData.copyNbt();
        return nbt.containsUuid("target") ? nbt.getUuid("target") : null;
    }
}
