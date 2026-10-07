package annina.sparkstrength.mixin.corruptcop;

import annina.sparkstrength.component.corruptcop.CorruptCopAbilityComponent;
import annina.sparkstrength.role.corruptcop.CorruptCopRules;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds the Corrupt Cop lateral bonus after vanilla normalizes movement input.
 * 在原版完成移动输入归一化后补上黑警的横移加成。
 */
@Mixin(Entity.class)
public abstract class CorruptCopLateralVelocityMixin {
    @Inject(method = "updateVelocity", at = @At("TAIL"))
    private void sparkstrength$addCorruptCopLateralVelocity(float speed, Vec3d movementInput, CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;
        if (!(entity instanceof PlayerEntity player)) {
            return;
        }

        Role role = GameWorldComponent.KEY.get(player.getWorld()).getRole(player);
        Vec3d bonus = CorruptCopRules.lateralVelocityBonus(
                movementInput,
                speed,
                player.getYaw(),
                CorruptCopRules.isCorruptCop(role),
                CorruptCopAbilityComponent.KEY.get(player).isActive(),
                GameFunctions.isPlayerAliveAndSurvival(player)
        );
        if (bonus.lengthSquared() > 0.0D) {
            player.setVelocity(player.getVelocity().add(bonus));
        }
    }
}
