package com.searmr.pmweatherflowingfluidscompat.mixin;

import com.searmr.pmweatherflowingfluidscompat.PmWeatherFlowingFluidsCompat;
import com.searmr.pmweatherflowingfluidscompat.PmWeatherFlowingFluidsCompatClient;
import dev.protomanly.pmweather.multiblock.MultiBlockHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.checkerframework.checker.units.qual.A;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiBlockHandler.class)
public class PmWeatherMixin {
    @Inject(method = {"update"}, at = {@At("HEAD")},cancellable = true)
    private static void waterCheck(BlockPos blockPos, LevelAccessor level, CallbackInfo ci) {
        BlockState baseBlock = level.getBlockState(blockPos);
        if (baseBlock.getBlock() instanceof LiquidBlock)  ci.cancel();
    }
}
