package com.searmr.pmweatherflowingfluidscompat.mixin;


import com.bawnorton.mixinsquared.TargetHandler;
import dev.protomanly.pmweather.event.GameBusEvents;
import dev.protomanly.pmweather.weather.WeatherHandlerServer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FlowingFluid;
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
        WeatherHandlerServer handler = (WeatherHandlerServer) GameBusEvents.MANAGERS.get(instance.dimension());
        int topY = instance.getHeight(Heightmap.Types.WORLD_SURFACE,pos.getX(),pos.getZ());
        BlockPos topPos = new BlockPos(pos.getX(),topY,pos.getZ());
        if (handler != null && handler.getPrecipitation(topPos.getCenter()) > 0f) return false;
        return instance.setBlockAndUpdate(pos,state);
    }
}
