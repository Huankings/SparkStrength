package annina.sparkstrength.mixin.waiter;

import annina.sparkstrength.role.waiter.WaiterHelpingEconomyService;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import org.agmas.noellesroles.Noellesroles;
import dev.doctor4t.wathe.item.CocktailItem;
import org.spongepowered.asm.mixin.Mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import java.util.UUID;

/**
 * 给 Spark 版 NoellesRoles 原有喂食流程补充“帮助别人完成任务”的 50 金币。
 *
 * <p>原版服务员 Mixin 会在 {@code useOnEntity} 内直接把目标任务标记为完成，随后
 * Wathe 在下一次 tick 触发任务事件。因此这里在调用原逻辑前记录“原本未完成的目标任务”，
 * 在原逻辑返回后确认任务已经被喂食逻辑标记完成，避免目标没有对应需求时误发奖励。</p>
 */
@Mixin(value = Item.class, priority = 1100)
public abstract class WaiterFeedingRewardMixin {
    @WrapMethod(method = "useOnEntity")
    private net.minecraft.util.ActionResult sparkstrength$rewardAfterSuccessfulFeeding(
            ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand,
            Operation<net.minecraft.util.ActionResult> original
    ) {
        if (user.getWorld().isClient()
                || !(user instanceof ServerPlayerEntity waiter)
                || !(entity instanceof ServerPlayerEntity target)
                || !GameWorldComponent.KEY.get(user.getWorld()).isRole(waiter, Noellesroles.WAITER)) {
            return original.call(stack, user, entity, hand);
        }

        boolean isDrink = stack.getItem() instanceof CocktailItem;
        boolean isFood = !isDrink && stack.get(DataComponentTypes.FOOD) != null;
        if (!isDrink && !isFood) {
            return original.call(stack, user, entity, hand);
        }

        PlayerMoodComponent.Task taskType = isDrink
                ? PlayerMoodComponent.Task.DRINK : PlayerMoodComponent.Task.EAT;
        PlayerMoodComponent.TrainTask taskBefore = PlayerMoodComponent.KEY.get(target).tasks.get(taskType);
        boolean wasPending = taskBefore != null && !taskBefore.isFulfilled(target);

        net.minecraft.util.ActionResult result = original.call(stack, user, entity, hand);
        PlayerMoodComponent.TrainTask taskAfter = PlayerMoodComponent.KEY.get(target).tasks.get(taskType);
        if (wasPending && taskAfter != null && taskAfter.isFulfilled(target)) {
            WaiterHelpingEconomyService.reward(waiter);
        }
        return result;
    }
}
