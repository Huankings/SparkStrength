package annina.sparkstrength.role.engineer;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 工程师捕捉报告的内部标记工具。
 *
 * <p>报告本体仍然是普通纸，显示名和 lore 可以走语言文件；真正用于“识别旧报告、隐藏报告”
 * 的信息放在 CUSTOM_DATA 里。这样后续改报告标题、翻译文本或 lore 格式时，不会影响清理逻辑。</p>
 */
public final class EngineerCaptureReport {
    private static final String ROOT_KEY = "SparkStrengthEngineerCaptureReport";
    private static final String OWNER_KEY = "Owner";

    private EngineerCaptureReport() {
    }

    public static void mark(ItemStack stack, UUID ownerUuid) {
        NbtComponent component = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        NbtCompound data = component.copyNbt();

        NbtCompound root = new NbtCompound();
        root.putUuid(OWNER_KEY, ownerUuid);
        data.put(ROOT_KEY, root);
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(data));
    }

    public static boolean isCaptureReport(ItemStack stack) {
        return !stack.isEmpty()
                && stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT)
                .copyNbt()
                .contains(ROOT_KEY, NbtElement.COMPOUND_TYPE);
    }

    public static @Nullable UUID ownerUuid(ItemStack stack) {
        NbtCompound data = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
        if (!data.contains(ROOT_KEY, NbtElement.COMPOUND_TYPE)) {
            return null;
        }

        NbtCompound root = data.getCompound(ROOT_KEY);
        return root.containsUuid(OWNER_KEY) ? root.getUuid(OWNER_KEY) : null;
    }

    public static void removeOldReports(PlayerEntity player) {
        for (int slot = 0; slot < player.getInventory().size(); slot++) {
            ItemStack stack = player.getInventory().getStack(slot);
            if (isCaptureReport(stack)) {
                // 每次生成新报告前清理旧报告，避免玩家背包里堆积多份过期检测结果。
                player.getInventory().setStack(slot, ItemStack.EMPTY);
            }
        }
    }
}
