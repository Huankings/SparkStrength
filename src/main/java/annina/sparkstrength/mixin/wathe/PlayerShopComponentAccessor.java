package annina.sparkstrength.mixin.wathe;

import dev.doctor4t.wathe.cca.PlayerShopComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/**
 * Exact Adapter for Wathe's private {@code PlayerShopComponent.cooldowns} field.
 * Wathe 私有 {@code PlayerShopComponent.cooldowns} 字段的精确 Adapter。
 *
 * <p>Do not broaden this accessor to unrelated shop state.
 * 不要把此 accessor 扩展到无关商店状态。</p>
 */
@Mixin(value = PlayerShopComponent.class, remap = false)
public interface PlayerShopComponentAccessor {
    @Accessor(value = "cooldowns", remap = false)
    Map<String, Integer> sparkstrength$getCooldowns();
}
