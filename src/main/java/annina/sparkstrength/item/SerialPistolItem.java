package annina.sparkstrength.item;

import annina.sparkstrength.SparkStrengthItems;
import annina.sparkstrength.role.serialkiller.SerialKillerConstants;
import annina.sparkstrength.network.SerialPistolShootC2SPayload;
import dev.doctor4t.wathe.item.RevolverItem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.world.World;

import java.lang.reflect.Method;

/**
 * 连环杀手专用枪械。继承 Wathe 左轮以复用第三人称枪械姿势和枪械模型，
 * 但开火包通过反射发送，保证 main 源集不硬依赖 Fabric client networking。
 */
public final class SerialPistolItem extends RevolverItem {
    private final boolean leftHand;

    public SerialPistolItem(Settings settings, boolean leftHand) {
        super(settings);
        this.leftHand = leftHand;
    }

    public boolean isLeftHand() {
        return leftHand;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world.isClient) {
            EntityHitResult target = findTarget(user);
            sendShootPacket(new SerialPistolShootC2SPayload(target == null ? -1 : target.getEntity().getId(), hand));
            user.setPitch(user.getPitch() - 4.0F);
            // Wathe 的粒子类只在客户端分支加载；调用方式与左轮一致。
            RevolverItem.spawnHandParticle();
        }
        return TypedActionResult.consume(stack);
    }

    public static EntityHitResult findTarget(PlayerEntity user) {
        HitResult result = ProjectileUtil.getCollision(
                user,
                entity -> entity instanceof PlayerEntity target
                        && target != user
                        && dev.doctor4t.wathe.game.GameFunctions.isPlayerAliveAndSurvival(target),
                SerialKillerConstants.PISTOL_RANGE_BLOCKS
        );
        return result instanceof EntityHitResult entityHitResult ? entityHitResult : null;
    }

    private static void sendShootPacket(SerialPistolShootC2SPayload payload) {
        try {
            Class<?> networking = Class.forName("net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking");
            Method send = networking.getMethod("send", net.minecraft.network.packet.CustomPayload.class);
            send.invoke(null, payload);
        } catch (ReflectiveOperationException ignored) {
            // Dedicated server /未加载 client 源集时不应因网络类缺失而崩溃。
        }
    }
}
