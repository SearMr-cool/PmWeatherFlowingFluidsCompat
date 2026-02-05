package com.searmr.pmweatherflowingfluidscompat;


import dev.protomanly.pmweather.PMWeather;
import dev.protomanly.pmweather.config.ServerConfig;
import dev.protomanly.pmweather.event.GameBusEvents;
import dev.protomanly.pmweather.weather.Storm;
import dev.protomanly.pmweather.weather.ThermodynamicEngine;
import dev.protomanly.pmweather.weather.WeatherHandler;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import traben.flowing_fluids.FFFluidUtils;
import traben.flowing_fluids.FlowingFluids;

import java.util.*;

import static dev.protomanly.pmweather.weather.ThermodynamicEngine.getPrecipitationType;


@EventBusSubscriber(modid = PmWeatherFlowingFluidsCompat.MODID, bus=EventBusSubscriber.Bus.GAME)
public class EventHandlerServer {
;
    @SubscribeEvent
public static void ServerTick(ServerTickEvent.Post event ) {
            List<ServerPlayer> players =  event.getServer().getPlayerList().getPlayers();

            for (ServerPlayer player : players) {
                var managers = GameBusEvents.MANAGERS;
                WeatherHandler handle = (WeatherHandler) managers.get(player.level().dimension());
                Level level = player.level();
             for (int i = 0; Config.realisticDownfall && i <= Config.maxPaddleRadius * 16.25f || !Config.realisticDownfall && i <= 2; i++) {
                 int randX = (int)(-Config.maxPaddleRadius + (Math.random() * Config.maxPaddleRadius * 2) );
                 int randZ = (int)(-Config.maxPaddleRadius + (Math.random() * Config.maxPaddleRadius * 2) );
                 Vec3 pos = new Vec3(player.position().x + randX,200,player.position().z + randZ);
                 int topMostBlock = player.level().getHeight(Heightmap.Types.WORLD_SURFACE,(int)pos.x,(int)pos.z);
                 BlockPos topBlock = new BlockPos((int)pos.x,topMostBlock,(int)pos.z);


                 float rainLevel = handle.getPrecipitation(topBlock.getCenter());
                 boolean isRaining = rainLevel > 0;


                 BlockPos blockPos = topBlock;
                 if (isRaining && level.canSeeSky(blockPos.above()) && !level.getBiome(blockPos).is(BiomeTags.HAS_VILLAGE_DESERT)) {
                     int amount = 0;
                     if (Config.realisticDownfall) {
                         int rad = Config.maxPaddleRadius * 2 + 1;
                         int totalArea = rad * rad;
                         float averageTime = (((float)totalArea / (int)(Config.maxPaddleRadius * 16.25f) * 20) / 50f);
                         amount = (int)((Config.maxRainDownfall * rainLevel * averageTime)/125f);
                     }
                     else if (rainLevel > (float)Config.minRainLevelPuddle){
                         Math.clamp((int)(FlowingFluidsCompat.maxRainAmount * (rainLevel - (float)Config.minRainLevelPuddle)),0, Config.maxWaterAmount);
                     }
                     if (Config.isAdaptive) FlowingFluidsCompat.tempRainArray.add(true);
                     FFFluidUtils.setFluidStateAtPosToNewAmount(level, blockPos, Fluids.WATER, amount);
                 }
             }
            }

        FlowingFluidsCompat.OnTick();
    if (Config.rainFillsBlocks && !FlowingFluids.config.rainFillsWaterHigherV2) {
        FlowingFluids.config.rainFillsWaterHigherV2=true;
    }
    else if (!Config.rainFillsBlocks && FlowingFluids.config.rainFillsWaterHigherV2) {
        FlowingFluids.config.rainFillsWaterHigherV2=false;
    }
}



}


