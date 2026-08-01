package com.searmr.pmweatherflowingfluidscompat;

import dev.protomanly.pmweather.addons.AddonHelper;
import dev.protomanly.pmweather.addons.AddonInfo;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import traben.flowing_fluids.api.FlowingFluidsAPI;
import traben.flowing_fluids.api.FlowingFluidsApiImpl;

import java.util.List;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(value = PmWeatherFlowingFluidsCompatServer.MODID)
public class PmWeatherFlowingFluidsCompatServer {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "pmweatherflowingfluidscompat";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();
    public static  final FlowingFluidsAPI FLOWINGFLUIDSAPI = FlowingFluidsAPI.getInstance(MODID);;
    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public PmWeatherFlowingFluidsCompatServer(IEventBus modEventBus, ModContainer modContainer) {
        AddonHelper.registerAddon(new AddonInfo(modContainer, List.of("0.16.4","0.17.0", "0.17.1","0.17.2")));
        modContainer.registerConfig(ModConfig.Type.SERVER, com.searmr.pmweatherflowingfluidscompat.Config.thing);
    }
}
