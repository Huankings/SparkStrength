package annina.sparkstrength.network;

import annina.sparkstrength.SparkStrengthItems;
import annina.sparkstrength.role.serialkiller.SerialKillerConstants;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerPsychoComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.record.GameRecordManager;
import dev.doctor4t.wathe.util.ShootMuzzleS2CPayload;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.NotNull;

/** 连环手枪专用开火包，携带手位以支持主手/副手各自独立冷却。 */
public record SerialPistolShootC2SPayload(int targetId, Hand hand) implements CustomPayload {
    public static final Id<SerialPistolShootC2SPayload> ID = new Id<>(Identifier.of("sparkstrength", "serial_pistol_shoot"));
    public static final PacketCodec<RegistryByteBuf, SerialPistolShootC2SPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, SerialPistolShootC2SPayload::targetId,
            
            PacketCodecs.VAR_INT.xmap(i -> i == 1 ? Hand.OFF_HAND : Hand.MAIN_HAND, hand -> hand == Hand.OFF_HAND ? 1 : 0),
            SerialPistolShootC2SPayload::hand,
            (targetId, hand) -> new SerialPistolShootC2SPayload(targetId, hand)
    );

    @Override public Id<? extends CustomPayload> getId() { return ID; }

    public static void receive(SerialPistolShootC2SPayload payload, ServerPlayNetworking.Context context) {
        ServerPlayerEntity shooter = context.player();
        if (!GameFunctions.isPlayerPlayingAndAlive(shooter)) return;
        // 枪械物品可能被指令或其它模组复制；服务端仍必须验证“连环杀手正在疯魔”。
        if (!GameWorldComponent.KEY.get(shooter.getWorld()).isRole(shooter, Noellesroles.SERIAL_KILLER)
                || PlayerPsychoComponent.KEY.get(shooter).getPsychoTicks() <= 0) return;
        ItemStack stack = shooter.getStackInHand(payload.hand());
        boolean valid = payload.hand() == Hand.MAIN_HAND
                ? stack.isOf(SparkStrengthItems.serialPistol())
                : stack.isOf(SparkStrengthItems.serialLeftPistol());
        if (!valid || shooter.getItemCooldownManager().isCoolingDown(stack.getItem())) return;

        shooter.getWorld().playSound(null, shooter.getX(), shooter.getEyeY(), shooter.getZ(),
                dev.doctor4t.wathe.index.WatheSounds.ITEM_REVOLVER_CLICK,
                net.minecraft.sound.SoundCategory.PLAYERS, 0.5F, 1.0F);

        PlayerEntity target = null;
        Entity entity = payload.targetId() < 0 ? null : shooter.getServerWorld().getEntityById(payload.targetId());
        if (entity instanceof PlayerEntity candidate
                && candidate != shooter
                && GameFunctions.isPlayerAliveAndSurvival(candidate)
                && candidate.distanceTo(shooter) <= SerialKillerConstants.PISTOL_RANGE_BLOCKS) {
            target = candidate;
        }
        if (target != null && target instanceof ServerPlayerEntity serverTarget) {
            GameRecordManager.recordItemUse(shooter, net.minecraft.registry.Registries.ITEM.getId(stack.getItem()), serverTarget, null);
            GameFunctions.killPlayer(serverTarget, true, shooter, GameConstants.DeathReasons.GUN);
        }

        shooter.getWorld().playSound(null, shooter.getX(), shooter.getEyeY(), shooter.getZ(),
                dev.doctor4t.wathe.index.WatheSounds.ITEM_REVOLVER_SHOOT,
                net.minecraft.sound.SoundCategory.PLAYERS, 5.0F, 1.0F);
        for (ServerPlayerEntity tracking : PlayerLookup.tracking(shooter)) {
            ServerPlayNetworking.send(tracking, new ShootMuzzleS2CPayload(shooter.getUuidAsString()));
        }
        ServerPlayNetworking.send(shooter, new ShootMuzzleS2CPayload(shooter.getUuidAsString()));
        if (!shooter.isCreative()) {
            shooter.getItemCooldownManager().set(stack.getItem(), SerialKillerConstants.PISTOL_COOLDOWN_TICKS);
        }
    }
}
