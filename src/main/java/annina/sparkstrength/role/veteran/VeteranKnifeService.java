package annina.sparkstrength.role.veteran;

import annina.sparkstrength.component.veteran.VeteranKnifeComponent;
import annina.sparkstrength.role.coroner.CoronerService;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerVeteranComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.record.GameRecordManager;
import dev.doctor4t.wathe.util.KnifeStabPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;

/**
 * 老兵匕首的服务端规则。
 *
 * <p>这个服务由 mixin 在 {@link KnifeStabPayload.Receiver#receive} 开头调用。
 * 如果发包者是老兵，就由这里完整接管刺杀并取消 Wathe 原逻辑，从而同时做到：
 * 无刺杀音效、刺杀后无刀 CD、支持多把商店匕首累计次数，以及避免原版 0/1/2 次组件吞掉新买的刀。</p>
 */
public final class VeteranKnifeService {
    private VeteranKnifeService() {
    }

    public static void assignForRole(ServerPlayerEntity player, Role role) {
        VeteranKnifeComponent knife = VeteranKnifeComponent.KEY.get(player);
        if (VeteranRules.isVeteran(role)) {
            knife.initializeStartingKnife();
            syncWatheVeteranGuard(player, knife);
        } else {
            knife.reset();
            PlayerVeteranComponent.KEY.get(player).reset();
        }
    }

    public static void reset(ServerPlayerEntity player) {
        VeteranKnifeComponent.KEY.get(player).reset();
        PlayerVeteranComponent.KEY.get(player).reset();
    }

    /**
     * @return true 表示本次 payload 已由老兵逻辑处理，原 Wathe 刀人逻辑必须取消。
     */
    public static boolean handleKnifeStab(KnifeStabPayload payload, ServerPlayNetworking.Context context) {
        ServerPlayerEntity player = context.player();
        GameWorldComponent game = GameWorldComponent.KEY.get(player.getWorld());
        Role role = game.getRole(player);
        if (!VeteranRules.isVeteran(role) && CoronerService.hasInstantSilentKnifeDisguise(player)) {
            return handleCoronerInstantSilentKnifeDisguise(payload, player, game);
        }
        if (!VeteranRules.isVeteran(role)) {
            return false;
        }

        // 只要是老兵的刀包，就不再放回原逻辑；无效目标直接整刀无效，避免原版播放声音/设置 CD。
        if (player.isSpectator()) {
            return true;
        }
        Entity targetEntity = player.getServerWorld().getEntityById(payload.target());
        if (!(targetEntity instanceof ServerPlayerEntity target)
                || target.isSpectator()
                || target.distanceTo(player) > 3.0D) {
            return true;
        }
        if (!isHoldingKnife(player)) {
            return true;
        }

        VeteranKnifeComponent knife = VeteranKnifeComponent.KEY.get(player);
        adoptExistingWatheUsesIfNeeded(player, knife);
        if (!knife.hasStabUsesLeft()) {
            syncWatheVeteranGuard(player, knife);
            return true;
        }

        Hand usedHand = heldKnifeHand(player);
        // 必须在目标死亡前保存有效阵营；SparkTraits 会在死亡后的事件中清空目标当前词条。
        boolean targetWasEffectiveCivilian =
                VeteranRules.isEffectiveCivilian(game.getRole(target), target);
        knife.useStab();
        if (VeteranRules.shouldRemoveKnifeAfterUse(knife.getStabUsesLeft())) {
            removeOneHeldOrInventoryKnife(player);
        }
        syncWatheVeteranGuard(player, knife);

        // 保留 Wathe 的回放记录：之后复盘仍能看到老兵使用了刀。
        GameRecordManager.recordItemUse(
                player,
                Registries.ITEM.getId(WatheItems.KNIFE),
                target,
                null
        );

        GameFunctions.killPlayer(target, true, player, GameConstants.DeathReasons.KNIFE);
        punishVeteranForStabbingInnocent(game, player, target, targetWasEffectiveCivilian);
        player.swingHand(usedHand);
        // 老兵加强要求刺杀后没有刀 CD；这里显式清掉，防止其他逻辑在同 tick 写入冷却。
        player.getItemCooldownManager().remove(WatheItems.KNIFE);
        return true;
    }

