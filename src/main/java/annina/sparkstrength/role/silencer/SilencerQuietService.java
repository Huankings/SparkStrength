package annina.sparkstrength.role.silencer;

import annina.sparkstrength.role.coroner.CoronerService;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.index.WatheItems;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.agmas.noellesroles.Noellesroles;

/**
 * 静语者武器静音的统一判定入口。
 *
 * <p>用户确认：验尸官变形成静语者时，也吃到静语者加强。
 * 因此所有音效 mixin 都调用这里，而不是各自只检查真实职业。</p>
 */
public final class SilencerQuietService {
    private SilencerQuietService() {
    }

    public static boolean usesSilencerQuietWeapons(PlayerEntity player) {
        if (player == null || player.getWorld() == null) {
            return false;
        }

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        return gameWorld.isRole(player, Noellesroles.SILENCER)
                || CoronerService.hasSilencerDisguise(player);
    }

    public static boolean shouldSilenceWatheRevolver(PlayerEntity player) {
        // 用户确认只静音 Wathe 的 revolver；derringer 和其它枪保持原音效。
        return usesSilencerQuietWeapons(player)
                && player.getMainHandStack().isOf(WatheItems.REVOLVER);
    }

    public static boolean shouldSilenceWatheBat(PlayerEntity player) {
        return usesSilencerQuietWeapons(player)
                && player.getMainHandStack().isOf(WatheItems.BAT);
    }

    public static boolean isHoldingWatheKnife(PlayerEntity player) {
        ItemStack mainHandStack = player.getMainHandStack();
        ItemStack offHandStack = player.getOffHandStack();
        return mainHandStack.isOf(WatheItems.KNIFE) || offHandStack.isOf(WatheItems.KNIFE);
    }
}
