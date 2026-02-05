package com.searmr.pmweatherflowingfluidscompat.mixin;
//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//



import com.searmr.pmweatherflowingfluidscompat.Config;
import com.searmr.pmweatherflowingfluidscompat.FlowingFluidsCompat;
import com.searmr.pmweatherflowingfluidscompat.PmWeatherFlowingFluidsCompat;
import dev.protomanly.pmweather.event.GameBusEvents;
import dev.protomanly.pmweather.weather.WeatherHandler;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.WaterFluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import traben.flowing_fluids.FFFluidUtils;
import traben.flowing_fluids.FlowingFluids;


@Mixin({WaterFluid.class})
public abstract class MixinWaterFluid extends FlowingFluid {
    @Unique
    boolean isWithinInfBiomeHeights = false;
    @Unique
    boolean isInfBiome = false;
    @Unique
    boolean hasSkyLight = false;

    @Shadow
    public abstract int getDropOff(LevelReader var1);

    @Shadow
    public abstract boolean isSame(Fluid var1);

    protected void randomTick(Level level, BlockPos blockPos, FluidState fluidState, RandomSource randomSource) {
        super.randomTick(level, blockPos, fluidState, randomSource);

        if (!level.isClientSide() && !fluidState.isEmpty() && FlowingFluids.config.enableMod && FlowingFluids.config.isFluidAllowed(fluidState)) {
            if (!FlowingFluids.config.dontTickAtLocation(blockPos, level)) {

                var managers = GameBusEvents.MANAGERS;
                WeatherHandler handle = (WeatherHandler) managers.get(level.dimension());
                float rainLevel = handle.getPrecipitation(blockPos.getCenter());
                boolean isRaining = rainLevel > Config.minRainLevelPuddle;
                rainLevel -= (float) Config.minRainLevelPuddle;
                this.isWithinInfBiomeHeights = FlowingFluids.config.fastBiomeRefillAtSeaLevelOnly ? level.getSeaLevel() == blockPos.getY() || level.getSeaLevel() - 1 == blockPos.getY() : level.getSeaLevel() == blockPos.getY() && blockPos.getY() > 0;
                this.hasSkyLight = level.getBrightness(LightLayer.SKY, blockPos) > 0;
                this.isInfBiome = FFFluidUtils.matchInfiniteBiomes(level.getBiome(blockPos));
                int amount = fluidState.getAmount();
                if (amount < 8) {
                    if (this.ff$tryBiomeFillOrDrain(level, blockPos, amount, level.random.nextFloat(),isRaining)) {
                        if (FlowingFluids.config.printRandomTicks) {
                            String var8 = String.valueOf(blockPos);
                            FlowingFluids.info("--- Water was filled by biome at " + var8 + ". Chance: " + FlowingFluids.config.oceanRiverSwampRefillChance);
                        }

                        return;
                    }


                    if (this.ff$tryEvaporateNether(level, blockPos, amount, level.random.nextFloat())) {
                        if (FlowingFluids.config.printRandomTicks) {
                            String var6 = String.valueOf(blockPos);
                            FlowingFluids.info("--- Water was evaporated via Nether at " + var6 + ". Chance: " + FlowingFluids.config.evaporationChanceV2);
                        }

                        return;
                    }

                    if (this.ff$tryEvaporate(level, blockPos, amount, level.random.nextFloat(),isRaining) && FlowingFluids.config.printRandomTicks) {
                        String var10000 = String.valueOf(blockPos);
                        FlowingFluids.info("--- Water was evaporated - non Nether at " + var10000 + ". Chance: " + FlowingFluids.config.evaporationChanceV2);
                    }
                }

            }
        }
    }

