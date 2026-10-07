package annina.sparkstrength.client.role.demonhunter;

import annina.sparkstrength.component.demonhunter.DemonHunterSniffPlayerComponent;
import annina.sparkstrength.network.demonhunter.DemonHunterSniffC2SPacket;
import annina.sparkstrength.role.demonhunter.DemonHunterSniffRules;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.event.GetInstinctHighlight;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.client.NoellesrolesClient;

/**
 * 猎魔人嗅探技能的客户端挂钩。
 *
 * <p>按键直接复用 NoellesRoles 的能力键绑定，这样玩家在设置里重绑 G 键后，
 * 嗅探提示和触发都会自动跟随。这里用 {@link KeyBinding#isPressed()} 做上升沿检测，
 * 不消耗 NoellesRoles 自己的 {@code wasPressed()} 队列，避免两个 mod 抢同一次按键。</p>
 */
public final class DemonHunterSniffClientHooks {
    private static boolean registered;
    private static boolean abilityKeyWasDown;

    private DemonHunterSniffClientHooks() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        ClientTickEvents.END_CLIENT_TICK.register(DemonHunterSniffClientHooks::tickAbilityKey);
        GetInstinctHighlight.EVENT.register(DemonHunterSniffClientHooks::highlightSniffReveal);
    }

    private static void tickAbilityKey(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        KeyBinding abilityBind = NoellesrolesClient.abilityBind;
        if (player == null || abilityBind == null) {
            abilityKeyWasDown = false;
            return;
        }

        boolean keyDown = abilityBind.isPressed();
        if (keyDown && !abilityKeyWasDown && canSendSniffRequest(player)) {
            ClientPlayNetworking.send(new DemonHunterSniffC2SPacket());
        }
        abilityKeyWasDown = keyDown;
    }

    private static boolean canSendSniffRequest(ClientPlayerEntity player) {
        if (!GameFunctions.isPlayerPlayingAndAlive(player)) {
            return false;
        }
        GameWorldComponent game = GameWorldComponent.KEY.get(player.getWorld());
        Role role = game.getRole(player);
        return DemonHunterSniffRules.isDemonHunter(role);
    }

    private static GetInstinctHighlight.HighlightResult highlightSniffReveal(Entity target) {
        ClientPlayerEntity viewer = MinecraftClient.getInstance().player;
        if (viewer == null || !(target instanceof PlayerEntity targetPlayer)) {
            return null;
        }
        if (viewer.getUuid().equals(targetPlayer.getUuid()) || !GameFunctions.isPlayerPlayingAndAlive(viewer)) {
            return null;
        }

        GameWorldComponent game = GameWorldComponent.KEY.get(viewer.getWorld());
        Role viewerRole = game.getRole(viewer);
        if (!DemonHunterSniffRules.isDemonHunter(viewerRole)) {
            return null;
        }

        DemonHunterSniffPlayerComponent component = DemonHunterSniffPlayerComponent.KEY.get(viewer);
        if (component.isSniffRevealing(targetPlayer.getUuid())) {
            // 低优先级让 NoellesRoles 原本的“疯魔中猎魔人高亮颜色”先赢；
            // 静语者因为原高亮特意跳过，所以会落到这里显示嗅探红色。
            return GetInstinctHighlight.HighlightResult.always(
                    DemonHunterSniffRules.HIGHLIGHT_COLOR,
                    DemonHunterSniffRules.HIGHLIGHT_PRIORITY
            );
        }
        return null;
    }
}
