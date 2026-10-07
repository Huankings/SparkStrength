package annina.sparkstrength.mixin.wathe;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.HashSet;
import java.util.UUID;

/**
 * 访问 Wathe 的死亡玩家集合。
 *
 * <p>Wathe 只公开了 {@code markPlayerDead(UUID)}，没有公开移除死亡标记的方法。
 * SparkStrength 的调试命令需要把玩家从“非存活”转换回“存活”，因此这里仅精确暴露
 * {@code GameWorldComponent.deadPlayers} 这个集合，不改变 Wathe 原有的存活判定逻辑。</p>
 *
 * <p>这个 accessor 只用于增删 UUID。实际的同步仍由命令在修改后调用
 * {@code GameWorldComponent.sync()} 完成。</p>
 */
@Mixin(value = GameWorldComponent.class, remap = false)
public interface GameWorldComponentAccessor {
    /**
     * 返回 Wathe 保存死亡玩家 UUID 的可变集合。
     * 返回原集合而不是副本，才能让移除 UUID 的结果直接影响 Wathe 的判定。
     */
    @Accessor(value = "deadPlayers", remap = false)
    HashSet<UUID> sparkstrength$getDeadPlayers();
}
