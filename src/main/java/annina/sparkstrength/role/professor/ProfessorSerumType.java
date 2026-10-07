package annina.sparkstrength.role.professor;

import annina.sparkstrength.SparkStrengthItems;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * 教授四种试剂的统一枚举。
 *
 * <p>背包按钮、网络包、商店和物品右键都会引用这里的同一组类型，
 * 这样后续如果要改顺序、改 ID 或增加新试剂，不会出现多个地方写法不一致。</p>
 */
public enum ProfessorSerumType {
    SEDATIVE("sedative"),
    DOORPASSING("doorpassing_potion"),
    INVISIBILITY("invisibility_serum"),
    TRUTH("truth_serum");

    /** 背包二级界面要求的固定按钮顺序：镇静、穿门、隐身、吐真。 */
    public static final List<ProfessorSerumType> BACKPACK_ORDER = List.of(
            SEDATIVE,
            DOORPASSING,
            INVISIBILITY,
            TRUTH
    );

    private final String id;

    ProfessorSerumType(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public Item item() {
        return switch (this) {
            case SEDATIVE -> SparkStrengthItems.sedative();
            case DOORPASSING -> SparkStrengthItems.doorpassingPotion();
            case INVISIBILITY -> SparkStrengthItems.invisibilitySerum();
            case TRUTH -> SparkStrengthItems.truthSerum();
        };
    }

    public ItemStack defaultStack() {
        return item().getDefaultStack();
    }

    public static Optional<ProfessorSerumType> byId(String id) {
        return Arrays.stream(values())
                .filter(type -> type.id.equals(id))
                .findFirst();
    }

    public static @Nullable ProfessorSerumType byItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        for (ProfessorSerumType type : values()) {
            if (stack.isOf(type.item())) {
                return type;
            }
        }
        return null;
    }

    public static boolean isSerum(ItemStack stack) {
        return byItem(stack) != null;
    }
}
