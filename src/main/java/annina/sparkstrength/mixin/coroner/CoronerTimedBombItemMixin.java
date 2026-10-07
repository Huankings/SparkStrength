package annina.sparkstrength.mixin.coroner;

import annina.sparkstrength.role.coroner.CoronerService;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.item.TimedBombItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 验尸官伪装炸弹客时允许放置 NoellesRoles 定时炸弹。
 */
@Mixin(TimedBombItem.class)
public abstract class CoronerTimedBombItemMixin {
    @Redirect(
            method = "useOnEntity",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/doctor4t/wathe/cca/GameWorldComponent;isRole(Lnet/minecraft/entity/player/PlayerEntity;Ldev/doctor4t/wathe/api/Role;)Z"
            )
    )
    private boolean sparkstrength$coronerBomberDisguiseCanPlaceBomb(
            GameWorldComponent gameWorld,
            PlayerEntity user,
            Role role
    ) {
        /*
         * 定时炸弹传递逻辑在原方法前半段已经允许携弹者使用；
         * 这里仅把“新放置炸弹”的真实 BOMBER 判断扩展给验尸官炸弹客伪装。
         */
        return gameWorld.isRole(user, role) || CoronerService.hasBomberDisguise(user);
    }
}
