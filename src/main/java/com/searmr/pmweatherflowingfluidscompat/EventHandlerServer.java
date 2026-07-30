package com.searmr.pmweatherflowingfluidscompat;


import dev.protomanly.pmweather.event.GameBusEvents;
import dev.protomanly.pmweather.weather.ThermodynamicEngine;
import dev.protomanly.pmweather.weather.WeatherHandler;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import traben.flowing_fluids.FlowingFluids;
import traben.flowing_fluids.api.FlowingFluidsAPI;

import java.util.*;

import static dev.protomanly.pmweather.weather.ThermodynamicEngine.getPrecipitationType;


@EventBusSubscriber(modid = PmWeatherFlowingFluidsCompatServer.MODID, bus=EventBusSubscriber.Bus.GAME)
public class EventHandlerServer {
    private static boolean stormSurgeActive = false;
    @SubscribeEvent
    public static void ServerTick(ServerTickEvent.Post event ) {
        List<ServerPlayer> players =  event.getServer().getPlayerList().getPlayers();
        boolean rainingSomewhere = false;
        for (ServerPlayer player : players) {

            var managers = GameBusEvents.MANAGERS;
            WeatherHandler handle = (WeatherHandler) managers.get(player.level().dimension());
            Level level = player.level();

         if (stormSurgeActive) {
             StormSurgeManager.createManager((ServerLevel) level);
             StormSurgeManager manager = StormSurgeManager.managers.get(level.dimension());
             manager.surgeChunks();
//             PmWeatherFlowingFluidsCompatServer.LOGGER.debug(Integer.toString(manager.getSurgeChunkAmount()));
         }
//            for (int i = 0; i <= 2; i++) {
//                int randX = (int)(-Config.maxPaddleRadius + (Math.random() * Config.maxPaddleRadius * 2) );
//                int randZ = (int)(-Config.maxPaddleRadius + (Math.random() * Config.maxPaddleRadius * 2) );
//                Vec3 pos = new Vec3(player.position().x + randX,200,player.position().z + randZ);
//                int topMostBlock = player.level().getHeight(Heightmap.Types.WORLD_SURFACE,(int)pos.x,(int)pos.z);
//                BlockPos topBlock = new BlockPos((int)pos.x,topMostBlock,(int)pos.z);
//                float rainLevel = handle.getPrecipitation(topBlock.getCenter());
//                boolean isRaining = rainLevel > 0 && getPrecipitationType(handle,topBlock.getCenter(),level, 0).equals(ThermodynamicEngine.Precipitation.RAIN);
//                if (isRaining) {
//                    int amount = getAmountToPlace(rainLevel);
//                    if (Config.isAdaptive) rainingSomewhere = true;
//                    BlockState blockState = level.getBlockState(topBlock.below());
//                    if (!blockState.getFluidState().is(Fluids.WATER) && amount > 1) {
//                        PmWeatherFlowingFluidsCompatServer.FLOWINGFLUIDSAPI.modifyFluidAmountAtPos(level, topBlock, Fluids.WATER, 1);
//                    }
//                }
//            }
            Vec3 playerPos = player.position();

        }
        FlowingFluidsCompat.OnTick(rainingSomewhere);


    }

    public static int getAmountToPlace(float rainLevel) {
        int amount = 0;
        if (Config.realisticDownfall) {
            amount = (int)(Config.maxRainDownfall * rainLevel * 0.946f);
        }
        else if (rainLevel > (float)Config.minRainLevelPuddle){
            amount = Math.clamp((int)(FlowingFluidsCompat.maxRainAmount * (rainLevel - (float)Config.minRainLevelPuddle)),0, Config.maxWaterAmount);
        }
        return amount;
    }


    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("ff_compat").executes(context -> {
            stormSurgeActive = !stormSurgeActive;
            context.getSource().sendSuccess(() -> Component.literal("Flowing fluids storm surge test"),false);
            return  1;
        }));
    }
    @SubscribeEvent
    public static void chunkLoad(ChunkEvent.Load chunkEvent) {
        Level level = (Level) chunkEvent.getLevel();
        if (!level.isClientSide()) {
            StormSurgeManager.createManager((ServerLevel) level);
           StormSurgeManager manager = StormSurgeManager.managers.get(level.dimension());
           manager.processChunk(chunkEvent.getChunk().getPos().getWorldPosition().getCenter());
        }
    }

    @SubscribeEvent
    public static void chunkUnload(ChunkEvent.Unload chunkEvent) {
        Level level = (Level) chunkEvent.getLevel();
        if (!chunkEvent.getLevel().isClientSide()) {
            StormSurgeManager manager = StormSurgeManager.managers.get(level.dimension());
            manager.removeSurgeChunk(chunkEvent.getChunk().getPos());
        }
    }
}


