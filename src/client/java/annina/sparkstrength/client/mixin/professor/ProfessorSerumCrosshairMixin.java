package annina.sparkstrength.client.mixin.professor;

import annina.sparkstrength.role.professor.ProfessorSerumRules;
import annina.sparkstrength.role.professor.ProfessorSerumType;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.doctor4t.wathe.client.gui.CrosshairRenderer;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 教授试剂专用准心。
 *
 * <p>试剂物品主类在 main 源集，不能引用客户端渲染类；所以准心变化放在 client mixin。
 * 当 1.5 格内准星命中可投喂玩家时，显示 Wathe 的目标准心，否则显示普通准心。</p>
 */
@Mixin(CrosshairRenderer.class)
public abstract class ProfessorSerumCrosshairMixin {
    @Unique private static final Identifier CROSSHAIR = Identifier.of("wathe", "hud/crosshair");
    @Unique private static final Identifier CROSSHAIR_TARGET = Identifier.of("wathe", "hud/crosshair_target");

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private static void sparkstrength$renderProfessorSerumCrosshair(
            @NotNull MinecraftClient client,
            @NotNull ClientPlayerEntity player,
            @NotNull DrawContext context,
            @NotNull RenderTickCounter tickCounter,
            @NotNull CallbackInfo ci
    ) {
        if (!client.options.getPerspective().isFirstPerson()
                || ProfessorSerumType.byItem(player.getMainHandStack()) == null) {
            return;
        }

        ci.cancel();

        boolean target = ProjectileUtil.getCollision(
                player,
                entity -> entity instanceof PlayerEntity targetPlayer
                        && targetPlayer != player
                        && GameFunctions.isPlayerPlayingAndAlive(targetPlayer)
                        && GameFunctions.isPlayerAliveAndSurvival(targetPlayer),
                ProfessorSerumRules.FEED_RANGE
        ) instanceof EntityHitResult;

        context.getMatrices().push();
        context.getMatrices().translate(context.getScaledWindowWidth() / 2F, context.getScaledWindowHeight() / 2F, 0.0F);
        context.getMatrices().push();
        context.getMatrices().translate(-1.5F, -1.5F, 0.0F);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SrcFactor.ONE_MINUS_DST_COLOR,
                GlStateManager.DstFactor.ONE_MINUS_SRC_COLOR,
                GlStateManager.SrcFactor.ONE,
                GlStateManager.DstFactor.ZERO
        );
        context.drawGuiTexture(target ? CROSSHAIR_TARGET : CROSSHAIR, 0, 0, 3, 3);
        context.getMatrices().pop();
        context.getMatrices().pop();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }
}
