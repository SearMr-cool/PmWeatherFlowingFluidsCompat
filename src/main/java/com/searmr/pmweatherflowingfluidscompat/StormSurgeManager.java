package com.searmr.pmweatherflowingfluidscompat;

import dev.protomanly.pmweather.weather.WindEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
record ChunkKey(int x, int y) {}
public class StormSurgeManager {
    // yes I know this is similar to pmweathers handlers thingy, this was inspired from that
    public static Map<ResourceKey<Level>,StormSurgeManager> managers = new HashMap<>();
    private final ServerLevel level;
    private final ConcurrentHashMap<ChunkPos, Set<BlockPos>> chunksStormSurge = new ConcurrentHashMap<>();
    public static void createManager(ServerLevel level) {
        if (!managers.containsKey(level.dimension())) managers.put(level.dimension(),new StormSurgeManager(level));
    }

    public StormSurgeManager(ServerLevel level) {
        this.level = level;
    }

    static double oldDepth = 13;
    static double coastLine = 25000;
    static double gravity = 9.81;

    public void surgeChunks() {
        Random random = new Random();
        chunksStormSurge.forEach((k,v) -> {
            if (random.nextFloat() < 0.02) {
                v.forEach((b) -> {
                 if (level.shouldTickBlocksAt(b)) {
                     double wind = WindEngine.getWind(b,level).length();
                     double dragC = getDragCoefficient(wind);
                     double stormFactor = getStormFactor(wind,dragC);
                     double stormSurge = getStormSurge(stormFactor);
                     int currentSurge = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG,b.getX(),b.getZ()) - level.getSeaLevel();
                     if (currentSurge - 1 < stormSurge) {
                         PmWeatherFlowingFluidsCompatServer.FLOWINGFLUIDSAPI.placeFluidAmountFromPos(level,b,Fluids.WATER,3,true,false);
                     }
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
        // we iterate through all the top blocks of the chunk here
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                var tempPos = startPos.offset(x,0,z);
                int heightT = level.getHeight(Heightmap.Types.WORLD_SURFACE,tempPos.getX(),tempPos.getZ()) - 1;
                // set out init pos to be the surface block
                tempPos = new BlockPos(tempPos.getX(), heightT, tempPos.getZ());
                Holder<Biome> biome = level.getBiome(tempPos);
                // is the biome we are in an infbiome?
                CoastData coastData = new CoastData(false,0);
                BlockState state = level.getBlockState(tempPos);
                // see if block in non infbiome is a solid block
                if (!state.getFluidState().is(Fluids.WATER)) {
                    // we store the height in the depth variable as it has no use otherwise
                    coastData.land = true;
                    coastData.y = heightT;
                    surfaceBlockCount++;
                }
                else {
                    // if it is water treat it as a infbiome block
                    int height = level.getHeight(Heightmap.Types.OCEAN_FLOOR,tempPos.getX(),tempPos.getZ());
                    int depth =  1 +    (level.getSeaLevel() - height);
                    if (depth >= 1) {
                        coastData.y = height;
                        infBiomeCount++;
                    }
                }
                // put key value pair in here
                thing.put(new ChunkKey(tempPos.getX(),tempPos.getZ()),coastData);
            }
        }
        if (infBiomeCount > 3 && surfaceBlockCount > 3) {
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
        if (!chunksStormSurge.containsKey(chunkPos)) chunksStormSurge.put(chunkPos, ConcurrentHashMap.newKeySet());
        chunksStormSurge.get(chunkPos).add(blockPos);
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
}
