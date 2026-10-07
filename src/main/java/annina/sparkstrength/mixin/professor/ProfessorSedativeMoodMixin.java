package annina.sparkstrength.mixin.professor;

import annina.sparkstrength.component.professor.ProfessorSerumTargetComponent;
import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * 镇静试剂：只阻止“任务未完成导致的心情扣减”。
 *
 * <p>PlayerMoodComponent 里完成任务加心情、精神崩溃判定、任务生成都还有自己的逻辑。
 * 这里精准改写两处 setMood(mood - drain) 的参数，让镇静期间传回当前 mood，
 * 因此不会影响完成任务后的心情回复，也不会把其它角色的心情规则改坏。</p>
 */
@Mixin(PlayerMoodComponent.class)
public abstract class ProfessorSedativeMoodMixin {
    @Shadow @Final private PlayerEntity player;

    @Shadow public abstract float getMood();

    @ModifyArg(
            method = "serverTick",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/doctor4t/wathe/cca/PlayerMoodComponent;setMood(F)V",
                    ordinal = 0
            ),
            index = 0
    )
    private float sparkstrength$stopServerMoodDrain(float moodAfterDrain) {
        return ProfessorSerumTargetComponent.KEY.get(player).hasSedative() ? getMood() : moodAfterDrain;
    }

    @ModifyArg(
            method = "clientTick",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/doctor4t/wathe/cca/PlayerMoodComponent;setMood(F)V"
            ),
            index = 0
    )
    private float sparkstrength$stopClientMoodDrain(float moodAfterDrain) {
        return ProfessorSerumTargetComponent.KEY.get(player).hasSedative() ? getMood() : moodAfterDrain;
    }
}
