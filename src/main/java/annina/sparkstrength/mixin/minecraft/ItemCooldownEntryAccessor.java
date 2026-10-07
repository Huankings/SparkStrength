package annina.sparkstrength.mixin.minecraft;

import net.minecraft.entity.player.ItemCooldownManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 读取单个物品冷却条目的结束 tick。
 *
 * <p>毒理学家冷却抵扣只需要知道“还剩多久”，不直接改冷却条目本身；
 * 缩短后的冷却通过 {@link ItemCooldownManager#set} 重新写入。</p>
 */
@Mixin(targets = "net.minecraft.entity.player.ItemCooldownManager$Entry")
public interface ItemCooldownEntryAccessor {
    @Accessor("endTick")
    int sparkstrength$getEndTick();
}
