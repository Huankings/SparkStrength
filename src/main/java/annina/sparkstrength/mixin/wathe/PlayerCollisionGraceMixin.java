package annina.sparkstrength.mixin.wathe;

import annina.sparkstrength.component.collision.PlayerCollisionGraceWorldComponent;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;

/**
 * 为 Wathe 的玩家实体碰撞增加开局保护期。
 *
 * <p>Wathe Spark 版本会在 {@code Entity#collidesWith} 中把运行中玩家之间的结果强制改为 true，
 * 让玩家互相充当实体墙。本 Mixin 包住该方法的最终计算结果，检查 SparkStrength 的世界级保护状态，
 * 只把这段时间内“原本会碰撞的玩家对”改回 false。</p>
 *
 * <p>这里使用优先级高于 Wathe 的 {@code WrapMethod}，直接包住 Wathe 的最终结果。
 * 这样不会出现“内层注入已经返回 false，但 Wathe 外层包装随后又直接返回 true”的调用顺序问题。
 * 如果 NoellesRoles、SparkFactionAPI、SparkTraits 或 SparkWitch 已经返回 false，
 * 本包装会原样保留；保护期只取消 Wathe 的额外实体墙，不拦截方块碰撞，也不修改
 * LivingEntity 的原版轻微推挤流程。</p>
 */
@Mixin(value = Entity.class, priority = 1200)
public abstract class PlayerCollisionGraceMixin {
    @WrapMethod(method = "collidesWith(Lnet/minecraft/entity/Entity;)Z")
    private boolean sparkstrength$disableWathePlayerWallDuringOpeningGrace(
            Entity other,
            Operation<Boolean> original
    ) {
        boolean originalResult = original.call(other);
        Entity self = (Entity) (Object) this;
        if (!(self instanceof PlayerEntity selfPlayer) || !(other instanceof PlayerEntity otherPlayer)) {
            return originalResult;
        }
        if (selfPlayer.getWorld() != otherPlayer.getWorld() || !originalResult) {
            return originalResult;
        }

        // 世界组件同时在服务端和客户端参与判断，保证服务端权威碰撞与客户端移动预测一致。
        if (PlayerCollisionGraceWorldComponent.KEY.get(selfPlayer.getWorld()).isGracePeriodActive()) {
            // 只取消 Wathe 玩家实体墙；LivingEntity 的 pushAway 不在这里修改，因此保留原版轻推挤。
            return false;
        }
        return originalResult;
    }
}
