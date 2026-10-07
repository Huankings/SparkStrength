package annina.sparkstrength.client.role.professor;

import annina.sparkstrength.component.professor.ProfessorSerumTargetComponent;
import annina.sparkstrength.role.coroner.CoronerService;
import annina.sparkstrength.role.professor.ProfessorSerumRules;
import dev.doctor4t.wathe.api.event.GetInstinctHighlight;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.WatheClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.Noellesroles;

/**
 * 教授试剂相关的客户端事件。
 */
public final class ProfessorSerumClientHooks {
    private static boolean registered;

    private ProfessorSerumClientHooks() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        GetInstinctHighlight.EVENT.register(ProfessorSerumClientHooks::serumInstinctHighlight);
    }

    private static GetInstinctHighlight.HighlightResult serumInstinctHighlight(Entity entity) {
        if (!(entity instanceof PlayerEntity target)) {
            return null;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity localPlayer = client.player;
        if (localPlayer == null || !WatheClient.isPlayerPlayingAndAlive()) {
            return null;
        }

        ProfessorSerumTargetComponent targetComponent = ProfessorSerumTargetComponent.KEY.get(target);
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(localPlayer.getWorld());
        boolean localProfessor = gameWorld.isRole(localPlayer, Noellesroles.PROFESSOR)
                || CoronerService.hasProfessorDisguise(localPlayer);

        if (localProfessor) {
            int color = targetComponent.highestPriorityHighlightColor();
            if (color != -1) {
                return GetInstinctHighlight.HighlightResult.always(
                        color,
                        ProfessorSerumRules.SERUM_HIGHLIGHT_PRIORITY
                );
            }
            return null;
        }

        if (targetComponent.hasInvisibility()) {
            return GetInstinctHighlight.HighlightResult.always(
                    -1,
                    ProfessorSerumRules.INVISIBILITY_SKIP_PRIORITY
            );
        }
        return null;
    }
}
