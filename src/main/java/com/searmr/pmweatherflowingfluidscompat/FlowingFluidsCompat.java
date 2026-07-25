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
    static boolean isRaining = true;
    public static int maxRainAmount = 0;
    static int tickDelay = 60;
    static int currentTick = 0;
    static double GetTickTimeMs() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            return (double) server.getAverageTickTimeNanos() / 1000000; // nanoseconds to milliseconds
        }
        return 0.0;
    }
    public static void OnTick(boolean rainingSomewhere) {
        currentTick++;
        double tickTime = GetTickTimeMs();
       isRaining = isRaining || rainingSomewhere;
        if (currentTick >= tickDelay) {
            if (Config.isAdaptive) {
                if (isRaining) {
                    if (tickTime <= Config.targetTps) {
                        maxRainAmount = Math.clamp(maxRainAmount + 1, 0, Config.maxWaterAmount);
                    } else maxRainAmount = Math.clamp(maxRainAmount - 1, 0, Config.maxWaterAmount);
                } else maxRainAmount = Math.clamp(maxRainAmount - 3, 0, Config.maxWaterAmount);
            }   else maxRainAmount = Config.maxWaterAmount;
        currentTick=0;
        isRaining = false;
        } else currentTick++;
    }
}