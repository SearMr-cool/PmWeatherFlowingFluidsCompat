package com.searmr.pmweatherflowingfluidscompat;

import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.jetbrains.annotations.Range;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Neo's config APIs
@EventBusSubscriber(
        modid = PmWeatherFlowingFluidsCompat.MODID

)
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();



    public static boolean hailDestroysBlocks ;
    private static final ModConfigSpec.IntValue max_water_amount;
    public static int maxWaterAmount ;
    private static final ModConfigSpec.BooleanValue water_drains_rain;
    public static boolean waterDrainsRain ;
    private static final ModConfigSpec.BooleanValue is_adaptive;
    public static boolean isAdaptive ;
    private static final ModConfigSpec.BooleanValue rain_anywhere;
    public static boolean rainAnywhere ;
    private static final ModConfigSpec.IntValue target_tps;
    public static int targetTps ;
    private static final ModConfigSpec.IntValue max_drain_chance;
    public static int maxDrainChance;
    public static boolean rainFillsBlocks ;
    private static final ModConfigSpec.BooleanValue rain_fills_blocks;
    public static boolean drainingDisabledWhileRaining ;
    private static final ModConfigSpec.BooleanValue draining_disabled_while_raining;
    public static final ModConfigSpec thing;



    @SubscribeEvent
    private static void onLoad(ModConfigEvent event) {
        if (!(event instanceof ModConfigEvent.Unloading) && event.getConfig().getSpec() == thing){

            maxWaterAmount = (Integer) max_water_amount.get();
            waterDrainsRain = (boolean) water_drains_rain.get();
            isAdaptive = (boolean) is_adaptive.get();
            rainAnywhere = (boolean) rain_anywhere.get();
            targetTps = (int) target_tps.get();
            maxDrainChance = (int) max_drain_chance.get();
            rainFillsBlocks = (boolean) rain_fills_blocks.get();
            drainingDisabledWhileRaining = (boolean) draining_disabled_while_raining.get();
        }


    }



    static  {

        max_water_amount = BUILDER.comment("The max amount of water that can be added to water during rain (This is the biggest factor when it comes to how fast flooding happens").defineInRange("maxwateramount",4,1,100);
        water_drains_rain = BUILDER.comment("Should rain be in areas that are raining? If enabled it will make flooding very difficult plus lag may occur so it is recommended to leave this off").define("waterdrainsrain",false);
        is_adaptive = BUILDER.comment("If enabled the amount of rain will dynamically adjust to try and stop overloading the users system (do note the highest this will go is still limited by the user set value").define("isadaptive",true);
        target_tps = BUILDER.comment("If adaptive is enabled each tick will aim to be this many milliseconds long").defineInRange("targettps",30,1,50);
        rain_anywhere = BUILDER.comment("By default Flowing Fluids does not allow rain to build up in oceans/rivers (and some other biomes), this setting if enabled will bypass this default behavior making flooding easier but it may cause more lag in the long run").define("rainanywhere",true);
        max_drain_chance = BUILDER.comment("The maximum drain chance for water to be drained (This is required as some values here are changed dynamically (0-100%)").defineInRange("maxdrainchance",6,0,100);
        rain_fills_blocks = BUILDER.comment("If rain places blocks above water (This must be turned on for flooding to properly occur)").define("rainfillsblocks",true);
        draining_disabled_while_raining = BUILDER.comment("This option will completely disable draining server wide if it is raining anywhere which can solve a lag spike when it stops raining (which can impact gameplay depending on the situation hence why this is disabled by default)").define("raindisabledwhileraining",false);



        thing = BUILDER.build();
    }
}

