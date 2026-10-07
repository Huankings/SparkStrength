package annina.sparkstrength.mixin.noellesroles;

import annina.sparkstrength.role.shadowjester.ShadowJesterShowdownService;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.shadowjester.ShadowJesterPlayerComponent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 在 NoellesRoles 影子小丑结盟状态真正写入组件后补充 SparkStrength 的强化内容。
 *
 * <p>旧实现注入了 NoellesRoles 编译器生成的
 * {@code lambda$registerPackets$42}。这个编号会随着 NoellesRoles 的其他网络包注册顺序
 * 改变，导致不同构建版本在启动时出现“找不到注入目标”的致命错误。
 * 现在改为注入稳定的 {@code ShadowJesterPlayerComponent#setAllied(boolean)}，
 * 只观察结盟状态变化，不参与原有握手判定。</p>
 */
@Mixin(value = ShadowJesterPlayerComponent.class, remap = false)
public abstract class ShadowJesterAllianceMixin {
    @Inject(
            method = "setAllied(Z)V",
            at = @At("TAIL"),
            remap = false
    )
    private void sparkstrength$enhanceAlliance(
            boolean allied,
            CallbackInfo ci
    ) {
        // 该方法注入在实例方法上，下面通过 Shadow 字段取得发生状态变化的玩家。
        if (allied && player instanceof ServerPlayerEntity serverPlayer) {
            ShadowJesterShowdownService.enhanceAllianceAfterStateChange(serverPlayer);
        }
    }

    /**
     * NoellesRoles 组件持有的归属玩家。
     *
     * <p>这是组件的稳定实例字段，用于把状态变化关联回服务端玩家；
     * 不依赖网络包接收器或编译器生成的 lambda 方法。</p>
     */
    @Shadow
    @Final
    private PlayerEntity player;
}
