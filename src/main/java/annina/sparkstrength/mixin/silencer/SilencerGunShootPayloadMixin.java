package annina.sparkstrength.mixin.silencer;

import annina.sparkstrength.role.silencer.SilencerQuietService;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.doctor4t.wathe.index.WatheSounds;
import dev.doctor4t.wathe.util.GunShootPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 静语者使用 Wathe 左轮时不播放 click / shoot 音效。
 *
 * <p>用户确认只静音 {@code WatheItems.REVOLVER}，因此这里仍放行 derringer
 * 和其它复用 {@link GunShootPayload} 的枪械声音。</p>
 */
@Mixin(GunShootPayload.Receiver.class)
public abstract class SilencerGunShootPayloadMixin {
    @WrapOperation(
            method = "receive",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;playSound(Lnet/minecraft/entity/player/PlayerEntity;DDDLnet/minecraft/sound/SoundEvent;Lnet/minecraft/sound/SoundCategory;FF)V"
            )
    )
    private void sparkstrength$silencerQuietRevolverSound(
            World world,
            PlayerEntity source,
            double x,
            double y,
            double z,
            SoundEvent sound,
            SoundCategory category,
            float volume,
            float pitch,
            Operation<Void> original,
            GunShootPayload payload,
            ServerPlayNetworking.Context context
    ) {
        if (SilencerQuietService.shouldSilenceWatheRevolver(context.player())
                && (WatheSounds.ITEM_REVOLVER_CLICK.equals(sound)
                || WatheSounds.ITEM_REVOLVER_SHOOT.equals(sound))) {
            return;
        }

        original.call(world, source, x, y, z, sound, category, volume, pitch);
    }
}
