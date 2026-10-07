package annina.sparkstrength.client;

import annina.sparkstrength.client.item.CapsuleClient;
import annina.sparkstrength.client.role.coroner.CoronerClientHooks;
import annina.sparkstrength.client.role.corruptcop.CorruptCopClientHooks;
import annina.sparkstrength.client.role.corruptcop.CorruptCopMusicController;
import annina.sparkstrength.client.role.demonhunter.DemonHunterSniffClientHooks;
import annina.sparkstrength.client.role.detective.CriminologistClientHooks;
import annina.sparkstrength.client.role.economy.RoleEconomyClientHooks;
import annina.sparkstrength.client.role.engineer.EngineerClientHooks;
import annina.sparkstrength.client.role.morphling.MorphlingClientHooks;
import annina.sparkstrength.client.role.professor.ProfessorSerumClientHooks;
import annina.sparkstrength.client.role.veteran.VeteranClientHooks;
import annina.sparkstrength.client.role.waiter.WaiterTaskRevealClientHooks;
import annina.sparkstrength.client.role.timekeeper.TimekeeperWatchClientHooks;
import annina.sparkstrength.client.role.shadowjester.ShadowJesterShowdownMusicController;
import annina.sparkstrength.client.screen.criminologist.CriminologistScreen;
import annina.sparkstrength.client.screen.tablet.TabletClientState;
import annina.sparkstrength.client.screen.tablet.TabletScreen;
import annina.sparkstrength.client.tablet.TabletClientHighlights;
import annina.sparkstrength.network.criminologist.OpenCriminologistScreenS2CPacket;
import annina.sparkstrength.network.tablet.OpenTabletScreenS2CPacket;
import annina.sparkstrength.network.tablet.SyncTabletSnapshotS2CPacket;
import annina.sparkstrength.network.shadowjester.SyncShadowJesterShowdownMusicS2CPacket;
import annina.sparkstrength.network.veteran.SyncVeteranBlackoutS2CPacket;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import annina.sparkstrength.SparkStrengthItems;
import annina.sparkstrength.component.timekeeper.TimekeeperWatchComponent;
import annina.sparkstrength.item.TimekeeperWatchItem;
import annina.sparkstrength.role.timekeeper.TimekeeperConstants;
import annina.sparkstrength.role.timekeeper.TimekeeperWatchMode;

public final class SparkStrengthClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        CapsuleClient.register();
        CoronerClientHooks.register();
        CorruptCopClientHooks.register();
        CriminologistClientHooks.register();
        DemonHunterSniffClientHooks.register();
        EngineerClientHooks.register();
        MorphlingClientHooks.register();
        ProfessorSerumClientHooks.register();
        RoleEconomyClientHooks.register();
        TabletClientHighlights.register();
        VeteranClientHooks.register();
        WaiterTaskRevealClientHooks.register();
        ClientTickEvents.END_CLIENT_TICK.register(CorruptCopMusicController::tick);
        ClientTickEvents.END_CLIENT_TICK.register(ShadowJesterShowdownMusicController::tick);
        ClientTickEvents.END_CLIENT_TICK.register(TimekeeperWatchClientHooks::tick);
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
            if (!stack.isOf(SparkStrengthItems.dyingWatch())) {
                return;
            }

            TimekeeperWatchMode mode = TimekeeperWatchItem.getMode(stack);
            int cost = mode == TimekeeperWatchMode.ITEM_REFRESH
                    ? TimekeeperConstants.ITEM_REFRESH_COST_COINS
                    : TimekeeperConstants.ABILITY_REFRESH_COST_COINS;
            int cooldownTicks = 0;
            if (MinecraftClient.getInstance().player != null) {
                var watch = TimekeeperWatchComponent.KEY.get(MinecraftClient.getInstance().player);
                cooldownTicks = mode == TimekeeperWatchMode.ITEM_REFRESH
                        ? watch.getItemRefreshCooldownTicks()
                        : watch.getAbilityRefreshCooldownTicks();
            }
            Text cooldown = cooldownTicks > 0
                    ? Text.translatable("item.sparkstrength.dying_watch.cooldown", (cooldownTicks + 19) / 20)
                    : Text.translatable("item.sparkstrength.dying_watch.cooldown.ready");
            var style = net.minecraft.text.Style.EMPTY.withColor(0xADD8E6).withItalic(false);
            int insertAt = Math.min(1, lines.size());
            lines.add(insertAt, Text.translatable("item.sparkstrength.dying_watch.cost", cost).setStyle(style));
            lines.add(insertAt + 1, cooldown.copy().setStyle(style));
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
                VeteranClientHooks.resetBlackoutState());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
                ShadowJesterShowdownMusicController.reset());
        ClientPlayNetworking.registerGlobalReceiver(OpenCriminologistScreenS2CPacket.ID,
                (payload, context) -> context.client().execute(() ->
                        context.client().setScreen(new CriminologistScreen(payload.victimUuid()))));
        ClientPlayNetworking.registerGlobalReceiver(OpenTabletScreenS2CPacket.ID,
                (payload, context) -> context.client().execute(() ->
                        context.client().setScreen(new TabletScreen())));
        ClientPlayNetworking.registerGlobalReceiver(SyncTabletSnapshotS2CPacket.ID,
                (payload, context) -> context.client().execute(() -> {
                    TabletClientState.apply(payload.snapshot());
                    if (context.client().currentScreen instanceof TabletScreen tabletScreen) {
                        tabletScreen.handleSnapshotUpdate();
                    }
                }));
        ClientPlayNetworking.registerGlobalReceiver(SyncVeteranBlackoutS2CPacket.ID,
                (payload, context) -> context.client().execute(() ->
                        VeteranClientHooks.setBlackoutActive(payload.active())));
        ClientPlayNetworking.registerGlobalReceiver(
                SyncShadowJesterShowdownMusicS2CPacket.ID,
                (payload, context) -> context.client().execute(() ->
                        ShadowJesterShowdownMusicController.setServerRequestedActive(payload.active()))
        );
    }
}
