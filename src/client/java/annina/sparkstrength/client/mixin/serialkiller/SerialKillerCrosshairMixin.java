package annina.sparkstrength.client.mixin.serialkiller;

import annina.sparkstrength.SparkStrengthItems;
import annina.sparkstrength.item.SerialPistolItem;
import dev.doctor4t.wathe.client.gui.CrosshairRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 连环手枪使用 Wathe 同款准心，并按 30 格射程判断目标。 */
@Mixin(CrosshairRenderer.class)
public abstract class SerialKillerCrosshairMixin {
    @Unique private static final Identifier NORMAL = Identifier.of("wathe", "hud/crosshair");
    @Unique private static final Identifier TARGET = Identifier.of("wathe", "hud/crosshair_target");

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private static void sparkstrength$renderSerialPistolCrosshair(MinecraftClient client, ClientPlayerEntity player,
                                                                    DrawContext context, RenderTickCounter tickCounter,
                                                                    CallbackInfo ci) {
        ItemStack stack = player.getMainHandStack();
        if (!client.options.getPerspective().isFirstPerson()
                || (!stack.isOf(SparkStrengthItems.serialPistol()) && !stack.isOf(SparkStrengthItems.serialLeftPistol()))) return;
        ci.cancel();
        boolean hit = !player.getItemCooldownManager().isCoolingDown(stack.getItem())
                && SerialPistolItem.findTarget(player) != null;
        context.getMatrices().push();
        context.getMatrices().translate(context.getScaledWindowWidth() / 2F, context.getScaledWindowHeight() / 2F, 0.0F);
        context.getMatrices().translate(-1.5F, -1.5F, 0.0F);
        context.drawGuiTexture(hit ? TARGET : NORMAL, 0, 0, 3, 3);
        context.getMatrices().pop();
    }
}
