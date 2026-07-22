package com.searmr.pmweatherflowingfluidscompat;


import dev.protomanly.pmweather.event.GameBusEvents;
import dev.protomanly.pmweather.weather.ThermodynamicEngine;
import dev.protomanly.pmweather.weather.WeatherHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.fluids.FluidType;
import traben.flowing_fluids.FFFluidUtils;
import traben.flowing_fluids.FlowingFluids;
import traben.flowing_fluids.api.FlowingFluidsAPI;
import traben.flowing_fluids.api.FlowingFluidsApiImpl;

import java.util.*;

import static dev.protomanly.pmweather.weather.ThermodynamicEngine.getPrecipitationType;


@EventBusSubscriber(modid = PmWeatherFlowingFluidsCompatServer.MODID, bus=EventBusSubscriber.Bus.GAME)
public class EventHandlerServer {
    @SubscribeEvent
public static void ServerTick(ServerTickEvent.Post event ) {
            List<ServerPlayer> players =  event.getServer().getPlayerList().getPlayers();
            boolean rainingSomewhere = false;
            for (ServerPlayer player : players) {
                var managers = GameBusEvents.MANAGERS;
                WeatherHandler handle = (WeatherHandler) managers.get(player.level().dimension());
                Level level = player.level();

             for (int i = 0; Config.realisticDownfall && i <= Config.maxPaddleRadius / 16.25f || !Config.realisticDownfall && i <= 2; i++) {
                 int randX = (int)(-Config.maxPaddleRadius + (Math.random() * Config.maxPaddleRadius * 2) );
                 int randZ = (int)(-Config.maxPaddleRadius + (Math.random() * Config.maxPaddleRadius * 2) );
                 Vec3 pos = new Vec3(player.position().x + randX,200,player.position().z + randZ);
                 int topMostBlock = player.level().getHeight(Heightmap.Types.WORLD_SURFACE,(int)pos.x,(int)pos.z);
                 BlockPos topBlock = new BlockPos((int)pos.x,topMostBlock,(int)pos.z);


                 float rainLevel = handle.getPrecipitation(topBlock.getCenter());
                 boolean isRaining = rainLevel > 0 && getPrecipitationType(handle,topBlock.getCenter(),level, 0).equals(ThermodynamicEngine.Precipitation.RAIN);


                 BlockPos blockPos = topBlock;
                 if (level.random.nextFloat() < Math.min(FlowingFluids.config.rainRefillChance, FlowingFluids.config.evaporationChanceV2 / 3.0F) && isRaining && level.canSeeSky(blockPos.above())) {
                     int amount = getAmount(rainLevel);
                     if (Config.isAdaptive) rainingSomewhere = true;
                     BlockState blockState = level.getBlockState(blockPos.below());
                   if (!blockState.getFluidState().is(Fluids.WATER)) {
                       PmWeatherFlowingFluidsCompatServer.FLOWINGFLUIDSAPI.modifyFluidAmountAtPos(level, blockPos, Fluids.WATER, amount > 0 ? 1 : 0);
                       if (amount > 1) placeWaterRecursive(level,blockPos,amount - 1);}
                   else {
                       placeWaterRecursive(level,blockPos.below(),amount);
                   }
                 }
             }
            }

        FlowingFluidsCompat.OnTick(rainingSomewhere);
//    if (Config.rainFillsBlocks && !FlowingFluids.config.rainFillsWaterHigherV2) {
//        FlowingFluids.config.rainFillsWaterHigherV2=true;
//    }
//    else if (!Config.rainFillsBlocks && FlowingFluids.config.rainFillsWaterHigherV2) {
//        FlowingFluids.config.rainFillsWaterHigherV2=false;
//    }
}


private static void placeWaterRecursive(Level level, BlockPos blockPos, int amount) {
        if (amount > 0) {
            placeWaterRecursive(level,blockPos,PmWeatherFlowingFluidsCompatServer.FLOWINGFLUIDSAPI.placeFluidAmountFromPos(level, blockPos, Fluids.WATER, amount - 1,true,true));
        }
    }

    private static int getAmount(float rainLevel) {
        int amount = 0;
        if (Config.realisticDownfall) {
            int rad = Config.maxPaddleRadius * 2 + 1;
            int totalArea = rad * rad;
            float averageTime = (((float)totalArea / (int)(Config.maxPaddleRadius / 16.25f * 20)) / 50f);
            amount = (int)((Config.maxRainDownfall * rainLevel * averageTime)/125f);

        }
        else if (rainLevel > (float)Config.minRainLevelPuddle){
            amount = Math.clamp((int)(FlowingFluidsCompat.maxRainAmount * (rainLevel - (float)Config.minRainLevelPuddle)),0, Config.maxWaterAmount);
        }
        return amount;
    }


}


