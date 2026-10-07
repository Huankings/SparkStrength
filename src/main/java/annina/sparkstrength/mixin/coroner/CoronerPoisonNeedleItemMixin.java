package annina.sparkstrength.mixin.coroner;

import annina.sparkstrength.role.coroner.CoronerService;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.item.PoisonNeedleItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 验尸官伪装毒师时允许使用 NoellesRoles 的毒针。
 */
@Mixin(PoisonNeedleItem.class)
public abstract class CoronerPoisonNeedleItemMixin {
    @Redirect(
            method = "useOnEntity",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/doctor4t/wathe/cca/GameWorldComponent;isRole(Lnet/minecraft/entity/player/PlayerEntity;Ldev/doctor4t/wathe/api/Role;)Z"
            )
    )
    private boolean sparkstrength$coronerPoisonerDisguiseCanUseNeedle(
            GameWorldComponent gameWorld,
            PlayerEntity user,
            Role role
    ) {
        /*
         * 原毒针只允许真实 POISONER 使用。
         * 验尸官从毒师尸体获得的是“尸体身份变形”，因此这里把当前有效毒师伪装作为额外豁免；
         * 高优先级变形怪效果覆盖验尸官时，CoronerService 会返回 false，毒针也随之失效。
         */
        return gameWorld.isRole(user, role) || CoronerService.hasPoisonerDisguise(user);
    }
}
