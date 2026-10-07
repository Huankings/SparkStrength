package annina.sparkstrength.role.silencer;

import dev.doctor4t.wathe.api.WatheGameModes;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.record.GameRecordManager;
import dev.doctor4t.wathe.util.KnifeStabPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;

/**
 * 静语者匕首刺杀的服务端静音接管。
 *
 * <p>NoellesRoles 已经用 redirect 静音清道夫的 {@code target.playSound(...)}。
 * 为避免两个 redirect 抢同一个调用，这里在 {@link KnifeStabPayload.Receiver#receive} 开头
 * 只接管静语者/验尸官静语者伪装，并手动复刻 Wathe 原刺杀流程，唯一删除刺杀声音。</p>
 */
public final class SilencerKnifeService {
    private SilencerKnifeService() {
    }

    /**
     * @return true 表示这次刀包已经由静语者静音逻辑处理，原 Wathe 流程必须取消。
     */
    public static boolean handleQuietKnifeStab(KnifeStabPayload payload, ServerPlayNetworking.Context context) {
        ServerPlayerEntity player = context.player();
        if (!SilencerQuietService.usesSilencerQuietWeapons(player)) {
            return false;
        }

        if (player.isSpectator()) {
            return true;
        }

        Entity targetEntity = player.getServerWorld().getEntityById(payload.target());
        if (!(targetEntity instanceof ServerPlayerEntity target)
                || target.isSpectator()
                || target.distanceTo(player) > 3.0D) {
            return true;
        }
        if (!SilencerQuietService.isHoldingWatheKnife(player)) {
            return true;
        }

        GameWorldComponent gameComponent = GameWorldComponent.KEY.get(player.getWorld());
        GameRecordManager.recordItemUse(
                player,
                Registries.ITEM.getId(WatheItems.KNIFE),
                target,
                null
        );

        GameFunctions.killPlayer(target, true, player, GameConstants.DeathReasons.KNIFE);
        player.swingHand(heldKnifeHand(player));
        applyWatheKnifeCooldown(player, gameComponent);
        return true;
    }

    private static void applyWatheKnifeCooldown(ServerPlayerEntity player, GameWorldComponent gameComponent) {
        if (player.isCreative() || gameComponent.getGameMode() == WatheGameModes.LOOSE_ENDS) {
            return;
        }

        // 完整保留 Wathe 原版“人数过多时动态降低刀 CD”的计算，只移除刺杀音效。
        int totalPlayers = player.getServerWorld().getPlayers().size();
        int killerCount = gameComponent.getAllKillerTeamPlayers().size();
        int killerRatio = gameComponent.getKillerDividend();
        int excessPlayers = Math.max(0, totalPlayers - (killerCount * killerRatio));
        int baseCooldown = GameConstants.ITEM_COOLDOWNS.get(WatheItems.KNIFE);
        int cooldownReductionPerExcess = GameConstants.getInTicks(0, 5);
        int adjustedCooldown = Math.max(
                GameConstants.getInTicks(0, 10),
                baseCooldown - (excessPlayers * cooldownReductionPerExcess)
        );
        player.getItemCooldownManager().set(WatheItems.KNIFE, adjustedCooldown);
    }

    private static Hand heldKnifeHand(ServerPlayerEntity player) {
        return player.getMainHandStack().isOf(WatheItems.KNIFE) ? Hand.MAIN_HAND : Hand.OFF_HAND;
    }
}
