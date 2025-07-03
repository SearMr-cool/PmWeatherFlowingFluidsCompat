package com.searmr.pmweatherflowingfluidscompat;


import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.material.FlowingFluid;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import traben.flowing_fluids.FlowingFluids;
import traben.flowing_fluids.api.FlowingFluidsAPI;
import traben.flowing_fluids.config.FFConfig;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class FlowingFluidsCompat {
    public static List<Boolean> tempRainArray = new ArrayList<>();
    static boolean isRaining = false;
    public static int maxRainAmount = 0;
    static int tickDelay = 60;
    static int currentTick = 0;

    static float maxDrainChance  =  0.1f;
    static double targetTickTime = 30;

    static List<Float> stableDrainVales = new ArrayList<>();
    public static boolean modExist = false;

    static double GetTickTimeMs() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            return (double) server.getAverageTickTimeNanos() / 1000000; // nanoseconds to milliseconds
        }
        return 0.0;
    }

    public static void OnTick() {


        currentTick++;

        double tickTime = GetTickTimeMs();

        float originalVal = FlowingFluids.config.infiniteWaterBiomeDrainSurfaceChance;
        if (currentTick >= tickDelay) {

            if (tickTime <= Config.targetTps) {
                originalVal=Math.clamp(originalVal+0.001f,0,Config.maxDrainChance);

            }
            else originalVal=Math.clamp(originalVal + -0.001f,0,Config.maxDrainChance);;
            if (Config.isAdaptive) {

                UpdateRain();
                if (isRaining) {

                    if (tickTime <= targetTickTime) {

                        maxRainAmount = Math.clamp(maxRainAmount + 1, 0, Config.maxWaterAmount);
                    } else maxRainAmount = Math.clamp(maxRainAmount - 1, 0, Config.maxWaterAmount);
                } else maxRainAmount = 0;

            }   else maxRainAmount = Config.maxWaterAmount;
        currentTick=0;
        } else currentTick++;
        if (tickTime > 50 || isRaining && Config.drainingDisabledWhileRaining) originalVal=0f;
        if (originalVal != FlowingFluids.config.infiniteWaterBiomeDrainSurfaceChance) FlowingFluids.config.infiniteWaterBiomeDrainSurfaceChance=originalVal;

    }

    static void UpdateRain() {

        isRaining = !tempRainArray.isEmpty();
        tempRainArray.clear();

    }

}
