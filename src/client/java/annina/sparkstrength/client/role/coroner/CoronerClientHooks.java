package annina.sparkstrength.client.role.coroner;

import annina.sparkstrength.compat.SparkTraitsCompat;
import annina.sparkstrength.role.coroner.CoronerService;
import dev.doctor4t.wathe.api.event.GetInstinctHighlight;
import dev.doctor4t.wathe.api.event.ShouldShowCohort;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerPoisonComponent;
import dev.doctor4t.wathe.cca.PlayerPsychoComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.bartender.BartenderPlayerComponent;
import org.agmas.noellesroles.bomber.BomberPlayerComponent;
import org.agmas.noellesroles.demonhunter.DemonHunterPlayerComponent;
import org.agmas.noellesroles.professor.IronManPlayerComponent;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;

/**
 * 验尸官伪装后的客户端本能提示。
 *
 * <p>当验尸官当前变形成“杀手阵营尸体身份”时，杀手阵营玩家会像看卧底一样，
 * 在按本能键时把他误认为同伙，并显示 cohort 提示。</p>
 */
public final class CoronerClientHooks {
    private static final int KILLER_COHORT_RED = MathHelper.hsvToRgb(0F, 1.0F, 0.6F);

    private CoronerClientHooks() {
    }

    public static void register() {
        GetInstinctHighlight.EVENT.register(CoronerClientHooks::highlightKillerDisguise);
        GetInstinctHighlight.EVENT.register(CoronerClientHooks::highlightBorrowedRoleInstincts);
        ShouldShowCohort.EVENT.register(CoronerClientHooks::cohortPrompt);
    }

    /**
     * Returns the killer-coloured instinct result for a Coroner killer disguise.
     *
     * <p>This small public seam is intentionally kept on the client side. SparkTraits
     * can discover it through an optional reflection bridge when its own early
     * WatheClient mixin runs before Wathe dispatches {@code GetInstinctHighlight}.
     * SparkStrength itself never acquires a hard dependency on SparkTraits.</p>
     *
     * <p>这里提供一个客户端公开接缝：当 SparkTraits 的 WatheClient 早期 Mixin
     * 先于 Wathe 本能事件执行时，SparkTraits 可以通过可选反射调用这里的判断。
     * SparkStrength 不会因此对 SparkTraits 形成硬依赖。</p>
     */
    public static @Nullable Integer resolveKillerDisguiseInstinctColor(Entity target) {
        ClientPlayerEntity viewer = MinecraftClient.getInstance().player;
        if (viewer == null || !(target instanceof PlayerEntity targetPlayer) || targetPlayer.isSpectator()) {
            return null;
        }

        GameWorldComponent game = GameWorldComponent.KEY.get(viewer.getWorld());
        /*
         * 使用 SparkTraits 的公开软兼容门面判断观察者是否属于有效杀手阵营。
         * 没有 SparkTraits 时，该门面会自动回退到 Wathe 原生 canUseKiller 结果，
         * 因此原版 SparkStrength + Wathe 的行为不会改变。
         *
         * 使用有效阵营而不是 game.canUseKillerFeatures(viewer)，才能让“原始好人
         * + 内鬼词条”的观察者也获得验尸官杀手伪装的红色本能和同伙提示。
         */
        if (!SparkTraitsCompat.isEffectiveKiller(game.getRole(viewer), viewer)
                || !GameFunctions.isPlayerPlayingAndAlive(viewer)
                || !GameFunctions.isPlayerPlayingAndAlive(targetPlayer)
                || !(CoronerService.hasKillerFactionDisguise(targetPlayer)
                || CoronerService.hasUndercoverDisguise(targetPlayer))) {
            return null;
        }

        return KILLER_COHORT_RED;
    }

    private static GetInstinctHighlight.HighlightResult highlightKillerDisguise(Entity target) {
        Integer color = resolveKillerDisguiseInstinctColor(target);
        if (color == null) {
            return null;
        }

        return GetInstinctHighlight.HighlightResult.withKeybind(
                color,
                GetInstinctHighlight.HighlightResult.PRIORITY_HIGH
        );
    }

