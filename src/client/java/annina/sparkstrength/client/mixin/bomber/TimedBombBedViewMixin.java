package annina.sparkstrength.client.mixin.bomber;

import annina.sparkstrength.role.bomber.TimedBombTrapHolder;
import annina.sparkstrength.role.coroner.CoronerService;
import dev.doctor4t.wathe.block_entity.TrimmedBedBlockEntity;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 床中预埋定时炸弹的客户端提示粒子。
 */
@Mixin(TrimmedBedBlockEntity.class)
public abstract class TimedBombBedViewMixin {
    @Inject(method = "clientTick", at = @At("HEAD"), remap = false)
    private static void sparkstrength$renderTimedBombBed(
            World world,
            BlockPos pos,
            BlockState state,
            BlockEntity blockEntity,
            CallbackInfo ci
    ) {
        if (!(blockEntity instanceof TimedBombTrapHolder trap) || !trap.sparkstrength$hasTimedBombTrap()) {
            return;
        }
        if (!sparkstrength$canSeeTimedBombTrap(world)) {
            return;
        }
        if (world.getRandom().nextBetween(0, 20) < 17) {
            return;
        }

        world.addParticle(
                ParticleTypes.SMOKE,
                pos.getX() + 0.5F,
                pos.getY() + 0.65F,
                pos.getZ() + 0.5F,
                0.0F,
                0.04F,
                0.0F
        );
    }

    private static boolean sparkstrength$canSeeTimedBombTrap(World world) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return false;
        }

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(world);
        return gameWorld.canUseKillerFeatures(client.player) || CoronerService.hasBomberDisguise(client.player);
    }
}
