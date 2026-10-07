package annina.sparkstrength.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;

import java.util.List;

/**
 * 电力恢复系统的商店展示物品。
 *
 * <p>它不在背包中执行右键效果，真正的恢复逻辑由工程师商店购买回调触发；
 * 保留独立 Item 只是为了让商店和资源包有稳定图标。</p>
 */
public final class PowerRestorationItem extends Item {
    public PowerRestorationItem(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("item.sparkstrength.power_restoration.tooltip")
                .styled(style -> style.withColor(0x808080).withItalic(false)));
    }
}
