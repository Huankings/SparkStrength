package annina.sparkstrength.role.engineer;

import annina.sparkstrength.mixin.wathe.PlayerShopComponentAccessor;
import dev.doctor4t.wathe.api.event.ShopPurchase;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.cca.WorldBlackoutComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheSounds;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * 工程师电力恢复系统。
 *
 * <p>购买是否成功由商店 {@code onBuy} 回调决定；购买成功后的扣钱、商店购买记录仍交给 Wathe。
 * 我们只在 {@link ShopPurchase#AFTER} 里拿到真实支付价格，然后把这笔钱平分给存活杀手并缩短停电冷却。</p>
 */
public final class EngineerPowerRestorationService {
    private static boolean registered;

    private EngineerPowerRestorationService() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        ShopPurchase.AFTER.register(EngineerPowerRestorationService::afterPurchase);
    }

    public static boolean tryRestorePower(PlayerEntity buyer) {
        WorldBlackoutComponent blackout = WorldBlackoutComponent.KEY.get(buyer.getWorld());
        if (!blackout.isBlackoutActive()) {
            buyer.sendMessage(Text.translatable("message.sparkstrength.power_restoration.unavailable"), true);
            return false;
        }

        blackout.reset();
        if (buyer.getWorld() instanceof ServerWorld serverWorld) {
            clearBlackoutPotionEffects(serverWorld);
            for (ServerPlayerEntity player : serverWorld.getPlayers()) {
                player.playSoundToPlayer(WatheSounds.BLOCK_LIGHT_TOGGLE, SoundCategory.PLAYERS, 1.0F, 1.0F);
            }
        }
        return true;
    }

    private static void afterPurchase(ServerPlayerEntity buyer, ShopEntry entry, int index, int pricePaid) {
        if (!EngineerRules.POWER_RESTORATION_ENTRY_ID.equals(entry.id())) {
            return;
        }

        ServerWorld world = buyer.getServerWorld();
        List<ServerPlayerEntity> aliveKillers = aliveKillers(world);
        distributeRestorationCost(aliveKillers, pricePaid);
        applyShortBlackoutCooldown(aliveKillers);
    }

    private static void clearBlackoutPotionEffects(ServerWorld world) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            // Wathe 停电会给杀手/老兵夜视，给普通存活玩家失明；这里仅清理这两个停电指定效果。
            player.removeStatusEffect(StatusEffects.NIGHT_VISION);
            player.removeStatusEffect(StatusEffects.BLINDNESS);
        }
    }

    private static List<ServerPlayerEntity> aliveKillers(ServerWorld world) {
        GameWorldComponent game = GameWorldComponent.KEY.get(world);
        List<ServerPlayerEntity> killers = new ArrayList<>();
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (game.canUseKillerFeatures(player) && GameFunctions.isPlayerPlayingAndAlive(player)) {
                killers.add(player);
            }
        }
        return killers;
    }

    private static void distributeRestorationCost(List<ServerPlayerEntity> aliveKillers, int pricePaid) {
        if (aliveKillers.isEmpty() || pricePaid <= 0) {
            return;
        }

        int baseShare = pricePaid / aliveKillers.size();
        int remainder = pricePaid % aliveKillers.size();
        for (int i = 0; i < aliveKillers.size(); i++) {
            int share = baseShare + (i < remainder ? 1 : 0);
            if (share > 0) {
                PlayerShopComponent.KEY.get(aliveKillers.get(i)).addToBalance(share);
            }
        }
    }

    private static void applyShortBlackoutCooldown(List<ServerPlayerEntity> aliveKillers) {
        for (ServerPlayerEntity killer : aliveKillers) {
            PlayerShopComponent shop = PlayerShopComponent.KEY.get(killer);
            // Wathe has no public cooldown setter; the approved Adapter targets only its exact cooldown map.
            // Wathe 没有公开冷却写入接口；获批 Adapter 只访问其精确 cooldown map。
            ((PlayerShopComponentAccessor) shop).sparkstrength$getCooldowns().put(
                    EngineerRules.WATHE_BLACKOUT_ENTRY_ID,
                    EngineerRules.BLACKOUT_COOLDOWN_AFTER_RESTORATION_TICKS
            );
            shop.sync();
        }
    }
}
