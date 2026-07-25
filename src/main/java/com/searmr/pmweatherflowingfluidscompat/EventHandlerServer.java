package com.searmr.pmweatherflowingfluidscompat;


import dev.protomanly.pmweather.event.GameBusEvents;
import dev.protomanly.pmweather.weather.ThermodynamicEngine;
import dev.protomanly.pmweather.weather.WeatherHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
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
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import traben.flowing_fluids.FlowingFluids;

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
            for (int i = 0; i <= 2; i++) {
                int randX = (int)(-Config.maxPaddleRadius + (Math.random() * Config.maxPaddleRadius * 2) );
                int randZ = (int)(-Config.maxPaddleRadius + (Math.random() * Config.maxPaddleRadius * 2) );
                Vec3 pos = new Vec3(player.position().x + randX,200,player.position().z + randZ);
                int topMostBlock = player.level().getHeight(Heightmap.Types.WORLD_SURFACE,(int)pos.x,(int)pos.z);
                BlockPos topBlock = new BlockPos((int)pos.x,topMostBlock,(int)pos.z);
                float rainLevel = handle.getPrecipitation(topBlock.getCenter());
                boolean isRaining = rainLevel > 0 && getPrecipitationType(handle,topBlock.getCenter(),level, 0).equals(ThermodynamicEngine.Precipitation.RAIN);
                if (isRaining) {
                    int amount = getAmountToPlace(rainLevel);
                    if (Config.isAdaptive) rainingSomewhere = true;
                    BlockState blockState = level.getBlockState(topBlock.below());
                    if (!blockState.getFluidState().is(Fluids.WATER) && amount > 1) {
                        PmWeatherFlowingFluidsCompatServer.FLOWINGFLUIDSAPI.modifyFluidAmountAtPos(level, topBlock, Fluids.WATER, 1);
                    }
                }
            }
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

    public static void ChunkCheck(ServerLevel level, Vec3 pos) {
        BlockPos startPos = new BlockPos((int) (pos.x - pos.x % 16), (int) pos.y, (int) (pos.z - pos.z % 16));
        HashMap<Vec2, CoastData> thing = new HashMap<>();
        int infBiomeCount = 0;
        int surfaceBlockCount = 0;
        for (int x = 0; x < 16; x++) {
            int tempX = startPos.getX() + x;
            for (int z = 0; z < 16; z++) {

                var tempPos = startPos.offset(tempX,0,z);
                int heightT = level.getHeight(Heightmap.Types.WORLD_SURFACE,tempPos.getX(),tempPos.getZ()) - 1;
                tempPos = tempPos.atY(heightT);
                Holder<Biome> biome = level.getBiome(tempPos);
                boolean isInfBiome = PmWeatherFlowingFluidsCompatServer.FLOWINGFLUIDSAPI.doesBiomeInfiniteWaterRefill(biome);
                CoastData coastData = new CoastData(false,0);
                if (isInfBiome) {
                    int height = level.getHeight(Heightmap.Types.OCEAN_FLOOR,tempPos.getX(),tempPos.getZ());
                    coastData.depth = level.getSeaLevel() - height;
                    infBiomeCount++;
                }
                else {
                    BlockState state = level.getBlockState(tempPos);
                    if (!state.getFluidState().is(Fluids.WATER)) {
                        coastData.land = true;
                        coastData.depth = heightT;
                        surfaceBlockCount++;
                    }
                    else {
                        int height = level.getHeight(Heightmap.Types.OCEAN_FLOOR,tempPos.getX(),tempPos.getZ());
                        coastData.depth = level.getSeaLevel() - height;
                    }
                }
                thing.put(new Vec2(tempPos.getX(),tempPos.getZ()),coastData);
            }
        }

        if (infBiomeCount > 1 && surfaceBlockCount > 1) {
            ArrayList<Integer> dirCount = new ArrayList<>();
            thing.forEach((k,v) -> {
                if (v.land) {
                    BlockPos basePos = new BlockPos((int)k.x,v.depth,(int)k.y);
                    FluidState stateN = level.getBlockState(basePos.north()).getFluidState();
                    FluidState stateE = level.getBlockState(basePos.east()).getFluidState();
                    FluidState stateS = level.getBlockState(basePos.south()).getFluidState();
                    FluidState stateW = level.getBlockState(basePos.west()).getFluidState();
                    if (stateN.is(Fluids.WATER)) dirCount.add(0);
                    if (stateE.is(Fluids.WATER)) dirCount.add(1);
                    if (stateS.is(Fluids.WATER)) dirCount.add(2);
                    if (stateW.is(Fluids.WATER)) dirCount.add(3);
                }
            });
            HashMap<String,Integer> storedDirections = new HashMap<>();
            storedDirections.put("north",Collections.frequency(dirCount,0));
            storedDirections.put("east",Collections.frequency(dirCount,1));
            storedDirections.put("south",Collections.frequency(dirCount,2));
            storedDirections.put("west",Collections.frequency(dirCount,3));
            List<Map.Entry<String,Integer>> list = new ArrayList<>(storedDirections.entrySet());
            list.sort(Map.Entry.comparingByValue());
            String direction = list.getLast().getKey();
            // direction logic to go here
        }
    }


}


