package com.searmr.pmweatherflowingfluidscompat.mixin;


import com.bawnorton.mixinsquared.TargetHandler;
import com.llamalad7.mixinextras.sugar.Local;
import com.searmr.pmweatherflowingfluidscompat.Config;
import com.searmr.pmweatherflowingfluidscompat.PmWeatherFlowingFluidsCompatServer;
import dev.protomanly.pmweather.event.GameBusEvents;
import dev.protomanly.pmweather.weather.WeatherHandlerServer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.WaterFluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(value = WaterFluid.class, priority = 1500)
public abstract class MixinWaterFluid extends FlowingFluid {

    @Shadow
    public abstract int getDropOff(final LevelReader levelReader);

    @Shadow
    public abstract boolean isSame(final Fluid fluid);


    @TargetHandler(
            mixin = "traben.flowing_fluids.mixin.mixins.MixinWaterFluid",
            name = " ff$tryBiomeFillOrDrain"
    )
    @Inject(method = "@MixinSquared:Handler",
    at = @At(value = "HEAD"), cancellable = true)
    private void fillBiomeDrainMixin(Level level, BlockPos blockPos, int amount, float chance, boolean isInfBiome, boolean isWithinInfBiomeHeights, boolean hasSkyLight, CallbackInfoReturnable<Boolean> cir) {
        WeatherHandlerServer handler = (WeatherHandlerServer) GameBusEvents.MANAGERS.get(level.dimension());
        int topY = level.getHeight(Heightmap.Types.WORLD_SURFACE,blockPos.getX(),blockPos.getZ());
        BlockPos topPos = new BlockPos(blockPos.getX(),topY,blockPos.getZ());
        if (handler.getPrecipitation(topPos.getCenter()) > 0f) cir.cancel();
    }

    @TargetHandler(
            mixin = "traben.flowing_fluids.mixin.mixins.MixinWaterFluid",
            name = " ff$tryEvaporate"
    )
    @Inject(method = "@MixinSquared:Handler",
            at = @At(value = "HEAD"), cancellable = true)
    private void tryEvaporateMixin(Level level, BlockPos blockPos, int amount, float chance, boolean isInfBiome, boolean isWithinInfBiomeHeights, boolean hasSkyLight, CallbackInfoReturnable<Boolean> cir) {
        WeatherHandlerServer handler = (WeatherHandlerServer) GameBusEvents.MANAGERS.get(level.dimension());
        if (!Config.waterDrainsRain && handler.getPrecipitation(blockPos.getCenter()) > 0) cir.cancel();
    }

    @TargetHandler(
            mixin = "traben.flowing_fluids.mixin.mixins.MixinWaterFluid",
            name = " ff$tryRainFill"
    )
    @Inject(method = "@MixinSquared:Handler",
            at = @At(value = "HEAD"), cancellable = true)
    private void tryRainFillMixin(Level level, BlockPos blockPos, float chance, boolean isInfBiome, boolean isWithinInfBiomeHeights, CallbackInfoReturnable<Boolean> cir) {
        cir.cancel();
    }
}