    @Unique
    private boolean ff$tryRainFill(Level level, BlockPos blockPos, float chance, boolean isRaining,float rainLevel) {
        if (chance < Math.min(FlowingFluids.config.rainRefillChance, FlowingFluids.config.evaporationChanceV2 / 3.0F) && isRaining && level.canSeeSky(blockPos.above()) && (!this.isInfBiome || !this.isWithinInfBiomeHeights || Config.rainAnywhere) && !level.getBiome(blockPos).is(BiomeTags.HAS_VILLAGE_DESERT)) {
            int amount = Math.clamp((int)(FlowingFluidsCompat.maxRainAmount * rainLevel),0, Config.maxWaterAmount);
            if (Config.isAdaptive) FlowingFluidsCompat.tempRainArray.add(true);
            Pair<Integer, Runnable> result = FFFluidUtils.placeConnectedFluidAmountAndPlaceAction(level, blockPos, amount, this, 40, FlowingFluids.config.rainFillsWaterHigherV2, false);
            if ((Integer)result.first() != amount) {

                ((Runnable)result.second()).run();

                return true;
            }
        }
        return false;
    }

    @Unique
    private boolean ff$tryBiomeFillOrDrain(Level level, BlockPos blockPos, int amount, float chance, boolean isRaining) {
        if (!Config.waterDrainsRain && isRaining) return false;
        if (level.getSeaLevel() == blockPos.getY()) {
            if (chance < FlowingFluids.config.infiniteWaterBiomeNonConsumeChance || chance < FlowingFluids.config.oceanRiverSwampRefillChance || isRaining && chance < FlowingFluids.config.rainRefillChance) {
                FluidState below = level.getFluidState(blockPos.below());
                if (below.getAmount() == 8 && below.is(FluidTags.WATER) && this.hasSkyLight && this.isInfBiome) {
                    level.setBlockAndUpdate(blockPos, FFFluidUtils.getBlockForFluidByAmount(this, amount - 2));
                    return true;
                }
            }
        } else if (this.isWithinInfBiomeHeights && amount < 8 && chance < FlowingFluids.config.oceanRiverSwampRefillChance && this.isInfBiome && this.hasSkyLight) {
            level.setBlockAndUpdate(blockPos, FFFluidUtils.getBlockForFluidByAmount(this, amount + 2));
            return true;
        }

        return false;
    }

    @Unique
    private boolean ff$tryEvaporate(Level level, BlockPos blockPos, int amount, float chance,boolean isRaining) {
        if (!Config.waterDrainsRain && isRaining) return false;
        if (chance < FlowingFluids.config.evaporationChanceV2 && amount <= this.getDropOff(level) && level.getFluidState(blockPos.below()).isEmpty()) {
            level.setBlockAndUpdate(blockPos, Blocks.AIR.defaultBlockState());
            return true;
        } else {
            return false;
        }
    }

    @Unique
    private boolean ff$tryEvaporateNether(Level level, BlockPos blockPos, int amount, float chance) {
        if (chance < FlowingFluids.config.evaporationNetherChance && level.getBiome(blockPos).is(BiomeTags.IS_NETHER)) {
            if (amount == 1) {
                level.setBlockAndUpdate(blockPos, Blocks.AIR.defaultBlockState());
            } else {
                level.setBlockAndUpdate(blockPos, FFFluidUtils.getBlockForFluidByAmount(this, amount - 3));
            }

            return true;
        } else {
            return false;
        }
    }

    @Inject(
            method = {"getSlopeFindDistance(Lnet/minecraft/world/level/LevelReader;)I"},
            at = {@At("RETURN")},
            cancellable = true
    )
    private void ff$modifySlopeDistance(LevelReader level, CallbackInfoReturnable<Integer> cir) {
        if (FlowingFluids.config.enableMod && FlowingFluids.config.isFluidAllowed(this)) {
            cir.setReturnValue(Mth.clamp(FlowingFluids.config.waterFlowDistance, 1, 8));
        }

    }

    @Inject(
            method = {"getTickDelay(Lnet/minecraft/world/level/LevelReader;)I"},
            at = {@At("RETURN")},
            cancellable = true
    )
    private void ff$modifyTickDelay(LevelReader level, CallbackInfoReturnable<Integer> cir) {
        if (FlowingFluids.config.enableMod && FlowingFluids.config.isFluidAllowed(this)) {
            cir.setReturnValue(Mth.clamp(FlowingFluids.config.waterTickDelay, 1, 255));
        }

    }
}

