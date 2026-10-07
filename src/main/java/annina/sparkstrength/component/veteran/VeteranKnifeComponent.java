package annina.sparkstrength.component.veteran;

import annina.sparkstrength.SparkStrength;
import annina.sparkstrength.role.veteran.VeteranRules;
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
 * SparkStrength 自己维护的老兵刀次数。
 *
 * <p>原版 {@code PlayerVeteranComponent} 只有 0/1/2 三种次数状态，适合“开局一把刀”，
 * 但无法表示商店无限购买后的多把刀累计次数。这里用单独组件保存总剩余次数：
 * 每买一把匕首 +2，每刺杀一次 -1，每消耗完一把刀的第二次时再移除一把实体刀。</p>
 *
 * <p>原版组件仍然会被服务层同步成“是否还有刀”的兼容标记，用于继续拦截老兵捡枪；
 * 但真实次数以这个组件为准。</p>
 */
public final class VeteranKnifeComponent implements AutoSyncedComponent {
    public static final ComponentKey<VeteranKnifeComponent> KEY = ComponentRegistry.getOrCreate(
            SparkStrength.id("veteran_knife"),
            VeteranKnifeComponent.class
    );

    private final PlayerEntity player;
    private int stabUsesLeft;

    public VeteranKnifeComponent(PlayerEntity player) {
        this.player = player;
    }

    public int getStabUsesLeft() {
        return stabUsesLeft;
    }

    public boolean hasStabUsesLeft() {
        return stabUsesLeft > 0;
    }

    public void initializeStartingKnife() {
        stabUsesLeft = VeteranRules.STAB_USES_PER_KNIFE;
        sync();
    }

    public void addKnife() {
        addStabUses(VeteranRules.STAB_USES_PER_KNIFE);
    }

    public void addStabUses(int uses) {
        if (uses <= 0) {
            return;
        }
        stabUsesLeft = Math.max(0, stabUsesLeft + uses);
        sync();
    }

    public boolean useStab() {
        if (stabUsesLeft <= 0) {
            return false;
        }
        stabUsesLeft--;
        sync();
        return true;
    }

    public void reset() {
        if (stabUsesLeft == 0) {
            return;
        }
        stabUsesLeft = 0;
        sync();
    }

    public void sync() {
        KEY.sync(player);
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity recipient) {
        // 目前次数只给老兵本人看；其他玩家不需要知道老兵还剩几刀。
        return recipient == player;
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        if (stabUsesLeft > 0) {
            tag.putInt("StabUsesLeft", stabUsesLeft);
        }
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        stabUsesLeft = tag.contains("StabUsesLeft", NbtElement.NUMBER_TYPE)
                ? Math.max(0, tag.getInt("StabUsesLeft"))
                : 0;
    }
}
