package annina.sparkstrength.mixin.minecraft;

import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/**
 * 访问原版物品冷却管理器的精确字段。
 *
 * <p>解毒剂的冷却写在 {@link ItemCooldownManager} 内部，公开 API 只能整体设置或移除，
 * 不能读取剩余 tick。毒理学家“按中毒人数缩短当前冷却并保留剩余抵扣”需要精确知道
 * 当前剩余时间，所以这里只暴露 entries 与 tick 两个字段。</p>
 */
@Mixin(ItemCooldownManager.class)
public interface ItemCooldownManagerAccessor {
    @Accessor("entries")
    Map<Item, Object> sparkstrength$getEntries();

    @Accessor("tick")
    int sparkstrength$getTick();
}
