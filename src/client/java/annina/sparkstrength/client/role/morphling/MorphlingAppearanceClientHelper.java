package annina.sparkstrength.client.role.morphling;

import annina.sparkstrength.component.morphling.MorphBodyDisguiseWorldComponent;
import annina.sparkstrength.component.morphling.MorphMarkPlayerComponent;
import annina.sparkstrength.role.coroner.CoronerService;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.client.model.WatheModelLayers;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.client.jester.JesterMomentClient;
import org.agmas.noellesroles.morphling.MorphlingPlayerComponent;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 变形试剂客户端外观解析中心。
 *
 * <p>这里刻意只按 UUID 从 Tab 玩家列表、Wathe 玩家缓存和默认皮肤里取“原始皮肤”，
 * 不读取目标实体当前的 {@code getSkinTextures()}。NoellesRoles 原 Morphling 也会改写玩家
 * 皮肤入口，如果这里再递归读取目标实体，就可能把目标正在伪装的外观也套进来，甚至重新触发
 * 双方互相变形时的嵌套渲染崩溃。</p>
 */
public final class MorphlingAppearanceClientHelper {
    private static final UUID FALLBACK_BODY_SKIN_UUID = UUID.fromString("25adae11-cd98-48f4-990b-9fe1b2ee0886");

    private static PlayerEntityModel<AbstractClientPlayerEntity> classicPlayerModel;
    private static PlayerEntityModel<AbstractClientPlayerEntity> slimPlayerModel;
    private static PlayerEntityModel<PlayerBodyEntity> classicBodyModel;
    private static PlayerEntityModel<PlayerBodyEntity> slimBodyModel;

    private MorphlingAppearanceClientHelper() {
    }

    public static void initializePlayerModels(EntityRendererFactory.Context context) {
        if (classicPlayerModel == null) {
            classicPlayerModel = new PlayerEntityModel<>(context.getPart(EntityModelLayers.PLAYER), false);
        }
        if (slimPlayerModel == null) {
            slimPlayerModel = new PlayerEntityModel<>(context.getPart(EntityModelLayers.PLAYER_SLIM), true);
        }
    }

    public static void initializeBodyModels(EntityRendererFactory.Context context) {
        if (classicBodyModel == null) {
            classicBodyModel = new PlayerEntityModel<>(context.getPart(WatheModelLayers.PLAYER_BODY), false);
        }
        if (slimBodyModel == null) {
            slimBodyModel = new PlayerEntityModel<>(context.getPart(WatheModelLayers.PLAYER_BODY_SLIM), true);
        }
    }

    public static @Nullable PlayerEntityModel<AbstractClientPlayerEntity> getPlayerModel(@Nullable SkinTextures skinTextures) {
        if (skinTextures == null) {
            return null;
        }
        return skinTextures.model() == SkinTextures.Model.SLIM ? slimPlayerModel : classicPlayerModel;
    }

    public static @Nullable PlayerEntityModel<PlayerBodyEntity> getBodyModel(@Nullable SkinTextures skinTextures) {
        if (skinTextures == null) {
            return null;
        }
        return skinTextures.model() == SkinTextures.Model.SLIM ? slimBodyModel : classicBodyModel;
    }

    public static @Nullable SkinTextures resolveActivePlayerSkinTextures(AbstractClientPlayerEntity player) {
        if (JesterMomentClient.isActive()) {
            return null;
        }

        MorphlingPlayerComponent originalMorph = MorphlingPlayerComponent.KEY.get(player);
        if (originalMorph.getMorphTicks() > 0) {
            /*
             * NoellesRoles 原本的 Morphling 变形优先级更高。
             * 试剂作用在 Morphling 自己身上时只负责经济奖励和语音/外观副作用，
             * 不覆盖原技能正在进行的 35 秒变形，避免两套变形系统互相抢皮肤。
             */
            return null;
        }

        MorphMarkPlayerComponent component = MorphMarkPlayerComponent.KEY.get(player);
        UUID sampleUuid = component.sampleUuid();
        if (component.isActive() && sampleUuid != null) {
            return resolveOriginalSkinTextures(sampleUuid);
        }

        /*
         * 验尸官的尸体身份变形优先级最低：只有原 Morphling 和 SparkStrength 变形试剂都没生效，
         * 才使用验尸官自己的长期变形，满足“低于变形怪增强机制”的需求。
         */
        UUID coronerDisguiseUuid = CoronerService.activeDisguiseUuid(player);
        if (coronerDisguiseUuid != null) {
            return resolveOriginalSkinTextures(coronerDisguiseUuid);
        }
        return null;
    }

