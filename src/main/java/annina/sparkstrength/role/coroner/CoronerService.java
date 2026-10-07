package annina.sparkstrength.role.coroner;

import annina.sparkstrength.SparkStrengthItems;
import annina.sparkstrength.compat.SparkWitchCompat;
import annina.sparkstrength.component.coroner.CoronerBodySnapshotComponent;
import annina.sparkstrength.component.coroner.CoronerPlayerComponent;
import annina.sparkstrength.component.morphling.MorphBodyDisguiseWorldComponent;
import annina.sparkstrength.component.morphling.MorphMarkPlayerComponent;
import annina.sparkstrength.component.timekeeper.TimekeeperWatchComponent;
import com.mojang.authlib.GameProfile;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.event.PsychoModeEvents;
import dev.doctor4t.wathe.api.event.ShouldPunishGunShooter;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.index.tag.WatheItemTags;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.WrittenBookContentComponent;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.RawFilteredPair;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.demonhunter.DemonHunterPistolItem;
import org.agmas.noellesroles.demonhunter.DemonHunterPlayerComponent;
import org.agmas.noellesroles.morphling.MorphlingPlayerComponent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * 验尸官增强的服务端权威逻辑。
 *
 * <p>客户端只负责显示头像按钮和提交 UUID；是否是验尸官、这具尸体是否解锁、变形成哪张脸、
 * 发什么临时装备、何时回收装备，都统一在这里判断。</p>
 */
public final class CoronerService {
    private static final String TEMP_ITEM_ROOT_KEY = "SparkStrengthCoronerTemporaryGrant";
    private static final String TEMP_OWNER_KEY = "Owner";
    private static final String TEMP_DISGUISE_KEY = "Disguise";
    private static final String TEMP_ROLE_ID_KEY = "RoleId";
    private static final String TEMP_KIND_KEY = "Kind";
    private static boolean registered;

