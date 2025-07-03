package com.searmr.pmweatherflowingfluidscompat;


import dev.protomanly.pmweather.PMWeather;
import dev.protomanly.pmweather.config.ServerConfig;
import dev.protomanly.pmweather.event.GameBusEvents;
import dev.protomanly.pmweather.weather.Storm;
import dev.protomanly.pmweather.weather.ThermodynamicEngine;
import dev.protomanly.pmweather.weather.WeatherHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
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
import traben.flowing_fluids.FlowingFluids;

import java.util.*;

import static dev.protomanly.pmweather.weather.ThermodynamicEngine.getPrecipitationType;


@EventBusSubscriber(modid = PmWeatherFlowingFluidsCompat.MODID, bus=EventBusSubscriber.Bus.GAME)
public class EventHandlerServer {
    @SubscribeEvent
public static void ServerTick(ServerTickEvent.Post event ) {
        FlowingFluidsCompat.OnTick();
    if (Config.rainFillsBlocks && !FlowingFluids.config.rainFillsWaterHigherV2) {
        FlowingFluids.config.rainFillsWaterHigherV2=true;
    }
    else if (!Config.rainFillsBlocks && FlowingFluids.config.rainFillsWaterHigherV2) {
        FlowingFluids.config.rainFillsWaterHigherV2=false;
    }
}



}