    public static @Nullable Text resolveActiveDisplayName(PlayerEntity player) {
        if (JesterMomentClient.isActive() || player.isInvisible()) {
            return null;
        }
        MorphlingPlayerComponent originalMorph = MorphlingPlayerComponent.KEY.get(player);
        if (originalMorph.corpseMode || originalMorph.getMorphTicks() > 0) {
            return null;
        }

        MorphMarkPlayerComponent component = MorphMarkPlayerComponent.KEY.get(player);
        UUID sampleUuid = component.sampleUuid();
        if (component.isActive() && sampleUuid != null) {
            return resolveDisplayName(sampleUuid, component.sampleName());
        }

        UUID coronerDisguiseUuid = CoronerService.activeDisguiseUuid(player);
        if (coronerDisguiseUuid != null) {
            return resolveDisplayName(coronerDisguiseUuid, "");
        }
        return null;
    }

    public static SkinTextures resolveBodySkinTextures(PlayerBodyEntity body) {
        if (shouldRevealOriginalBodyToLocalKiller()) {
            return resolveOriginalSkinTextures(body.getPlayerUuid());
        }

        MorphBodyDisguiseWorldComponent bodyComponent = MorphBodyDisguiseWorldComponent.KEY.get(body.getWorld());
        return bodyComponent.getDisguise(body.getPlayerUuid())
                .map(disguise -> resolveOriginalSkinTextures(disguise.disguiseUuid()))
                .orElseGet(() -> resolveOriginalSkinTextures(body.getPlayerUuid()));
    }

    public static Identifier resolveBodyTexture(PlayerBodyEntity body, Identifier fallbackTexture) {
        SkinTextures skinTextures = resolveBodySkinTextures(body);
        if (skinTextures != null && skinTextures.texture() != null) {
            return skinTextures.texture();
        }
        return fallbackTexture;
    }

    private static boolean shouldRevealOriginalBodyToLocalKiller() {
        ClientPlayerEntity localPlayer = MinecraftClient.getInstance().player;
        if (localPlayer == null || !WatheClient.isInstinctEnabled()) {
            return false;
        }
        if (!GameFunctions.isPlayerPlayingAndAlive(localPlayer)
                || !GameFunctions.isPlayerAliveAndSurvival(localPlayer)) {
            return false;
        }
        GameWorldComponent game = GameWorldComponent.KEY.get(localPlayer.getWorld());
        return game.canUseKillerFeatures(localPlayer);
    }

    private static SkinTextures resolveOriginalSkinTextures(@Nullable UUID uuid) {
        UUID targetUuid = uuid == null ? FALLBACK_BODY_SKIN_UUID : uuid;
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity localPlayer = client.player;

        if (localPlayer != null && localPlayer.networkHandler != null) {
            PlayerListEntry entry = localPlayer.networkHandler.getPlayerListEntry(targetUuid);
            if (entry != null) {
                return entry.getSkinTextures();
            }
        }

        if (WatheClient.PLAYER_ENTRIES_CACHE != null) {
            PlayerListEntry cachedEntry = WatheClient.PLAYER_ENTRIES_CACHE.get(targetUuid);
            if (cachedEntry != null) {
                return cachedEntry.getSkinTextures();
            }
        }

        return DefaultSkinHelper.getSkinTextures(targetUuid);
    }

    private static Text resolveDisplayName(UUID uuid, String fallbackName) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity localPlayer = client.player;

        if (localPlayer != null && localPlayer.networkHandler != null) {
            PlayerListEntry entry = localPlayer.networkHandler.getPlayerListEntry(uuid);
            if (entry != null) {
                return entry.getDisplayName() != null
                        ? entry.getDisplayName()
                        : Text.literal(entry.getProfile().getName());
            }
        }

        if (WatheClient.PLAYER_ENTRIES_CACHE != null) {
            PlayerListEntry cachedEntry = WatheClient.PLAYER_ENTRIES_CACHE.get(uuid);
            if (cachedEntry != null) {
                return cachedEntry.getDisplayName() != null
                        ? cachedEntry.getDisplayName()
                        : Text.literal(cachedEntry.getProfile().getName());
            }
        }

        return Text.literal(fallbackName == null || fallbackName.isBlank()
                ? uuid.toString().substring(0, 8)
                : fallbackName);
    }
}