    private static boolean handleCoronerInstantSilentKnifeDisguise(
            KnifeStabPayload payload,
            ServerPlayerEntity player,
            GameWorldComponent game
    ) {
        if (player.isSpectator()) {
            return true;
        }
        Entity targetEntity = player.getServerWorld().getEntityById(payload.target());
        if (!(targetEntity instanceof ServerPlayerEntity target)
                || target.isSpectator()
                || target.distanceTo(player) > 3.0D) {
            return true;
        }
        if (!isHoldingKnife(player)) {
            return true;
        }

        /*
         * 验尸官只从“老兵/清道夫尸体身份”借到无蓄力、无声音出刀，
         * 不接入老兵 2 次次数池；但老兵尸体身份仍继承老兵“小脑惩罚”，
         * 清道夫尸体身份只继承无蓄力无声音出刀，不继承老兵惩罚；
         * 临时匕首会在解除/切换变形时统一回收。
         */
        Hand usedHand = heldKnifeHand(player);
        // 验尸官借用老兵身份时也要在死亡前保存词条阵营，和真实老兵完全一致。
        boolean targetWasEffectiveCivilian =
                VeteranRules.isEffectiveCivilian(game.getRole(target), target);
        GameRecordManager.recordItemUse(
                player,
                Registries.ITEM.getId(WatheItems.KNIFE),
                target,
                null
        );
        GameFunctions.killPlayer(target, true, player, GameConstants.DeathReasons.KNIFE);
        if (CoronerService.hasVeteranDisguise(player)) {
            punishVeteranForStabbingInnocent(game, player, target, targetWasEffectiveCivilian);
        }
        player.swingHand(usedHand);
        player.getItemCooldownManager().remove(WatheItems.KNIFE);
        return true;
    }

    public static void addPurchasedKnife(ServerPlayerEntity player) {
        VeteranKnifeComponent knife = VeteranKnifeComponent.KEY.get(player);
        knife.addKnife();
        syncWatheVeteranGuard(player, knife);
    }

    private static void punishVeteranForStabbingInnocent(
            GameWorldComponent game,
            ServerPlayerEntity veteran,
            ServerPlayerEntity target,
            boolean targetWasEffectiveCivilian
    ) {
        if (!game.isPlayerDead(target.getUuid())
                || !targetWasEffectiveCivilian
                || !GameFunctions.isPlayerPlayingAndAlive(veteran)) {
            return;
        }

        // 需求指定“使用匕首 knife 杀到好人阵营时老兵小脑死亡”。
        // 这里使用出刀前快照严格按有效阵营处理：善良词条仍算好人，会触发惩罚；
        // 内鬼词条虽然原始职业是好人，但有效阵营已是杀手，因此不会误罚老兵。
        // 先确认目标已死亡，避免疯魔盾或其它 KillPlayer.BEFORE 取消死亡时误罚老兵。
        // 此服务只接管 Wathe knife 的刺杀包，所以这里天然只覆盖明确使用匕首的成功击杀。
        GameFunctions.killPlayer(veteran, true, null, GameConstants.DeathReasons.SHOT_INNOCENT);
    }

    private static void adoptExistingWatheUsesIfNeeded(ServerPlayerEntity player, VeteranKnifeComponent knife) {
        if (knife.hasStabUsesLeft()) {
            return;
        }
        PlayerVeteranComponent watheVeteran = PlayerVeteranComponent.KEY.get(player);
        if (watheVeteran.hasStabUsesLeft()) {
            // 兼容中途加载/旧局状态：如果 Wathe 原组件还有开局刀次数，而我们还没记录，就接管过来。
            knife.addStabUses(watheVeteran.getStabUsesLeft());
        }
    }

    private static void syncWatheVeteranGuard(ServerPlayerEntity player, VeteranKnifeComponent knife) {
        PlayerVeteranComponent watheVeteran = PlayerVeteranComponent.KEY.get(player);
        if (knife.hasStabUsesLeft()) {
            // 原组件最多只能记 2 次，这里只把它当作“老兵仍有可用刀，不能捡枪”的布尔标记。
            watheVeteran.initialize();
        } else {
            watheVeteran.reset();
        }
    }

    private static boolean isHoldingKnife(ServerPlayerEntity player) {
        return player.getMainHandStack().isOf(WatheItems.KNIFE)
                || player.getOffHandStack().isOf(WatheItems.KNIFE);
    }

    private static Hand heldKnifeHand(ServerPlayerEntity player) {
        return player.getMainHandStack().isOf(WatheItems.KNIFE) ? Hand.MAIN_HAND : Hand.OFF_HAND;
    }

    private static void removeOneHeldOrInventoryKnife(ServerPlayerEntity player) {
        if (removeOneKnifeFromStack(player.getMainHandStack())) {
            return;
        }
        if (removeOneKnifeFromStack(player.getOffHandStack())) {
            return;
        }
        for (int slot = 0; slot < player.getInventory().size(); slot++) {
            ItemStack stack = player.getInventory().getStack(slot);
            if (stack.isOf(WatheItems.KNIFE)) {
                player.getInventory().removeStack(slot, 1);
                return;
            }
        }
    }

    private static boolean removeOneKnifeFromStack(ItemStack stack) {
        if (!stack.isOf(WatheItems.KNIFE)) {
            return false;
        }
        stack.decrement(1);
        return true;
    }
}
