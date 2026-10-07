package annina.sparkstrength.role.corruptcop;

import annina.sparkstrength.role.coroner.CoronerService;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.event.DoorInteraction;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

/**
 * Bridges NoellesRoles Corrupt Cop server enhancements through public Wathe hooks.
 * 通过 wathe 的公开钩子桥接 NoellesRoles 黑警服务端增强。
 */
public final class CorruptCopFeatureService {
    private static boolean registered;

    private CorruptCopFeatureService() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        DoorInteraction.EVENT.register(CorruptCopFeatureService::doorInteraction);
    }

    private static DoorInteraction.DoorInteractionResult doorInteraction(
            DoorInteraction.DoorInteractionContext context
    ) {
        PlayerEntity player = context.getPlayer();
        ItemStack handItem = context.getHandItem();
        Item item = handItem.getItem();
        Identifier handItemId = Registries.ITEM.getId(item);
        Role playerRole = GameWorldComponent.KEY.get(context.getWorld()).getRole(player);
        /*
         * 中立万能钥匙的“有效身份”同时看真实职业和验尸官尸体伪装：
         * 验尸官从中立尸体获得 neutral_master_key 后，应当能按该尸体身份使用列车门能力。
         */
        DoorInteraction.DoorInteractionResult result = CorruptCopRules.neutralMasterKeyDoorResult(
                handItemId,
                (playerRole != null && playerRole.isNeutral()) || CoronerService.hasNeutralDisguise(player),
                CorruptCopRules.isCorruptCop(playerRole) || CoronerService.hasCorruptCopDisguise(player),
                context.getDoorType(),
                context.isBlasted(),
                context.isJammed(),
                context.isOpen(),
                context.requiresKey(),
                player.getItemCooldownManager().isCoolingDown(item)
        );
        if (result == DoorInteraction.DoorInteractionResult.ALLOW) {
            player.getItemCooldownManager().set(item, CorruptCopRules.NEUTRAL_MASTER_KEY_COOLDOWN_TICKS);
        }
        return result;
    }
}
