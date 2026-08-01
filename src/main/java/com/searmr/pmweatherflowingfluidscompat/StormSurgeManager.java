package com.searmr.pmweatherflowingfluidscompat;

import dev.protomanly.pmweather.event.GameBusEvents;
import dev.protomanly.pmweather.weather.WindEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.GameEventTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

record ChunkKey(int x, int y) {}
public class StormSurgeManager {
    // yes I know this is similar to pmweathers handlers thingy, this was inspired from that
    public static Map<ResourceKey<Level>,StormSurgeManager> managers = new HashMap<>();
    private final ServerLevel level;
    private final ConcurrentHashMap<ChunkKey, Set<BlockPos>> chunksStormSurge = new ConcurrentHashMap<>();
    public static void createManager(ServerLevel level) {
        if (!managers.containsKey(level.dimension())) managers.put(level.dimension(),new StormSurgeManager(level));
    }

    public StormSurgeManager(ServerLevel level) {
        this.level = level;
    }

    private static double oldDepth = 20;
    private static double coastLine = 40 / 1.151 * 1852;
    private static double gravity = 9.81;

    public void surgeChunks() {
        Random random = new Random();
        chunksStormSurge.forEach((k,v) -> {
            ChunkPos chunkPos = new ChunkPos(k.x(),k.y());
            if (random.nextFloat() < 0.10 && Utils.withinPlayerRadius(level,chunkPos.getWorldPosition(),level.getServer().getPlayerList().getSimulationDistance() * 16 - 17)) {
                v.forEach((b) -> {
                        double wind = WindEngine.getWind(b,level).length()  * 0.447;
                        double dragC = getDragCoefficient(wind);
                        double stormFactor = getStormFactor(wind,dragC);
                        double stormSurge = getStormSurge(stormFactor);
                        float waterHeight = level.getHeight(Heightmap.Types.WORLD_SURFACE,b.getX(),b.getZ());
                        if (waterHeight >= level.getSeaLevel()) {
                            BlockState state = level.getBlockState(b.below());
                           waterHeight =  waterHeight - 1f + (1f / 8f * state.getFluidState().getAmount());
                        }
                        float currentSurge =   waterHeight -  level.getSeaLevel();
                        if (currentSurge < stormSurge) {
                            PmWeatherFlowingFluidsCompatServer.FLOWINGFLUIDSAPI.placeFluidAmountFromPos(level,b,Fluids.WATER,(int)(1f * stormSurge),true,false);
                        }

                });
            }
        });
    }

    public void processChunk(Vec3 pos) {
        BlockPos startPos = new BlockPos((int) (pos.x - Math.floorMod((int)pos.x,16)), (int) pos.y, (int) (pos.z - Math.floorMod((int)pos.z,16)));
        HashMap<ChunkKey, CoastData> thing = new HashMap<>();
        int infBiomeCount = 0;
        int surfaceBlockCount = 0;
        boolean hasBiome = false;
        // we iterate through all the top blocks of the chunk here
        start : for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                var tempPos = startPos.offset(x,0,z);
                int heightT = level.getHeight(Heightmap.Types.OCEAN_FLOOR,tempPos.getX(),tempPos.getZ());
                // set out init pos to be the surface block
                tempPos = new BlockPos(tempPos.getX(), heightT, tempPos.getZ());// is the biome we are in an infbiome?
                CoastData coastData = new CoastData(false,0);
                BlockState state = level.getBlockState(tempPos);
                // see if block in non infbiome is a solid block

                if (state.getFluidState().getAmount() < 8 && heightT >= level.getSeaLevel()) {
                    // we store the height in the depth variable as it has no use otherwise
                    coastData.land = true;
                    coastData.y = heightT - 1;
                    surfaceBlockCount++;
                }
                else if (state.getFluidState().is(FluidTags.WATER) && state.getFluidState().getAmount() >= 8) {
                    coastData.y = heightT;
                    // if it is water treat it as a infbiome block
                    int depth =  1 +    (level.getSeaLevel() - tempPos.getY());

                    if (depth >= Config.minDepthSurge) infBiomeCount++;
                }
                else continue;
                if (Config.stormSurgeBiomeCheck) {
                   Holder<Biome> biome =  level.getBiome(tempPos);
                   if (biome.is(BiomeTags.IS_RIVER)) {
                       hasBiome = false;
                       break start;
                   }
                   if (biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_BEACH) || biome.is(Biomes.STONY_SHORE)) hasBiome = true;

                }
                // put key value pair in here
                thing.put(new ChunkKey(tempPos.getX(),tempPos.getZ()),coastData);
            }
        }
        if (Config.stormSurgeBiomeCheck ? hasBiome && surfaceBlockCount > 1 : infBiomeCount > 3 && surfaceBlockCount > 3) {
            // here we find the locations on where storm surges can spawn
            thing.forEach((k,v) -> {
                if (v.land) {
                    BlockPos blockPos = new BlockPos((int)k.x(),v.y,(int)k.y());
                    // do depth check around land blocks and if water with a depth of x or higher is detected it is a storm surge spawn location
                    int[][] offsets = {{0,-1},{1,0},{0,1},{-1,0}};
                    Arrays.stream(offsets).forEach((v2 -> {
                        ChunkKey nextKey = new ChunkKey(k.x() + v2[0], k.y() + v2[1]);
                        if (thing.containsKey(nextKey)) {
                            CoastData cData = thing.get(nextKey);
                            if (!cData.land) {
                                addSurgeChunk(new BlockPos((int)nextKey.x(),cData.y,(int)nextKey.y()));
                                PmWeatherFlowingFluidsCompatServer.LOGGER.debug("Added chunk");
                            }
                        }
                    }));
                }
            });
        }
    }

    private void addSurgeChunk(BlockPos blockPos) {
        ChunkPos chunkPos = new ChunkPos(blockPos);
        ChunkKey chunkKey = new ChunkKey(chunkPos.x,chunkPos.z);
        if (!chunksStormSurge.containsKey(chunkKey)) chunksStormSurge.put(new ChunkKey(chunkPos.x,chunkPos.z), ConcurrentHashMap.newKeySet());
        chunksStormSurge.get(chunkKey).add(blockPos);
    }

    public void removeSurgeChunk(ChunkPos chunkPos) {
        chunksStormSurge.remove(chunkPos);
    }

    double getDragCoefficient(double wind) {
        return 1.2 * Math.pow(10,-6) + 2.25 * Math.pow(10,-6) * Math.pow(1 - (5.6/wind),2);
    }
    double getStormFactor(double wind, double dragC) {
        return Math.pow(1.15 * coastLine * dragC * wind,2) / Math.pow(gravity * oldDepth,2);
    }

    double getStormSurge(double stormFactor) {
        return oldDepth * Math.sqrt(1 + 2 * stormFactor - 1);
    }

    public int getSurgeChunkAmount() {
        return chunksStormSurge.size();
    }

    public boolean chunkRegistered(ChunkPos chunkPos) {
        return chunksStormSurge.containsKey(new ChunkKey(chunkPos.x,chunkPos.z));
    }
}
