package com.searmr.pmweatherflowingfluidscompat.mixin;


import com.bawnorton.mixinsquared.TargetHandler;
import com.searmr.pmweatherflowingfluidscompat.Config;
import com.searmr.pmweatherflowingfluidscompat.Utils;
import dev.protomanly.pmweather.event.GameBusEvents;
import dev.protomanly.pmweather.weather.WeatherHandlerServer;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = FlowingFluid.class, priority = 1500)
public class MixinFlowingFluid {
    @TargetHandler(
            mixin = "traben.flowing_fluids.mixin.mixins.MixinFlowingFluid",
            name = "ff$tickMixin"
    )
    @Redirect(method = "@MixinSquared:Handler",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
        private boolean rainCheck(Level instance, BlockPos pos, BlockState state) {
        if (!Config.waterDrainsRain) {
            WeatherHandlerServer handler = (WeatherHandlerServer) GameBusEvents.MANAGERS.get(instance.dimension());
            if (handler != null && handler.getPrecipitation(pos.getCenter()) > 0f) return false;
        }
        return instance.setBlockAndUpdate(pos,state);
    }
}
