package annina.sparkstrength.component.toxicologist;

import annina.sparkstrength.SparkStrength;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

/**
 * 毒理学家解毒剂冷却抵扣池。
 *
 * <p>需求中的“每中毒 1 个玩家减少解毒剂冷却 120 秒”需要能在无冷却时积攒，
 * 并且当前冷却被清空后如果还有剩余秒数，还要继续留到下一次解毒剂使用后生效。
 * 因此这里不直接保存“人数”，而是保存尚未消费完的冷却抵扣 tick 数。</p>
 */
public final class ToxicologistAntidoteComponent implements AutoSyncedComponent {
    public static final ComponentKey<ToxicologistAntidoteComponent> KEY = ComponentRegistry.getOrCreate(
            SparkStrength.id("toxicologist_antidote"),
            ToxicologistAntidoteComponent.class
    );

    private final PlayerEntity player;
    private int storedReductionTicks;

    public ToxicologistAntidoteComponent(PlayerEntity player) {
        this.player = player;
    }

    public int getStoredReductionTicks() {
        return storedReductionTicks;
    }

    public boolean hasStoredReduction() {
        return storedReductionTicks > 0;
    }

    public void addStoredReductionTicks(int ticks) {
        if (ticks <= 0) {
            return;
        }
        storedReductionTicks = Math.max(0, storedReductionTicks + ticks);
        sync();
    }

    public void setStoredReductionTicks(int ticks) {
        storedReductionTicks = Math.max(0, ticks);
        sync();
    }

    public void clear() {
        if (storedReductionTicks == 0) {
            return;
        }
        storedReductionTicks = 0;
        sync();
    }

    private void sync() {
        KEY.sync(player);
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity recipient) {
        // 抵扣池属于毒理学家本人信息；当前没有 UI 使用它，仍只同步本人以避免无关信息外泄。
        return recipient == player;
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        if (storedReductionTicks > 0) {
            tag.putInt("StoredReductionTicks", storedReductionTicks);
        }
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        storedReductionTicks = tag.contains("StoredReductionTicks", NbtElement.NUMBER_TYPE)
                ? Math.max(0, tag.getInt("StoredReductionTicks"))
                : 0;
    }
}
