package annina.sparkstrength.role.detective;

import annina.sparkstrength.component.detective.CriminologistPlayerComponent;
import annina.sparkstrength.component.detective.CriminologistWorldComponent;
import annina.sparkstrength.network.criminologist.OpenCriminologistScreenS2CPacket;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import dev.doctor4t.wathe.game.GameFunctions;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * Server-side actions for the Detective's criminologist skill.
 * 侦探“犯罪学家”第二技能的服务端逻辑。
 *
 * <p>这个类只处理犯罪学家相关流程：记录尸体真凶、打开选择界面、
 * 校验嫌疑人选择、开始追踪或进入冷却。商店、金币、乘务员等机制不再放在这里。</p>
 */
public final class CriminologistService {
    private static boolean registered;

    private CriminologistService() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        UseEntityCallback.EVENT.register(CriminologistService::useEntity);
    }

    public static void assignForRole(ServerPlayerEntity player, Role role) {
        CriminologistPlayerComponent component = CriminologistPlayerComponent.KEY.get(player);
        if (CriminologistRules.isDetective(role)) {
            component.initializeCriminologist();
        } else {
            component.clearCriminologist();
        }
    }

    public static void afterKill(
            ServerPlayerEntity victim,
            @Nullable ServerPlayerEntity killer,
            Identifier deathReason
    ) {
        if (killer != null) {
            CriminologistWorldComponent.KEY.get(victim.getServerWorld())
                    .recordCriminologistKill(victim.getUuid(), killer.getUuid());
        }
        for (ServerPlayerEntity player : victim.getServerWorld().getPlayers()) {
            CriminologistPlayerComponent component = CriminologistPlayerComponent.KEY.get(player);
            UUID targetUuid = component.getCriminologistTrackingTargetUuid();
            if (victim.getUuid().equals(targetUuid)) {
                component.startCriminologistCooldown();
            }
        }
    }

    public static void handleSelection(
            ServerPlayerEntity player,
            UUID victimUuid,
            UUID suspectUuid
    ) {
        GameWorldComponent gameComponent = GameWorldComponent.KEY.get(player.getServerWorld());
        Role role = gameComponent.getRole(player);
        CriminologistPlayerComponent component = CriminologistPlayerComponent.KEY.get(player);
        if (!CriminologistRules.isDetective(role) || !GameFunctions.isPlayerPlayingAndAlive(player)) {
            return;
        }
        if (component.getCriminologistCooldownTicks() > 0 || component.hasCriminologistTarget()) {
            return;
        }
        if (!victimUuid.equals(component.getCriminologistPendingVictimUuid())) {
            player.sendMessage(Text.translatable("message.sparkstrength.criminologist.no_pending"), true);
            return;
        }

        PlayerShopComponent shop = PlayerShopComponent.KEY.get(player);
        if (shop.getBalance() < CriminologistRules.COST) {
            player.sendMessage(Text.translatable(
                    "message.sparkstrength.criminologist.not_enough_money",
                    CriminologistRules.COST
            ), true);
            return;
        }
        // 先扣钱再判定嫌疑人是否正确，避免玩家用选择结果免费试探真凶。
        shop.setBalance(shop.getBalance() - CriminologistRules.COST);

        Optional<UUID> actualKiller = CriminologistWorldComponent.KEY.get(player.getServerWorld())
                .getCriminologistKiller(victimUuid);
        if (actualKiller.isEmpty() || !actualKiller.get().equals(suspectUuid)) {
            component.startCriminologistCooldown();
            player.sendMessage(Text.translatable("message.sparkstrength.criminologist.wrong"), true);
            return;
        }

        ServerPlayerEntity killer = player.getServer().getPlayerManager().getPlayer(suspectUuid);
        if (killer == null || !GameFunctions.isPlayerPlayingAndAlive(killer) || gameComponent.isPlayerDead(suspectUuid)) {
            component.startCriminologistCooldown();
            player.sendMessage(Text.translatable("message.sparkstrength.criminologist.killer_dead"), true);
            return;
        }

        component.startCriminologistTracking(suspectUuid);
        player.sendMessage(Text.translatable("message.sparkstrength.criminologist.correct", killer.getName()), true);
    }

    private static ActionResult useEntity(
            PlayerEntity player,
            World world,
            net.minecraft.util.Hand hand,
            net.minecraft.entity.Entity entity,
            @Nullable net.minecraft.util.hit.EntityHitResult hitResult
    ) {
        if (world.isClient() || !(player instanceof ServerPlayerEntity serverPlayer)) {
            return ActionResult.PASS;
        }
        if (!(entity instanceof PlayerBodyEntity body)) {
            return ActionResult.PASS;
        }

        return tryOpenScreen(serverPlayer, body.getPlayerUuid())
                ? ActionResult.SUCCESS
                : ActionResult.PASS;
    }

    private static boolean tryOpenScreen(ServerPlayerEntity player, UUID victimUuid) {
        GameWorldComponent gameComponent = GameWorldComponent.KEY.get(player.getServerWorld());
        Role role = gameComponent.getRole(player);
        if (!CriminologistRules.isDetective(role) || !GameFunctions.isPlayerPlayingAndAlive(player)) {
            return false;
        }

        CriminologistPlayerComponent component = CriminologistPlayerComponent.KEY.get(player);
        if (component.getCriminologistCooldownTicks() > 0) {
            player.sendMessage(Text.translatable(
                    "message.sparkstrength.criminologist.cooldown",
                    seconds(component.getCriminologistCooldownTicks())
            ), true);
            return true;
        }
        if (component.hasCriminologistTarget()) {
            player.sendMessage(Text.translatable("message.sparkstrength.criminologist.already_tracking"), true);
            return true;
        }

        PlayerShopComponent shop = PlayerShopComponent.KEY.get(player);
        if (shop.getBalance() < CriminologistRules.COST) {
            player.sendMessage(Text.translatable(
                    "message.sparkstrength.criminologist.not_enough_money",
                    CriminologistRules.COST
            ), true);
            return true;
        }

        component.setCriminologistPendingVictim(victimUuid);
        ServerPlayNetworking.send(player, new OpenCriminologistScreenS2CPacket(victimUuid));
        return true;
    }

    private static int seconds(int ticks) {
        return (int) Math.ceil(ticks / 20.0);
    }
}
