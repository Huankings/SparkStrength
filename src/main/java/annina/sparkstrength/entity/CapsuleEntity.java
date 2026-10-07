package annina.sparkstrength.entity;

import annina.sparkstrength.SparkStrengthEntities;
import annina.sparkstrength.SparkStrengthItems;
import annina.sparkstrength.item.CapsuleItem;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;

/**
 * Snowball-like capsule projectile.
 * 类似雪球的胶囊投掷物。
 */
public final class CapsuleEntity extends ThrownItemEntity {
    public CapsuleEntity(EntityType<? extends CapsuleEntity> entityType, World world) {
        super(entityType, world);
    }

    public CapsuleEntity(World world, LivingEntity owner) {
        super(SparkStrengthEntities.capsule(), owner, world);
    }

    @Override
    protected Item getDefaultItem() {
        return SparkStrengthItems.capsule();
    }

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        super.onEntityHit(entityHitResult);
        if (!(getWorld().isClient()) && entityHitResult.getEntity() instanceof ServerPlayerEntity target) {
            ItemStack contents = CapsuleItem.getContents(getStack(), target.getRegistryManager()).copy();
            CapsuleItem.forceConsume(target, contents);
        }
    }

    @Override
    protected void onCollision(HitResult hitResult) {
        super.onCollision(hitResult);
        if (!getWorld().isClient()) {
            discard();
        }
    }
}
