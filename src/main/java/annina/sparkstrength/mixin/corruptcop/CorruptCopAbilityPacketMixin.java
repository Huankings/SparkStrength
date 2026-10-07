package annina.sparkstrength.mixin.corruptcop;

import annina.sparkstrength.role.corruptcop.CorruptCopAbilityService;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.packet.AbilityC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Reuses NoellesRoles' shared ability packet without adding a SparkStrength payload id.
 * 复用 NoellesRoles 的通用技能包，不新增 SparkStrength 数据包 id。
 */
@Mixin(value = Noellesroles.class, remap = false)
public abstract class CorruptCopAbilityPacketMixin {
    // Two authorized NoellesRoles 1.7.6 builds exist: SparkStrength's historical jar uses $36,
    // while the shared SparkTraits/SparkWitch runtime baseline uses $5 for the same packet handler.
    // The group requires exactly one binary-specific seam to resolve at runtime.
    // 两个已授权的 NoellesRoles 1.7.6 构建分别在 $36 与 $5 处理同一个技能包；运行时必须且只能命中一个。
    @Group(name = "sparkstrength$corruptCopAbility", min = 1, max = 1)
    @Inject(
            method = "lambda$registerPackets$36(Lorg/agmas/noellesroles/packet/AbilityC2SPacket;Lnet/fabricmc/fabric/api/networking/v1/ServerPlayNetworking$Context;)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0,
            remap = false
    )
    private static void sparkstrength$toggleCorruptCopAbilityHistorical(
            AbilityC2SPacket payload,
            ServerPlayNetworking.Context context,
            CallbackInfo ci
    ) {
        sparkstrength$toggleCorruptCopAbility(context, ci);
    }

    @Group(name = "sparkstrength$corruptCopAbility", min = 1, max = 1)
    @Inject(
            method = "lambda$registerPackets$5(Lorg/agmas/noellesroles/packet/AbilityC2SPacket;Lnet/fabricmc/fabric/api/networking/v1/ServerPlayNetworking$Context;)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0,
            remap = false
    )
    private static void sparkstrength$toggleCorruptCopAbilityShared(
            AbilityC2SPacket payload,
            ServerPlayNetworking.Context context,
            CallbackInfo ci
    ) {
        sparkstrength$toggleCorruptCopAbility(context, ci);
    }

    @Unique
    private static void sparkstrength$toggleCorruptCopAbility(
            ServerPlayNetworking.Context context,
            CallbackInfo ci
    ) {
        if (CorruptCopAbilityService.toggle(context.player())) {
            ci.cancel();
        }
    }
}
