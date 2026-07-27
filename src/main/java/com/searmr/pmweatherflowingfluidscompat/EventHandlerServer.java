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
    static private Map<ChunkPos,List<BlockPos>> chunksStormSurge = new HashMap<>();
    @SubscribeEvent
    public static void ServerTick(ServerTickEvent.Post event ) {
        List<ServerPlayer> players =  event.getServer().getPlayerList().getPlayers();
        boolean rainingSomewhere = false;
        for (ServerPlayer player : players) {

            var managers = GameBusEvents.MANAGERS;
            WeatherHandler handle = (WeatherHandler) managers.get(player.level().dimension());
            Level level = player.level();
         if (stormSurgeActive) {
             new HashMap<>(chunksStormSurge).forEach((k,v) -> {
                 if (new Random().nextFloat() < 0.03f) {
                     new ArrayList<>(v).forEach((b) -> {
                         PmWeatherFlowingFluidsCompatServer.FLOWINGFLUIDSAPI.placeFluidAmountFromPos(level,b,Fluids.WATER, 1, true, false );
                     });
                 }
             });
         }
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

    public static void ChunkCheck(Level level, Vec3 pos) {
        BlockPos startPos = new BlockPos((int) (pos.x - Math.floorMod((int)pos.x,16)), (int) pos.y, (int) (pos.z - Math.floorMod((int)pos.z,16)));
        HashMap<BlockPos, CoastData> thing = new HashMap<>();
        int infBiomeCount = 0;
        int surfaceBlockCount = 0;
        // we iterate through all the top blocks of the chunk here
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                var tempPos = startPos.offset(x,0,z);
                int heightT = level.getHeight(Heightmap.Types.WORLD_SURFACE,tempPos.getX(),tempPos.getZ()) - 1;
                // set out init pos to be the surface block
                tempPos = new BlockPos(tempPos.getX(), heightT, tempPos.getZ());
                Holder<Biome> biome = level.getBiome(tempPos);
                // is the biome we are in an infbiome?
                boolean isInfBiome = PmWeatherFlowingFluidsCompatServer.FLOWINGFLUIDSAPI.doesBiomeInfiniteWaterRefill(biome);
                CoastData coastData = new CoastData(false,0);
                    BlockState state = level.getBlockState(tempPos);
                    // see if block in non infbiome is a solid block
                    if (!state.getFluidState().is(Fluids.WATER)) {
                        // we store the height in the depth variable as it has no use otherwise
                        coastData.land = true;
                        coastData.depth = heightT;
                        surfaceBlockCount++;
                    }
                    else {
                        // if it is water treat it as a infbiome block
                        int height = level.getHeight(Heightmap.Types.OCEAN_FLOOR,tempPos.getX(),tempPos.getZ());
                        int depth =  1 +    (level.getSeaLevel() - height);
                        if (depth >= 1) {
                            coastData.depth = depth;
                            infBiomeCount++;
                        }
                    }
                // put key value pair in here
                thing.put(tempPos,coastData);
            }
        }
        if (infBiomeCount > 1 && surfaceBlockCount > 1) {
            // here we find the locations on where storm surges can spawn
            thing.forEach((k,v) -> {
                if (v.land) {
                    // do depth check around land blocks and if water with a depth of x or higher is detected it is a storm surge spawn location
                        if (thing.containsKey(k.north()) && thing.get(k.north()).depth >= 1) addSurgeChunk(k.north());
                        if (thing.containsKey(k.east()) && thing.get(k.east()).depth >= 1) addSurgeChunk(k.east());
                        if (thing.containsKey(k.south()) && thing.get(k.south()).depth >= 1) addSurgeChunk(k.south());
                        if (thing.containsKey(k.west()) && thing.get(k.west()).depth >= 1) addSurgeChunk(k.west());
                }
            });
        }
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
        if (!chunkEvent.getLevel().isClientSide()) {
            ChunkCheck(chunkEvent.getChunk().getLevel(), chunkEvent.getChunk().getPos().getWorldPosition().getCenter());
        }
    }

    private static void addSurgeChunk(BlockPos blockPos) {
        ChunkPos chunkPos = new ChunkPos(blockPos);
        if (!chunksStormSurge.containsKey(chunkPos)) chunksStormSurge.put(chunkPos, new ArrayList<>());
        chunksStormSurge.get(chunkPos).add(blockPos);
    }
}


