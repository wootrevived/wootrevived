package wootrevived.woot.mixins.impl;

import net.minecraft.core.Holder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wootrevived.woot.drops.simulator.DropSimulator;
import wootrevived.woot.drops.simulator.DropSimulatorDimension;
import wootrevived.woot.mixins.accessors.LevelMixinAccessor;

@Mixin(Level.class)
public abstract class LevelMixin implements LevelMixinAccessor {
    @Shadow @Final @Mutable
    private ResourceKey<Level> dimension;

    @Shadow @Final @Mutable
    private Holder<DimensionType> dimensionTypeRegistration;

    public void woot$setDimension(ResourceKey<Level> dimension, Holder<DimensionType> dimensionTypeRegistration) {
        this.dimension = dimension;
        this.dimensionTypeRegistration = dimensionTypeRegistration;
    }

    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", at = @At("HEAD"), cancellable = true)
    private void woot$preventDropSimulatorBlockSet(BlockPos pos, BlockState state, int flags, int recursionLeft, CallbackInfoReturnable<Boolean> cir) {
        if(woot$isDropSimulatorLevel())
            cir.setReturnValue(true);
    }

    @Inject(method = "removeBlock(Lnet/minecraft/core/BlockPos;Z)Z", at = @At("HEAD"), cancellable = true)
    private void woot$preventDropSimulatorBlockRemove(BlockPos pos, boolean isMoving, CallbackInfoReturnable<Boolean> cir) {
        if(woot$isDropSimulatorLevel())
            cir.setReturnValue(true);
    }

    @Inject(method = "destroyBlock(Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/entity/Entity;I)Z", at = @At("HEAD"), cancellable = true)
    private void woot$preventDropSimulatorBlockDestroy(BlockPos pos, boolean dropBlock, Entity entity, int recursionLeft, CallbackInfoReturnable<Boolean> cir) {
        if(woot$isDropSimulatorLevel())
            cir.setReturnValue(true);
    }

    private boolean woot$isDropSimulatorLevel() {
        return DropSimulator.isLevel((Level)(Object)this) || dimension.equals(DropSimulatorDimension.DROP_SIMULATOR_LEVEL);
    }
}
