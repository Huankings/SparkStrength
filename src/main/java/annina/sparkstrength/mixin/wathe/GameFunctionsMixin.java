package annina.sparkstrength.mixin.wathe;

import annina.sparkstrength.role.coroner.CoronerService;
import com.llamalad7.mixinextras.sugar.Local;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 给 Wathe 玩家尸体补写验尸官需要的死亡身份快照。
 *
 * <p>这里不改 Wathe 的 PlayerBodyEntity 字段，而是把额外数据写入 SparkStrength 自己的 CCA 组件。
 * 采尸袋之后只读这份快照，避免死者在线身份发生变化时污染尸体身份。</p>
 */
@Mixin(GameFunctions.class)
public abstract class GameFunctionsMixin {
    @Inject(
            method = "killPlayer(Lnet/minecraft/server/network/ServerPlayerEntity;ZLnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/util/Identifier;Z)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;spawnEntity(Lnet/minecraft/entity/Entity;)Z",
                    shift = At.Shift.BEFORE
            )
    )
    private static void sparkstrength$coronerRecordBodySnapshot(
            ServerPlayerEntity victim,
            boolean spawnBody,
            @Nullable ServerPlayerEntity killer,
            Identifier deathReason,
            boolean force,
            CallbackInfo ci,
            @Local PlayerBodyEntity body
    ) {
        CoronerService.recordBodySnapshot(victim, body);
        CoronerService.recordDisguisedBodyIfNeeded(victim);
        CoronerService.applyNoisemakerBodyDeathEffects(victim, body);
    }

    @Inject(method = "shouldDropOnDeath", at = @At("HEAD"), cancellable = true)
    private static void sparkstrength$coronerTemporaryGrantsDoNotDrop(
            ItemStack stack,
            PlayerEntity victim,
            CallbackInfoReturnable<Boolean> cir
    ) {
        /*
         * 验尸官伪装身份时发放的刀、枪、钥匙都是“变形附带的临时装备”。
         * Wathe 默认会让左轮等物品死亡掉落，所以这里先把带有验尸官临时标记的物品从掉落规则中排除，
         * 真正的清空仍交给 CoronerService.afterKill/ResetPlayer，避免临时装备掉到地上被其他玩家捡走。
         */
        if (CoronerService.isTemporaryGrant(stack)) {
            cir.setReturnValue(false);
        }
    }
}
