package annina.sparkstrength.compat;

import dev.doctor4t.wathe.util.ShopEntry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * SparkWitch 软兼容桥。
 *
 * <p>SparkStrength 不能在编译期依赖 SparkWitch，否则单独安装任意一方都会变成硬依赖。
 * 这里仅在运行时检测到 sparkwitch 时反射调用它暴露的稳定 compat 门面。</p>
 */
public final class SparkWitchCompat {
    private static final String MOD_ID = "sparkwitch";
    private static final String KIDNAPPER_COMPAT = "dev.caecorthus.sparkwitch.compat.SparkWitchKidnapperCompat";

    private SparkWitchCompat() {
    }

    public static boolean isLoaded() {
        return FabricLoader.getInstance().isModLoaded(MOD_ID);
    }

    public static @Nullable ItemStack createKidnapperDrugStack() {
        Object result = invoke("createKnockoutDrugStack");
        return result instanceof ItemStack stack ? stack : null;
    }

    public static @Nullable ShopEntry createKidnapperDrugShopEntry() {
        Object result = invoke("createKnockoutDrugShopEntry");
        return result instanceof ShopEntry entry ? entry : null;
    }

    public static @Nullable ShopEntry createKidnapperDrugShopEntry(PlayerEntity player) {
        Object result = invoke("createKnockoutDrugShopEntry", new Class<?>[]{PlayerEntity.class}, player);
        return result instanceof ShopEntry entry ? entry : null;
    }

    private static @Nullable Object invoke(String methodName) {
        return invoke(methodName, new Class<?>[0]);
    }

    private static @Nullable Object invoke(String methodName, Class<?>[] parameterTypes, Object... arguments) {
        if (!isLoaded()) {
            return null;
        }
        try {
            Class<?> compat = Class.forName(KIDNAPPER_COMPAT);
            return compat.getMethod(methodName, parameterTypes).invoke(null, arguments);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return null;
        }
    }
}
