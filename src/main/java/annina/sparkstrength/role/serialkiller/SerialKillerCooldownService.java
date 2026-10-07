package annina.sparkstrength.role.serialkiller;

import dev.doctor4t.wathe.api.event.KillPlayer;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheItems;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.serialkiller.SerialKillerPlayerComponent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** 连环杀手成功击杀目标后移除原版刀与左轮的冷却条目。 */
public final class SerialKillerCooldownService {
    /**
     * 击杀事件与 NoellesRoles 的延迟刀 CD 写入不在同一个调用点。
     * 为避免世界 tick/CCA 组件 tick 的先后顺序导致清理过早结束，
     * 每次成功标记后连续保留数个 tick 的重试窗口。
     */
    private static final Map<UUID, Integer> PENDING_RESET = new HashMap<>();
    private static final int RESET_RETRY_TICKS = 4;
    private static boolean registered;

    private SerialKillerCooldownService() {}

    public static void register() {
        if (registered) return;
        registered = true;
        // BEFORE 先记录旧目标：NoellesRoles 自身的 AFTER 会立即重分配目标，
        // 若在 AFTER 才读取 currentTarget，看到的可能已经是新 UUID。
        KillPlayer.BEFORE.register((victim, killer, reason) -> {
            if (killer == null || !(killer.getWorld() instanceof ServerWorld)) return dev.doctor4t.wathe.api.event.KillPlayer.KillResult.allow();
            if (!GameFunctions.isPlayerPlayingAndAlive(killer)) return dev.doctor4t.wathe.api.event.KillPlayer.KillResult.allow();
            if (!org.agmas.noellesroles.Noellesroles.SERIAL_KILLER.equals(
                    dev.doctor4t.wathe.cca.GameWorldComponent.KEY.get(killer.getWorld()).getRole(killer))) return dev.doctor4t.wathe.api.event.KillPlayer.KillResult.allow();
            SerialKillerPlayerComponent comp = SerialKillerPlayerComponent.KEY.get(killer);
            if (comp.isCurrentTarget(victim.getUuid())) {
                synchronized (PENDING_RESET) {
                    PENDING_RESET.put(killer.getUuid(), RESET_RETRY_TICKS);
                }
            }
            return dev.doctor4t.wathe.api.event.KillPlayer.KillResult.allow();
        });
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_WORLD_TICK.register(world -> {
            synchronized (PENDING_RESET) {
                PENDING_RESET.entrySet().removeIf(entry -> {
                    PlayerEntity player = world.getPlayerByUuid(entry.getKey());
                    if (player == null) {
                        // END_WORLD_TICK 会对每个维度分别触发；玩家在其它维度时不能误删标记。
                        // 只有确认玩家已离线，才丢弃无法再处理的标记。
                        return world.getServer().getPlayerManager().getPlayer(entry.getKey()) == null;
                    }
                    if (player.getWorld() != world) return false;
                    clearCooldowns(player);
                    int remaining = entry.getValue() - 1;
                    if (remaining <= 0) return true;
                    entry.setValue(remaining);
                    return false;
                });
            }
        });
    }

    /**
     * 在 NoellesRoles 连环杀手组件自己的 serverTick 返回后执行清零。
     * 该时点晚于其“刀 CD 覆盖”代码，专门解决原版延迟写入覆盖 SparkStrength 清零的问题。
     */
    public static void applyComponentTickReset(PlayerEntity player) {
        if (player == null) return;
        synchronized (PENDING_RESET) {
            if (!PENDING_RESET.containsKey(player.getUuid())) return;
        }
        clearCooldowns(player);
    }

    private static void clearCooldowns(PlayerEntity player) {
        // set(item, 0) 不会明确删除 ItemCooldownManager.entries 中的旧条目；
        // remove 才会清除对应物品的冷却状态和客户端读条。
        player.getItemCooldownManager().remove(WatheItems.KNIFE);
        player.getItemCooldownManager().remove(WatheItems.REVOLVER);
    }
}