    private static ShouldShowCohort.CohortResult cohortPrompt(PlayerEntity viewer, PlayerEntity target) {
        if (viewer == null || target == null) {
            return null;
        }

        GameWorldComponent game = GameWorldComponent.KEY.get(viewer.getWorld());
        /*
         * 同伙提示必须与本能颜色使用同一套“有效杀手”判定，否则内鬼观察者
         * 虽然能按键透视，却仍然无法看到变形验尸官下方的 Killer's Cohort。
         */
        if (!SparkTraitsCompat.isEffectiveKiller(game.getRole(viewer), viewer)
                || !GameFunctions.isPlayerPlayingAndAlive(viewer)
                || !GameFunctions.isPlayerPlayingAndAlive(target)
                || !(CoronerService.hasKillerFactionDisguise(target)
                || CoronerService.hasUndercoverDisguise(target))) {
            return null;
        }

        return ShouldShowCohort.CohortResult.show();
    }

    private static GetInstinctHighlight.HighlightResult highlightBorrowedRoleInstincts(Entity target) {
        ClientPlayerEntity viewer = MinecraftClient.getInstance().player;
        if (viewer == null || !(target instanceof PlayerEntity targetPlayer)) {
            return null;
        }
        if (!GameFunctions.isPlayerPlayingAndAlive(viewer) || !GameFunctions.isPlayerPlayingAndAlive(targetPlayer)) {
            return null;
        }

        GameWorldComponent game = GameWorldComponent.KEY.get(viewer.getWorld());
        boolean hiddenSurvivalMaster = (game.isRole(targetPlayer, Noellesroles.SURVIVAL_MASTER)
                || CoronerService.hasSurvivalMasterDisguise(targetPlayer)) && !viewer.canSee(targetPlayer);
        if (hiddenSurvivalMaster) {
            return GetInstinctHighlight.HighlightResult.skip();
        }

        if (CoronerService.hasBomberDisguise(viewer)
                && BomberPlayerComponent.KEY.get(targetPlayer).hasBomb()) {
            return GetInstinctHighlight.HighlightResult.always(Noellesroles.BOMBER.color());
        }

        if (CoronerService.hasDemonHunterDisguise(viewer)) {
            DemonHunterPlayerComponent hunterComp = DemonHunterPlayerComponent.KEY.get(viewer);
            if (hunterComp.isPlayerFrenzied(targetPlayer.getUuid())
                    && !game.isRole(targetPlayer, Noellesroles.SILENCER)) {
                return GetInstinctHighlight.HighlightResult.always(Noellesroles.DEMON_HUNTER.color());
            }
        }

        if (PlayerPsychoComponent.KEY.get(viewer).getPsychoTicks() > 0
                && !game.isRole(viewer, Noellesroles.JESTER)
                && CoronerService.hasDemonHunterDisguise(targetPlayer)) {
            return GetInstinctHighlight.HighlightResult.always(Noellesroles.DEMON_HUNTER.color());
        }

        if (CoronerService.hasBartenderDisguise(viewer) && viewer.canSee(targetPlayer)
                && BartenderPlayerComponent.KEY.get(targetPlayer).glowTicks > 0) {
            return GetInstinctHighlight.HighlightResult.always(Color.GREEN.getRGB());
        }

        if (CoronerService.hasProfessorDisguise(viewer) && viewer.canSee(targetPlayer)
                && IronManPlayerComponent.KEY.get(targetPlayer).hasBuff()) {
            return GetInstinctHighlight.HighlightResult.always(Color.BLUE.getRGB());
        }

        if (CoronerService.hasToxicologistDisguise(viewer) && viewer.canSee(targetPlayer)
                && PlayerPoisonComponent.KEY.get(targetPlayer).poisonTicks > 0) {
            return GetInstinctHighlight.HighlightResult.always(Noellesroles.TOXICOLOGIST.color());
        }

        if (CoronerService.hasPoisonerDisguise(viewer)
                && PlayerPoisonComponent.KEY.get(targetPlayer).poisonTicks > 0) {
            /*
             * 验尸官伪装为毒师时，沿用 NoellesRoles 毒师的中毒透视：
             * 不要求视线，颜色使用毒师职业色；上方 hiddenSurvivalMaster 的 skip 已经保证
             * 生存大师在被遮挡时仍能免疫杀手阵营/毒师这类穿墙本能。
             */
            return GetInstinctHighlight.HighlightResult.always(Noellesroles.POISONER.color());
        }

        return null;
    }
}
