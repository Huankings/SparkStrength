package annina.sparkstrength.item;

import annina.sparkstrength.SparkStrengthEntities;
import annina.sparkstrength.entity.CaptureDeviceEntity;
import annina.sparkstrength.replay.SparkStrengthReplayFormatters;
import annina.sparkstrength.role.coroner.CoronerService;
import dev.doctor4t.wathe.record.GameRecordManager;
import dev.doctor4t.wathe.util.AdventureUsable;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

/**
 * Engineer capture-device item and placement entrypoint.
 * 工程师捕捉装置物品。
 *
 * <p>The item owns placement and spawning; the placed entity owns subsequent detection and effects.
 * <p>物品只负责“能否放置”和“生成实体”；后续检测、定身、报告与回放都交给
 * {@link CaptureDeviceEntity}，这样地板/天花板放置出来的装置使用同一套服务器权威逻辑。</p>
 */
public final class CaptureDeviceItem extends Item implements AdventureUsable {
    public CaptureDeviceItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        Direction side = context.getSide();
        if (side != Direction.UP && side != Direction.DOWN) {
            return ActionResult.PASS;
        }

        PlayerEntity player = context.getPlayer();
        if (player == null) {
            return ActionResult.PASS;
        }
        if (!CoronerService.canUseEngineerDisguiseItem(player)) {
            /*
             * 验尸官从工程师伪装商店购买的捕捉装置不跨伪装生效；
             * 物品保留在背包，但离开工程师伪装后不能继续放置。
             */
            return ActionResult.PASS;
        }

        World world = player.getWorld();
        if (!world.isClient()) {
            CaptureDeviceEntity entity = SparkStrengthEntities.captureDevice().create(world);
            if (entity == null) {
                return ActionResult.FAIL;
            }

            Vec3d hitPos = context.getHitPos();
            entity.setPosition(hitPos.x, hitPos.y, hitPos.z);
            entity.setYaw(player.getHeadYaw());
            entity.setOwnerUuid(player.getUuid());
            entity.setCeilingMounted(side == Direction.DOWN);
            world.spawnEntity(entity);

            if (player instanceof ServerPlayerEntity serverPlayer) {
                GameRecordManager.recordGlobalEvent(
                        serverPlayer.getServerWorld(),
                        SparkStrengthReplayFormatters.CAPTURE_DEVICE_PLACED,
                        serverPlayer,
                        null
                );
                serverPlayer.playSoundToPlayer(
                        SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                        SoundCategory.PLAYERS,
                        1.0F,
                        1.0F
                );
            }

            if (!player.isCreative()) {
                player.getStackInHand(context.getHand()).decrement(1);
            }
        }

        return ActionResult.SUCCESS;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("item.sparkstrength.capture_device.tooltip.line1")
                .styled(style -> style.withColor(0x808080).withItalic(false)));
        tooltip.add(Text.translatable("item.sparkstrength.capture_device.tooltip.line2")
                .styled(style -> style.withColor(0x808080).withItalic(false)));
        tooltip.add(Text.translatable("item.sparkstrength.capture_device.tooltip.line3")
                .styled(style -> style.withColor(0x808080).withItalic(false)));
    }
}
