package annina.sparkstrength.client.role.timekeeper;

import annina.sparkstrength.SparkStrengthItems;
import annina.sparkstrength.network.timekeeper.TimekeeperWatchModeC2SPacket;
import annina.sparkstrength.role.timekeeper.TimekeeperWatchMode;
import annina.sparkstrength.role.timekeeper.TimekeeperConstants;
import annina.sparkstrength.role.coroner.CoronerService;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import org.agmas.noellesroles.Noellesroles;

/** 客户端怀表交互：吞掉左键普通攻击，并在按下边沿切换两个刷新模式。 */
public final class TimekeeperWatchClientHooks {
    private static boolean attackHeld;

    private TimekeeperWatchClientHooks() {
    }

    public static void tick(MinecraftClient client) {
        if (client.player == null || !client.options.attackKey.isPressed()) {
            attackHeld = false;
        }
    }

    public static boolean handleAttack(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null || !isWatchModeSwitchAvailable(player)) {
            attackHeld = false;
            return false;
        }
        if (attackHeld) {
            return true;
        }

        attackHeld = true;
        TimekeeperWatchMode next = TimekeeperWatchMode.fromOrdinal(
                player.getMainHandStack().getOrDefault(SparkStrengthItems.TIMEKEEPER_WATCH_MODE, 0)
        ).next();
        player.getMainHandStack().set(SparkStrengthItems.TIMEKEEPER_WATCH_MODE, next.ordinal());
        player.sendMessage(
                Text.translatable("message.sparkstrength.dying_watch.mode_changed", next.text())
                        .copy()
                        .setStyle(net.minecraft.text.Style.EMPTY.withColor(
                                net.minecraft.text.TextColor.fromRgb(TimekeeperConstants.ROLE_COLOR)
                        )),
                true
        );
        ClientPlayNetworking.send(new TimekeeperWatchModeC2SPacket(next.ordinal()));
        return true;
    }

    private static boolean isWatchModeSwitchAvailable(ClientPlayerEntity player) {
        GameWorldComponent game = GameWorldComponent.KEY.get(player.getWorld());
        return player.getMainHandStack().isOf(SparkStrengthItems.dyingWatch())
                && (game.isRole(player, Noellesroles.TIMEKEEPER)
                || CoronerService.hasTimekeeperDisguise(player))
                && GameFunctions.isPlayerPlayingAndAlive(player);
    }
}
