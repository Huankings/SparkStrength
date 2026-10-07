package annina.sparkstrength.mixin.bomber;

import annina.sparkstrength.role.bomber.BomberTrapService;
import annina.sparkstrength.role.bomber.TimedBombTrapHolder;
import dev.doctor4t.wathe.block_entity.BeveragePlateBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * 给 wathe 托盘方块实体补一个“预埋定时炸弹”的同步状态。
 */
@Mixin(BeveragePlateBlockEntity.class)
public abstract class TimedBombTrayStateMixin extends BlockEntity implements TimedBombTrapHolder {
    @Unique
    private UUID sparkstrength$timedBombTrapOwner;

    private TimedBombTrayStateMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public boolean sparkstrength$hasTimedBombTrap() {
        return this.sparkstrength$timedBombTrapOwner != null;
    }

    @Override
    public @Nullable UUID sparkstrength$getTimedBombTrapOwner() {
        return this.sparkstrength$timedBombTrapOwner;
    }

    @Override
    public void sparkstrength$setTimedBombTrapOwner(@Nullable UUID owner) {
        this.sparkstrength$timedBombTrapOwner = owner;
        if (owner != null) {
            BomberTrapService.rememberTrap(this.world, this.pos);
        } else {
            BomberTrapService.forgetTrap(this.world, this.pos);
        }
        this.sparkstrength$syncTimedBombTrap();
    }

    @Inject(method = "writeNbt", at = @At("TAIL"))
    private void sparkstrength$writeTimedBombTrap(
            NbtCompound nbt,
            RegistryWrapper.WrapperLookup registryLookup,
            CallbackInfo ci
    ) {
        if (this.sparkstrength$timedBombTrapOwner != null) {
            nbt.putUuid(BomberTrapService.TRAP_OWNER_NBT_KEY, this.sparkstrength$timedBombTrapOwner);
        } else {
            nbt.remove(BomberTrapService.TRAP_OWNER_NBT_KEY);
        }
    }

    @Inject(method = "readNbt", at = @At("TAIL"))
    private void sparkstrength$readTimedBombTrap(
            NbtCompound nbt,
            RegistryWrapper.WrapperLookup registryLookup,
            CallbackInfo ci
    ) {
        this.sparkstrength$timedBombTrapOwner = nbt.containsUuid(BomberTrapService.TRAP_OWNER_NBT_KEY)
                ? nbt.getUuid(BomberTrapService.TRAP_OWNER_NBT_KEY)
                : null;
    }

    @Unique
    private void sparkstrength$syncTimedBombTrap() {
        if (this.world != null && !this.world.isClient) {
            this.markDirty();
            this.world.updateListeners(this.pos, this.getCachedState(), this.getCachedState(), 3);
        }
    }
}