    private CoronerService() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        ShouldPunishGunShooter.EVENT.register(CoronerService::shouldPunishGunShooter);
        PsychoModeEvents.ON_PSYCHO_START.register(CoronerService::onPsychoStart);
        PsychoModeEvents.ON_PSYCHO_END.register(CoronerService::onPsychoEnd);
    }

    public static void assignForRole(ServerPlayerEntity player, Role role) {
        if (CoronerRules.isCoroner(role)) {
            CoronerPlayerComponent.KEY.get(player).initializeForRole();
            removeTemporaryItems(player);
        } else {
            clearPlayer(player);
        }
    }

    public static void clearPlayer(ServerPlayerEntity player) {
        removeTemporaryItems(player);
        TimekeeperWatchComponent.KEY.get(player).reset();
        DemonHunterPlayerComponent.KEY.get(player).reset();
        CoronerPlayerComponent.KEY.get(player).clearAll();
    }

    public static void afterKill(ServerPlayerEntity victim) {
        removeTemporaryItems(victim);
        removeDroppedTemporaryItems(victim);
        TimekeeperWatchComponent.KEY.get(victim).reset();
        DemonHunterPlayerComponent.KEY.get(victim).reset();
        CoronerPlayerComponent.KEY.get(victim).clearAll();
    }

    public static void tick(ServerWorld world) {
        GameWorldComponent game = GameWorldComponent.KEY.get(world);
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (!GameFunctions.isPlayerPlayingAndAlive(player)
                    || !CoronerRules.isCoroner(game.getRole(player))) {
                continue;
            }
            rewardNearbyBodies(player, world);
            refreshTemporaryEquipmentForMorphPriority(player);
            if (hasDemonHunterDisguise(player)) {
                refreshDemonHunterState(player);
            }
        }
    }

    /**
     * 由 GameFunctions 的尸体生成 mixin 调用，在尸体落地前写入“死亡时身份快照”。
     */
    public static void recordBodySnapshot(ServerPlayerEntity victim, PlayerBodyEntity body) {
        Role deathRole = GameWorldComponent.KEY.get(victim.getWorld()).getRole(victim);
        Identifier roleId = deathRole == null ? CoronerRules.WATHE_NO_ROLE_ID : deathRole.identifier();
        CoronerBodySnapshotComponent.KEY.get(body).setRoleId(roleId);
    }

    public static boolean collectBodyWithBag(ServerPlayerEntity coroner, PlayerBodyEntity body) {
        if (!canUseCoronerAbility(coroner) || body == null) {
            return false;
        }

        UUID bodyOwnerUuid = body.getPlayerUuid();
        CoronerPlayerComponent.BodySnapshot snapshot = snapshotFromBody(coroner, body);
        CoronerPlayerComponent component = CoronerPlayerComponent.KEY.get(coroner);
        component.unlockBody(bodyOwnerUuid, snapshot);
        rewardBodyProximityIfEligible(coroner, component, body);
        recordBodyBagUse(coroner, body, snapshot);
        return true;
    }

    public static boolean selectDisguise(ServerPlayerEntity coroner, UUID targetUuid) {
        if (!canUseCoronerAbility(coroner) || targetUuid == null) {
            return false;
        }

        CoronerPlayerComponent component = CoronerPlayerComponent.KEY.get(coroner);
        if (targetUuid.equals(coroner.getUuid())) {
            removeTemporaryItems(coroner);
            TimekeeperWatchComponent.KEY.get(coroner).reset();
            DemonHunterPlayerComponent.KEY.get(coroner).reset();
            component.clearDisguise();
            return true;
        }

        if (!component.knowsDisguise(targetUuid)) {
            return false;
        }

        CoronerPlayerComponent.BodySnapshot snapshot = component.snapshot(targetUuid);
        if (snapshot == null) {
            return false;
        }

        removeTemporaryItems(coroner);
        TimekeeperWatchComponent.KEY.get(coroner).reset();
        DemonHunterPlayerComponent.KEY.get(coroner).reset();
        component.setActiveDisguise(targetUuid, snapshot);
        if (isHigherPriorityMorphActive(coroner)) {
            /*
             * SparkStrength/Noelles 的变形怪效果优先级高于验尸官长期伪装。
             * 高优先级变形存在时，验尸官先记住自己原本选中的尸体身份，但暂时不发放该身份装备。
             */
            component.setTemporaryEquipmentSuppressed(true);
        } else {
            grantTemporaryEquipment(coroner, targetUuid, snapshot);
        }
        return true;
    }

    public static @Nullable UUID activeDisguiseUuid(PlayerEntity player) {
        if (player == null) {
            return null;
        }
        CoronerPlayerComponent component = CoronerPlayerComponent.KEY.get(player);
        return component.hasActiveDisguise() ? component.activeDisguiseUuid() : null;
    }

    public static @Nullable Role activeDisguiseRoleForRules(PlayerEntity player) {
        return activeDisguiseRole(player);
    }

    public static boolean hasKillerFactionDisguise(PlayerEntity player) {
        return CoronerRules.isKillerFaction(activeDisguiseRole(player));
    }

    public static boolean hasVeteranDisguise(PlayerEntity player) {
        return CoronerRules.isVeteran(activeDisguiseRole(player));
    }

    public static boolean hasInstantSilentKnifeDisguise(PlayerEntity player) {
        return CoronerRules.grantsInstantSilentKnife(activeDisguiseRole(player));
    }

    public static boolean hasWaiterDisguise(PlayerEntity player) {
        return CoronerRules.grantsWaiterDoublePickup(activeDisguiseRole(player));
    }

    public static boolean hasCorruptCopDisguise(PlayerEntity player) {
        return CoronerRules.isCorruptCop(activeDisguiseRole(player));
    }

    public static boolean hasPoisonerDisguise(PlayerEntity player) {
        return CoronerRules.isPoisoner(activeDisguiseRole(player));
    }

    public static boolean hasToxicologistDisguise(PlayerEntity player) {
        return CoronerRules.isToxicologist(activeDisguiseRole(player));
    }

    public static boolean hasProfessorDisguise(PlayerEntity player) {
        return CoronerRules.isProfessor(activeDisguiseRole(player));
    }

    public static boolean hasReporterDisguise(PlayerEntity player) {
        return CoronerRules.isReporter(activeDisguiseRole(player));
    }

    public static boolean hasAttendantDisguise(PlayerEntity player) {
        return CoronerRules.isAttendant(activeDisguiseRole(player));
    }

    public static boolean hasSurvivalMasterDisguise(PlayerEntity player) {
        return CoronerRules.isSurvivalMaster(activeDisguiseRole(player));
    }

    public static boolean hasTimekeeperDisguise(PlayerEntity player) {
        return CoronerRules.isTimekeeper(activeDisguiseRole(player));
    }

    public static boolean hasUndercoverDisguise(PlayerEntity player) {
        return CoronerRules.isUndercover(activeDisguiseRole(player));
    }

    public static boolean hasNoisemakerDisguise(PlayerEntity player) {
        return CoronerRules.isNoisemaker(activeDisguiseRole(player));
    }

    public static boolean hasSilencerDisguise(PlayerEntity player) {
        return CoronerRules.isSilencer(activeDisguiseRole(player));
    }

    public static boolean hasRecallerDisguise(PlayerEntity player) {
        return CoronerRules.isRecaller(activeDisguiseRole(player));
    }

    public static boolean hasBartenderDisguise(PlayerEntity player) {
        return CoronerRules.isBartender(activeDisguiseRole(player));
    }

    public static boolean hasDemonHunterDisguise(PlayerEntity player) {
        return CoronerRules.isDemonHunter(activeDisguiseRole(player));
    }

    public static boolean hasEngineerDisguise(PlayerEntity player) {
        return CoronerRules.isEngineer(activeDisguiseRole(player));
    }

    public static boolean hasBomberDisguise(PlayerEntity player) {
        return CoronerRules.isBomber(activeDisguiseRole(player));
    }

    public static boolean hasKidnapperDisguise(PlayerEntity player) {
        return CoronerRules.isSparkWitchKidnapper(activeDisguiseRole(player));
    }

    public static boolean hasRoleDisguise(PlayerEntity player, Identifier roleId) {
        Role role = activeDisguiseRole(player);
        return role != null && roleId != null && roleId.equals(role.identifier());
    }

    public static boolean isActualCoroner(PlayerEntity player) {
        if (player == null) {
            return false;
        }
        return CoronerRules.isCoroner(GameWorldComponent.KEY.get(player.getWorld()).getRole(player));
    }

    public static boolean canUseToxicologistDisguiseItem(PlayerEntity player) {
        return !isActualCoroner(player) || hasToxicologistDisguise(player);
    }

    public static boolean canUseProfessorDisguiseItem(PlayerEntity player) {
        return !isActualCoroner(player) || hasProfessorDisguise(player);
    }

    public static boolean canUseBartenderDisguiseItem(PlayerEntity player) {
        return !isActualCoroner(player) || hasBartenderDisguise(player);
    }

    public static boolean canUseBomberDisguiseItem(PlayerEntity player) {
        return !isActualCoroner(player) || hasBomberDisguise(player);
    }

    public static boolean canUseEngineerDisguiseItem(PlayerEntity player) {
        return !isActualCoroner(player) || hasEngineerDisguise(player);
    }

    public static boolean hasNeutralDisguise(PlayerEntity player) {
        Role role = activeDisguiseRole(player);
        return role != null && role.isNeutral();
    }

    private static void refreshTemporaryEquipmentForMorphPriority(ServerPlayerEntity player) {
        CoronerPlayerComponent component = CoronerPlayerComponent.KEY.get(player);
        UUID disguiseUuid = component.activeDisguiseUuid();
        if (disguiseUuid == null) {
            component.setTemporaryEquipmentSuppressed(false);
            return;
        }

        if (isHigherPriorityMorphActive(player)) {
            if (!component.isTemporaryEquipmentSuppressed()) {
                /*
                 * 被变形怪效果覆盖时，验尸官自己的伪装能力临时失效：
                 * 外观已经由客户端优先级处理，这里同步收回尸体身份发放的刀、枪、钥匙。
                 */
                removeTemporaryItems(player);
                TimekeeperWatchComponent.KEY.get(player).reset();
                component.setTemporaryEquipmentSuppressed(true);
            }
            return;
        }

        if (!component.isTemporaryEquipmentSuppressed()) {
            return;
        }

        CoronerPlayerComponent.BodySnapshot snapshot = component.snapshot(disguiseUuid);
        component.setTemporaryEquipmentSuppressed(false);
        if (snapshot != null) {
            /*
             * 高优先级变形结束后，验尸官继续自己之前选中的长期尸体伪装，
             * 因此把该尸体身份对应的临时装备重新补发回来。
             */
            grantTemporaryEquipment(player, disguiseUuid, snapshot);
        }
    }

    private static boolean isHigherPriorityMorphActive(PlayerEntity player) {
        if (player == null) {
            return false;
        }
        MorphlingPlayerComponent originalMorph = MorphlingPlayerComponent.KEY.get(player);
        if (originalMorph.getMorphTicks() > 0) {
            return true;
        }
        return MorphMarkPlayerComponent.KEY.get(player).isActive();
    }

    private static void rewardNearbyBodies(ServerPlayerEntity coroner, ServerWorld world) {
        CoronerPlayerComponent component = CoronerPlayerComponent.KEY.get(coroner);
        Box searchBox = coroner.getBoundingBox().expand(CoronerRules.BODY_PROXIMITY_RANGE);
        for (PlayerBodyEntity body : world.getEntitiesByClass(PlayerBodyEntity.class, searchBox,
                body -> body.squaredDistanceTo(coroner) <= CoronerRules.BODY_PROXIMITY_RANGE_SQUARED)) {
            rewardBodyProximityIfEligible(coroner, component, body);
        }
    }

    private static void rewardBodyProximityIfEligible(
            ServerPlayerEntity coroner,
            CoronerPlayerComponent component,
            PlayerBodyEntity body
    ) {
        if (body.squaredDistanceTo(coroner) > CoronerRules.BODY_PROXIMITY_RANGE_SQUARED) {
            return;
        }
        UUID bodyEntityUuid = body.getUuid();
        if (component.hasRewardedBody(bodyEntityUuid)) {
            return;
        }
        component.markRewardedBody(bodyEntityUuid);
        PlayerShopComponent.KEY.get(coroner).addToBalance(CoronerRules.BODY_PROXIMITY_REWARD);
    }

    private static boolean canUseCoronerAbility(ServerPlayerEntity player) {
        Role role = GameWorldComponent.KEY.get(player.getWorld()).getRole(player);
        return CoronerRules.isCoroner(role)
                && GameFunctions.isPlayerPlayingAndAlive(player)
                && GameFunctions.isPlayerAliveAndSurvival(player);
    }

    private static CoronerPlayerComponent.BodySnapshot snapshotFromBody(ServerPlayerEntity coroner, PlayerBodyEntity body) {
        CoronerBodySnapshotComponent bodySnapshot = CoronerBodySnapshotComponent.KEY.get(body);
        Identifier roleId = bodySnapshot.roleId();
        if (!bodySnapshot.hasSnapshot()) {
            /*
             * 正常路径一定会由 GameFunctions mixin 写入尸体快照。
             * 这个兜底只服务于旧存档/其它模组临时生成的 PlayerBodyEntity，避免采尸袋完全失效。
             */
            Role fallbackRole = GameWorldComponent.KEY.get(coroner.getWorld()).getRole(body.getPlayerUuid());
            if (fallbackRole != null) {
                roleId = fallbackRole.identifier();
            }
        }
        return new CoronerPlayerComponent.BodySnapshot(roleId);
    }

    private static @Nullable Role activeDisguiseRole(PlayerEntity player) {
        if (player == null) {
            return null;
        }
        if (isHigherPriorityMorphActive(player)) {
            return null;
        }
        CoronerPlayerComponent component = CoronerPlayerComponent.KEY.get(player);
        Identifier roleId = component.activeDisguiseRoleId();
        if (roleId == null && component.activeDisguiseUuid() != null) {
            CoronerPlayerComponent.BodySnapshot snapshot = component.snapshot(component.activeDisguiseUuid());
            roleId = snapshot == null ? null : snapshot.roleId();
        }
        return CoronerRules.resolveRole(roleId);
    }

    private static void grantTemporaryEquipment(
            ServerPlayerEntity player,
            UUID disguiseUuid,
            CoronerPlayerComponent.BodySnapshot snapshot
    ) {
        Role role = CoronerRules.resolveRole(snapshot.roleId());
        if (role == null) {
            return;
        }

        if (CoronerRules.grantsDagger(role)) {
            giveTemporaryItem(player, WatheItems.KNIFE, disguiseUuid, snapshot.roleId(), "knife");
        }
        if (CoronerRules.grantsRevolver(role)) {
            giveTemporaryItem(player, WatheItems.REVOLVER, disguiseUuid, snapshot.roleId(), "revolver");
        }
        if (CoronerRules.grantsPoisonNeedle(role)) {
            /*
             * 毒师尸体身份是杀手阵营中的特例：验尸官不拿默认匕首，
             * 而是临时获得 NoellesRoles 毒师开局/商店使用的毒针。
             */
            giveTemporaryItem(player, ModItems.POISON_NEEDLE, disguiseUuid, snapshot.roleId(), "poison_needle");
        }
        if (CoronerRules.isToxicologist(role)) {
            giveTemporaryItem(player, ModItems.ANTIDOTE, disguiseUuid, snapshot.roleId(), "antidote");
        }
        if (CoronerRules.isProfessor(role)) {
            giveTemporaryItem(player, ModItems.IRON_MAN_VIAL, disguiseUuid, snapshot.roleId(), "iron_man_vial");
        }
        if (CoronerRules.isAttendant(role)) {
            giveTemporaryStack(player, createAttendantBook(player), disguiseUuid, snapshot.roleId(), "attendant_book");
        }
        if (CoronerRules.isUndercover(role)) {
            giveTemporaryItem(player, WatheItems.WALKIE_TALKIE, disguiseUuid, snapshot.roleId(), "walkie_talkie");
        }
        if (CoronerRules.isEngineer(role)) {
            giveTemporaryItem(player, ModItems.REPAIR_TOOL, disguiseUuid, snapshot.roleId(), "repair_tool");
        }
        if (CoronerRules.isTimekeeper(role)) {
            // 计时员伪装获得完整的 SparkStrength 濒毁怀表；临时标记会随伪装结束自动回收。
            giveTemporaryItem(player, SparkStrengthItems.dyingWatch(), disguiseUuid, snapshot.roleId(), "dying_watch");
        }
        if (CoronerRules.grantsTimedBomb(role)) {
            giveTemporaryItem(player, ModItems.TIMED_BOMB, disguiseUuid, snapshot.roleId(), "timed_bomb");
        }
        if (CoronerRules.isSparkWitchKidnapper(role)) {
            /*
             * 验尸官伪装 SparkWitch 绑架者时，通过软兼容拿 1 个临时迷药。
             * SparkWitch 未安装或版本过旧时 createKidnapperDrugStack 会返回 null，验尸官其它伪装不受影响。
             */
            ItemStack knockoutDrug = SparkWitchCompat.createKidnapperDrugStack();
            if (knockoutDrug != null) {
                giveTemporaryStack(player, knockoutDrug, disguiseUuid, snapshot.roleId(), "sparkwitch_knockout_drug");
            }
        }
        if (CoronerRules.isDemonHunter(role)) {
            refreshDemonHunterState(player);
        }
        if (CoronerRules.grantsConductorMasterKey(role)) {
            giveTemporaryItem(player, ModItems.MASTER_KEY, disguiseUuid, snapshot.roleId(), "master_key");
        }
        if (CoronerRules.grantsNeutralMasterKey(role)) {
            giveTemporaryItem(player, ModItems.NEUTRAL_MASTER_KEY, disguiseUuid, snapshot.roleId(), "neutral_master_key");
        }
    }

    private static void giveTemporaryStack(
            ServerPlayerEntity player,
            ItemStack stack,
            UUID disguiseUuid,
            Identifier roleId,
            String kind
    ) {
        if (stack.isEmpty()) {
            return;
        }
        markTemporaryGrant(stack, player.getUuid(), disguiseUuid, roleId, kind);
        player.giveItemStack(stack);
    }

    private static void giveTemporaryItem(
            ServerPlayerEntity player,
            Item item,
            UUID disguiseUuid,
            Identifier roleId,
            String kind
    ) {
        ItemStack stack = item.getDefaultStack();
        markTemporaryGrant(stack, player.getUuid(), disguiseUuid, roleId, kind);
        player.giveItemStack(stack);
    }

    private static void markTemporaryGrant(
            ItemStack stack,
            UUID ownerUuid,
            UUID disguiseUuid,
            Identifier roleId,
            String kind
    ) {
        NbtComponent component = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        NbtCompound data = component.copyNbt();

        NbtCompound root = new NbtCompound();
        root.putUuid(TEMP_OWNER_KEY, ownerUuid);
        root.putUuid(TEMP_DISGUISE_KEY, disguiseUuid);
        root.putString(TEMP_ROLE_ID_KEY, roleId.toString());
        root.putString(TEMP_KIND_KEY, kind);
        data.put(TEMP_ITEM_ROOT_KEY, root);
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(data));
    }

    public static boolean isTemporaryGrant(ItemStack stack) {
        return !stack.isEmpty()
                && stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT)
                .copyNbt()
                .contains(TEMP_ITEM_ROOT_KEY, NbtElement.COMPOUND_TYPE);
    }

    private static boolean isTemporaryGrantOwnedBy(ItemStack stack, UUID ownerUuid) {
        NbtCompound data = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
        if (!data.contains(TEMP_ITEM_ROOT_KEY, NbtElement.COMPOUND_TYPE)) {
            return false;
        }
        NbtCompound root = data.getCompound(TEMP_ITEM_ROOT_KEY);
        return root.containsUuid(TEMP_OWNER_KEY) && ownerUuid.equals(root.getUuid(TEMP_OWNER_KEY));
    }

    private static void removeTemporaryItems(ServerPlayerEntity player) {
        for (int slot = 0; slot < player.getInventory().size(); slot++) {
            ItemStack stack = player.getInventory().getStack(slot);
            if (isTemporaryGrantOwnedBy(stack, player.getUuid())) {
                player.getInventory().setStack(slot, ItemStack.EMPTY);
            }
        }
        removeDroppedTemporaryItems(player);
    }

    private static void removeDroppedTemporaryItems(ServerPlayerEntity player) {
        /*
         * 玩家可以在变形期间主动把临时装备丢到远处；解除/切换变形时必须把这些装备也收回。
         * 这里按当前世界全量扫描 ItemEntity，只在切换、死亡、重置等低频路径运行，换取回收语义可靠。
         */
        for (ItemEntity itemEntity : player.getServerWorld().getEntitiesByType(EntityType.ITEM,
                itemEntity -> isTemporaryGrantOwnedBy(itemEntity.getStack(), player.getUuid()))) {
            itemEntity.discard();
        }
    }

    public static void recordDisguisedBodyIfNeeded(ServerPlayerEntity victim) {
        if (victim == null || MorphMarkPlayerComponent.KEY.get(victim).isActive()) {
            return;
        }
        CoronerPlayerComponent component = CoronerPlayerComponent.KEY.get(victim);
        UUID disguiseUuid = component.activeDisguiseUuid();
        Role disguiseRole = activeDisguiseRole(victim);
        if (disguiseUuid == null || disguiseRole == null) {
            return;
        }
        /*
         * 验尸官伪装期间死亡时，尸体应展示为当前尸体身份的外观。
         * 这里复用 SparkStrength 变形试剂的尸体外观世界组件，但上方先检查 MorphMarkPlayerComponent，
         * 确保变形试剂死亡外观拥有更高优先级，验尸官不会覆盖它。
         */
        MorphBodyDisguiseWorldComponent.KEY.get(victim.getServerWorld())
                .recordBodyDisguise(victim.getUuid(), disguiseUuid, disguiseName(victim, disguiseUuid));
    }

    public static void applyNoisemakerBodyDeathEffects(ServerPlayerEntity victim, PlayerBodyEntity body) {
        if (!hasNoisemakerDisguise(victim)) {
            return;
        }
        /*
         * 验尸官顶着大嗓门尸体身份死亡时，尸体发光和全体无辜提示也按大嗓门处理。
         * 这发生在 CoronerService.afterKill 清空伪装之前，因此能读到死亡瞬间的伪装身份。
         */
        body.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 20 * 60, 0));
        ServerWorld serverWorld = victim.getServerWorld();
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(serverWorld);
        RegistryEntry<SoundEvent> soundEntry = RegistryEntry.of(SoundEvents.ENTITY_ALLAY_DEATH);
        long seed = serverWorld.random.nextLong();
        for (ServerPlayerEntity player : serverWorld.getServer().getPlayerManager().getPlayerList()) {
            if (gameWorld.isInnocent(player) || !GameFunctions.isPlayerPlayingAndAlive(player)) {
                player.networkHandler.sendPacket(new PlaySoundS2CPacket(soundEntry, SoundCategory.PLAYERS,
                        player.getX(), player.getY(), player.getZ(), 1.0f, 1.0f, seed));
                player.sendMessage(Text.translatable("noellesroles.noisemaker.death_scream"), true);
            }
        }
    }

    public static void refreshDemonHunterState(ServerPlayerEntity hunter) {
        if (!hasDemonHunterDisguise(hunter) || !GameFunctions.isPlayerPlayingAndAlive(hunter)) {
            DemonHunterPlayerComponent.KEY.get(hunter).reset();
            removeTemporaryDemonHunterPistols(hunter);
            return;
        }

        DemonHunterPlayerComponent component = DemonHunterPlayerComponent.KEY.get(hunter);
        boolean anyFrenzied = false;
        List<UUID> trackedBefore = new ArrayList<>(component.getFrenzyPlayerUuids());
        for (UUID trackedUuid : trackedBefore) {
            PlayerEntity tracked = hunter.getWorld().getPlayerByUuid(trackedUuid);
            if (tracked == null || dev.doctor4t.wathe.cca.PlayerPsychoComponent.KEY.get(tracked).getPsychoTicks() <= 0) {
                component.removeFrenzyPlayer(trackedUuid);
            }
        }
        for (ServerPlayerEntity player : hunter.getServerWorld().getPlayers()) {
            if (player == hunter || !GameFunctions.isPlayerPlayingAndAlive(player)) {
                continue;
            }
            if (dev.doctor4t.wathe.cca.PlayerPsychoComponent.KEY.get(player).getPsychoTicks() > 0) {
                component.addFrenzyPlayer(player.getUuid());
                anyFrenzied = true;
            }
        }

        anyFrenzied |= !component.getFrenzyPlayerUuids().isEmpty();

        if (anyFrenzied) {
            ensureTemporaryDemonHunterPistol(hunter);
        } else {
            removeTemporaryDemonHunterPistols(hunter);
        }
    }

    private static void onPsychoStart(ServerPlayerEntity frenziedPlayer, dev.doctor4t.wathe.api.event.PsychoType type) {
        for (ServerPlayerEntity hunter : frenziedPlayer.getServerWorld().getPlayers()) {
            if (hunter == frenziedPlayer || !hasDemonHunterDisguise(hunter) || !GameFunctions.isPlayerPlayingAndAlive(hunter)) {
                continue;
            }
            DemonHunterPlayerComponent.KEY.get(hunter).addFrenzyPlayer(frenziedPlayer.getUuid());
            ensureTemporaryDemonHunterPistol(hunter);
        }
    }

    private static void onPsychoEnd(ServerPlayerEntity frenziedPlayer, dev.doctor4t.wathe.api.event.PsychoType type) {
        for (ServerPlayerEntity hunter : frenziedPlayer.getServerWorld().getPlayers()) {
            if (!hasDemonHunterDisguise(hunter)) {
                continue;
            }
            DemonHunterPlayerComponent.KEY.get(hunter).removeFrenzyPlayer(frenziedPlayer.getUuid());
            refreshDemonHunterState(hunter);
        }
    }

    private static void ensureTemporaryDemonHunterPistol(ServerPlayerEntity hunter) {
        if (DemonHunterPistolItem.findPistol(hunter) != null) {
            return;
        }
        boolean hasAnyGun = hunter.getInventory().contains(stack -> stack.isIn(WatheItemTags.GUNS));
        if (hasAnyGun) {
            return;
        }
        CoronerPlayerComponent component = CoronerPlayerComponent.KEY.get(hunter);
        UUID disguiseUuid = component.activeDisguiseUuid();
        Identifier roleId = component.activeDisguiseRoleId();
        if (disguiseUuid == null || roleId == null) {
            return;
        }

        ItemStack pistol = new ItemStack(ModItems.DEMON_HUNTER_PISTOL);
        pistol.set(ModItems.BULLETS, 2);
        markTemporaryGrant(pistol, hunter.getUuid(), disguiseUuid, roleId, "demon_hunter_pistol");
        dev.doctor4t.wathe.util.ShopEntry.insertStackInFreeSlot(hunter, pistol);
    }

    private static void removeTemporaryDemonHunterPistols(ServerPlayerEntity player) {
        for (int slot = 0; slot < player.getInventory().size(); slot++) {
            ItemStack stack = player.getInventory().getStack(slot);
            if (stack.isOf(ModItems.DEMON_HUNTER_PISTOL) && isTemporaryGrantOwnedBy(stack, player.getUuid())) {
                player.getInventory().setStack(slot, ItemStack.EMPTY);
            }
        }
        for (ItemEntity itemEntity : player.getServerWorld().getEntitiesByType(EntityType.ITEM,
                itemEntity -> itemEntity.getStack().isOf(ModItems.DEMON_HUNTER_PISTOL)
                        && isTemporaryGrantOwnedBy(itemEntity.getStack(), player.getUuid()))) {
            itemEntity.discard();
        }
    }

    private static ItemStack createAttendantBook(ServerPlayerEntity player) {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        List<RawFilteredPair<Text>> pages = new ArrayList<>();
        HashMap<Integer, GameWorldComponent.RoomData> rooms = gameWorld.getRooms();
        HashMap<UUID, GameProfile> profiles = gameWorld.getGameProfiles();

        List<GameWorldComponent.RoomData> sortedRooms = new ArrayList<>(rooms.values());
        sortedRooms.sort(Comparator.comparingInt(GameWorldComponent.RoomData::getIndex));
        for (GameWorldComponent.RoomData room : sortedRooms) {
            StringBuilder pageContent = new StringBuilder();
            pageContent.append("§l§1【").append(room.getName()).append("】§r\n\n");
            List<UUID> roomPlayers = room.getPlayers();
            if (roomPlayers.isEmpty()) {
                pageContent.append("§8（空房间）§r");
            } else {
                for (UUID playerUuid : roomPlayers) {
                    GameProfile profile = profiles.get(playerUuid);
                    pageContent.append("§0• ")
                            .append(profile != null ? profile.getName() : "未知")
                            .append("§r\n");
                }
            }
            pages.add(RawFilteredPair.of(Text.literal(pageContent.toString())));
        }

        book.set(DataComponentTypes.WRITTEN_BOOK_CONTENT, new WrittenBookContentComponent(
                RawFilteredPair.of("列车信息手册"),
                "乘务员",
                0,
                pages,
                true
        ));
        return book;
    }

    private static String disguiseName(ServerPlayerEntity player, UUID disguiseUuid) {
        ServerPlayerEntity disguisePlayer = player.getServer().getPlayerManager().getPlayer(disguiseUuid);
        if (disguisePlayer != null) {
            return disguisePlayer.getGameProfile().getName();
        }
        GameProfile profile = GameWorldComponent.KEY.get(player.getWorld()).getGameProfiles().get(disguiseUuid);
        return profile == null ? disguiseUuid.toString().substring(0, 8) : profile.getName();
    }

    private static @Nullable ShouldPunishGunShooter.PunishResult shouldPunishGunShooter(
            PlayerEntity shooter,
            PlayerEntity victim
    ) {
        /*
         * 用户确认：黑警尸体是中立阵营；验尸官顶着黑警尸体开枪误伤无辜者时，
         * 按黑警处理，不触发“误杀好人导致自己死亡”的惩罚。
         */
        return hasCorruptCopDisguise(shooter)
                ? ShouldPunishGunShooter.PunishResult.cancel()
                : null;
    }

    private static void recordBodyBagUse(
            ServerPlayerEntity coroner,
            PlayerBodyEntity body,
            CoronerPlayerComponent.BodySnapshot snapshot
    ) {
        NbtCompound extra = new NbtCompound();
        extra.putUuid("body_uuid", body.getUuid());
        extra.putUuid("body_owner", body.getPlayerUuid());
        extra.putString("body_role", snapshot.roleId().toString());
        Vec3d bodyPos = body.getPos();
        GameRecordManager.putPos(extra, "body_pos", bodyPos);
        GameRecordManager.recordItemUse(
                coroner,
                Registries.ITEM.getId(SparkStrengthItems.coronerBodyBag()),
                null,
                extra
        );
    }

    public static void playBodyBagSound(ServerWorld world, PlayerBodyEntity body) {
        world.playSound(
                null,
                body.getX(),
                body.getY() + 0.1F,
                body.getZ(),
                SoundEvents.ITEM_BUNDLE_INSERT,
                SoundCategory.PLAYERS,
                0.5F,
                1.0F + world.random.nextFloat() * 0.1F - 0.05F
        );
    }
}
