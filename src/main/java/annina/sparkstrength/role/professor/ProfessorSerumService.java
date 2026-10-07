package annina.sparkstrength.role.professor;

import annina.sparkstrength.component.professor.ProfessorSerumTargetComponent;
import annina.sparkstrength.component.professor.ProfessorSerumUserComponent;
import annina.sparkstrength.replay.SparkStrengthReplayFormatters;
import annina.sparkstrength.role.coroner.CoronerService;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheSounds;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.entity.projectile.ProjectileUtil;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.taotie.SwallowedPlayerComponent;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 教授试剂的服务端中心逻辑。
 *
 * <p>所有入口都必须走这里：右键、背包远程投喂和未来可能的命令/调试入口。
 * 这样“只有存活教授能用”“目标必须存活”“回放事件怎么写”等规则只维护一份。</p>
 */
public final class ProfessorSerumService {
    private ProfessorSerumService() {
    }

    public static boolean useHeldSerum(ServerPlayerEntity professor, ItemStack stack, ProfessorSerumType type) {
        if (!canUseSerum(professor)) {
            return false;
        }

        ServerPlayerEntity target = findLookedAtTarget(professor);
        if (target == null) {
            target = professor;
        }
        if (!isValidFeedTarget(target)) {
            return false;
        }

        consumeHeldSerumAndApply(professor, target, stack, type);
        return true;
    }

    public static boolean useHeldSerumOnTarget(
            ServerPlayerEntity professor,
            ServerPlayerEntity target,
            ItemStack stack,
            ProfessorSerumType type
    ) {
        if (!canUseSerum(professor) || !isValidFeedTarget(target)) {
            return false;
        }
        consumeHeldSerumAndApply(professor, target, stack, type);
        return true;
    }

    private static void consumeHeldSerumAndApply(
            ServerPlayerEntity professor,
            ServerPlayerEntity target,
            ItemStack stack,
            ProfessorSerumType type
    ) {
        applySerum(professor, target, type);
        if (!professor.isCreative()) {
            stack.decrement(1);
        }
    }

    public static void tryRemoteFeed(ServerPlayerEntity professor, UUID targetUuid, ProfessorSerumType type) {
        if (targetUuid == null || type == null) {
            return;
        }
        if (!canUseSerum(professor)) {
            return;
        }

        ProfessorSerumUserComponent userComponent = ProfessorSerumUserComponent.KEY.get(professor);
        if (userComponent.isOnCooldown()) {
            return;
        }

        ServerPlayerEntity target = professor.getServer().getPlayerManager().getPlayer(targetUuid);
        if (!isValidFeedTarget(target)) {
            failRemoteFeed(professor, userComponent);
            return;
        }
        if (!consumeSerumFromInventory(professor, type)) {
            failRemoteFeed(professor, userComponent);
            return;
        }

        applySerum(professor, target, type);
        userComponent.setCooldownTicks(ProfessorSerumRules.REMOTE_SUCCESS_COOLDOWN_TICKS);
        professor.getWorld().playSound(
                null,
                professor.getBlockPos(),
                WatheSounds.UI_SHOP_BUY,
                SoundCategory.PLAYERS,
                1.0F,
                1.0F
        );
    }

