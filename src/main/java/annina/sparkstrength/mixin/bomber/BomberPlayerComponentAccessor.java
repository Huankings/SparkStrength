package annina.sparkstrength.mixin.bomber;

import org.agmas.noellesroles.bomber.BomberPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.UUID;

/**
 * 访问 NoellesRoles 炸弹客组件内部计时字段。
 *
 * <p>托盘/床陷阱触发时不能直接调用 {@link BomberPlayerComponent#placeBomb}，
 * 因为那个方法会记录“炸弹客直接对玩家安置炸弹”的旧回放。
 * 这里仅用于在 Strength 侧复刻同样的炸弹状态初始化，再由 Strength 写入专属回放。</p>
 */
@Mixin(value = BomberPlayerComponent.class, remap = false)
public interface BomberPlayerComponentAccessor {
    @Accessor(value = "hasBomb", remap = false)
    void sparkstrength$setHasBomb(boolean hasBomb);

    @Accessor(value = "bombTimer", remap = false)
    void sparkstrength$setBombTimer(int bombTimer);

    @Accessor(value = "beepTimer", remap = false)
    void sparkstrength$setBeepTimer(int beepTimer);

    @Accessor(value = "isBeeping", remap = false)
    void sparkstrength$setBeeping(boolean beeping);

    @Accessor(value = "bomberUuid", remap = false)
    void sparkstrength$setBomberUuid(UUID bomberUuid);

    @Accessor(value = "lastDisplayedSeconds", remap = false)
    void sparkstrength$setLastDisplayedSeconds(int lastDisplayedSeconds);
}