    public static boolean canUseSerum(ServerPlayerEntity player) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        return (ProfessorSerumRules.isProfessor(gameWorld.getRole(player)) || CoronerService.hasProfessorDisguise(player))
                && GameFunctions.isPlayerPlayingAndAlive(player)
                && GameFunctions.isPlayerAliveAndSurvival(player)
                && !SwallowedPlayerComponent.isPlayerSwallowed(player);
    }

    public static boolean isValidFeedTarget(@Nullable ServerPlayerEntity target) {
        return target != null
                && GameFunctions.isPlayerPlayingAndAlive(target)
                && GameFunctions.isPlayerAliveAndSurvival(target)
                && !SwallowedPlayerComponent.isPlayerSwallowed(target);
    }

    private static @Nullable ServerPlayerEntity findLookedAtTarget(ServerPlayerEntity professor) {
        HitResult hitResult = ProjectileUtil.getCollision(
                professor,
                entity -> entity instanceof ServerPlayerEntity target
                        && target != professor
                        && isValidFeedTarget(target),
                ProfessorSerumRules.FEED_RANGE
        );
        if (hitResult instanceof EntityHitResult entityHitResult
                && entityHitResult.getEntity() instanceof ServerPlayerEntity target) {
            return target;
        }
        return null;
    }

    private static boolean consumeSerumFromInventory(ServerPlayerEntity professor, ProfessorSerumType type) {
        if (professor.isCreative()) {
            return true;
        }

        for (int slot = 0; slot < professor.getInventory().size(); slot++) {
            ItemStack stack = professor.getInventory().getStack(slot);
            if (stack.isOf(type.item())) {
                stack.decrement(1);
                return true;
            }
        }
        return false;
    }

    private static void failRemoteFeed(ServerPlayerEntity professor, ProfessorSerumUserComponent userComponent) {
        userComponent.setCooldownTicks(ProfessorSerumRules.REMOTE_FAIL_COOLDOWN_TICKS);
        professor.sendMessage(
                Text.translatable("message.sparkstrength.professor.remote_feed_failed").formatted(Formatting.RED),
                true
        );
    }

    private static void applySerum(ServerPlayerEntity professor, ServerPlayerEntity target, ProfessorSerumType type) {
        recordFeed(professor, target, type);
        target.getWorld().playSound(
                null,
                target.getBlockPos(),
                SoundEvents.ENTITY_GENERIC_DRINK,
                SoundCategory.PLAYERS,
                1.0F,
                1.0F
        );

        ProfessorSerumTargetComponent targetComponent = ProfessorSerumTargetComponent.KEY.get(target);
        switch (type) {
            case INVISIBILITY -> {
                target.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.INVISIBILITY,
                        ProfessorSerumRules.INVISIBILITY_DURATION_TICKS,
                        0,
                        false,
                        false,
                        true
                ));
                targetComponent.apply(type);
            }
            case DOORPASSING, SEDATIVE -> targetComponent.apply(type);
            case TRUTH -> revealTruth(target);
        }
    }

    private static void revealTruth(ServerPlayerEntity target) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(target.getWorld());
        var role = gameWorld.getRole(target);
        Text roleName = role != null
                ? Text.translatable("announcement.role." + role.identifier().getPath()).withColor(role.color())
                : Text.literal("未知身份").formatted(Formatting.WHITE);

        Text targetText = Text.literal(target.getGameProfile().getName());
        Text otherMessage = Text.translatable(
                "message.sparkstrength.professor.truth.other",
                targetText,
                roleName
        ).withColor(Noellesroles.PROFESSOR.color());
        Text selfMessage = Text.translatable("message.sparkstrength.professor.truth.self")
                .withColor(Noellesroles.PROFESSOR.color());

        // 用户已确认“所有人”按在线玩家处理：除目标本人外，所有在线玩家都收到公开身份动作栏。
        for (ServerPlayerEntity player : target.getServer().getPlayerManager().getPlayerList()) {
            player.sendMessage(player == target ? selfMessage : otherMessage, true);
        }

        if (target.getWorld() instanceof ServerWorld serverWorld) {
            GameRecordManager.recordGlobalEvent(
                    serverWorld,
                    SparkStrengthReplayFormatters.PROFESSOR_TRUTH_REVEALED,
                    target,
                    null
            );
        }
    }

    private static void recordFeed(ServerPlayerEntity professor, ServerPlayerEntity target, ProfessorSerumType type) {
        if (!(professor.getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }

        ItemStack stack = type.defaultStack();
        NbtCompound extra = new NbtCompound();
        extra.putUuid("target", target.getUuid());
        extra.putString("item", Registries.ITEM.getId(stack.getItem()).toString());
        extra.putString("item_name", Text.Serialization.toJsonString(stack.getName(), professor.getRegistryManager()));
        GameRecordManager.recordGlobalEvent(
                serverWorld,
                SparkStrengthReplayFormatters.PROFESSOR_SERUM_FED,
                professor,
                extra
        );
    }
}
