package com.searmr.pmweatherflowingfluidscompat.mixin;

//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//


import com.searmr.pmweatherflowingfluidscompat.Config;
import dev.protomanly.pmweather.event.GameBusEvents;
import dev.protomanly.pmweather.weather.WeatherHandler;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.shorts.Short2BooleanMap;
import it.unimi.dsi.fastutil.shorts.Short2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Plane;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import traben.flowing_fluids.FFFluidUtils;
import traben.flowing_fluids.FlowingFluids;
import traben.flowing_fluids.config.FFConfig.LiquidHeight;

@Mixin({FlowingFluid.class})
public abstract class MixinFlowingFluid extends Fluid {
    @Unique
    private static short ffCacheKey(BlockPos sourcePos, BlockPos spreadPos) {
        int i = spreadPos.getX() - sourcePos.getX();
        int j = spreadPos.getZ() - sourcePos.getZ();
        return (short)((i + 128 & 255) << 8 | j + 128 & 255);
    }

    @Unique
    private static boolean ff$handleWaterLoggedFlowAndReturnIfHandled(Level level, BlockPos posFrom, FluidState fluidState, int amount, BlockState thisState, BlockPos posTo, int destFluidAmount, boolean flowingDown) {
        boolean fromIsWaterloggable = thisState.getBlock() instanceof LiquidBlockContainer && thisState.getBlock() instanceof BucketPickup;
        if (fromIsWaterloggable) {
            if (flowingDown) {
                if (FlowingFluids.config.waterLogFlowMode.blocksFlowOutDown()) {
                    return true;
                }
            } else if (FlowingFluids.config.waterLogFlowMode.blocksFlowOutSides()) {
                return true;
            }
        }

        Block blockTo = level.getBlockState(posTo).getBlock();
        boolean toIsWaterloggable = blockTo instanceof LiquidBlockContainer && blockTo instanceof BucketPickup;
        if (toIsWaterloggable && FlowingFluids.config.waterLogFlowMode.blocksFlowIn(flowingDown)) {
            return true;
        } else if (!fromIsWaterloggable && !toIsWaterloggable) {
            return false;
        } else {
            int totalAmount = destFluidAmount + amount;
            if (totalAmount < 8) {
                return true;
            } else {
                if (toIsWaterloggable && fromIsWaterloggable) {
                    FFFluidUtils.setFluidStateAtPosToNewAmount(level, posFrom, fluidState.getType(), 0);
                    FFFluidUtils.setFluidStateAtPosToNewAmount(level, posTo, fluidState.getType(), 8);
                } else if (toIsWaterloggable) {
                    FFFluidUtils.setFluidStateAtPosToNewAmount(level, posFrom, fluidState.getType(), totalAmount - 8);
                    FFFluidUtils.setFluidStateAtPosToNewAmount(level, posTo, fluidState.getType(), 8);
                } else {
                    if (destFluidAmount > 0) {
                        return true;
                    }

                    FFFluidUtils.setFluidStateAtPosToNewAmount(level, posFrom, fluidState.getType(), 0);
                    FFFluidUtils.setFluidStateAtPosToNewAmount(level, posTo, fluidState.getType(), 8);
                }

                return true;
            }
        }
    }

    protected boolean isRandomlyTicking() {
        return FlowingFluids.config.enableMod && FlowingFluids.config.isFluidAllowed(this) ? true : super.isRandomlyTicking();
    }

    protected void randomTick(Level level, BlockPos pos, FluidState state, RandomSource random) {
        super.randomTick(level, pos, state, random);
        if (FlowingFluids.config.enableMod && FlowingFluids.config.randomTickLevelingDistance > 0 && level.getChunkAt(pos).getFluidTicks().count() < 16 && FlowingFluids.config.isFluidAllowed(this) && !level.getFluidState(pos.above()).getType().isSame(this)) {
            int amount = state.getAmount();
            if (amount <= this.getDropOff(level)) {
                return;
            }

            int amountLess = amount - 1;
            Direction randomDirection = (Direction)FFFluidUtils.getCardinalsShuffle(level.getRandom()).get(0);
            BiConsumer<BlockPos.MutableBlockPos, BlockPos.MutableBlockPos> move;
            if (level.getRandom().nextBoolean()) {
                move = (mbp, up) -> {
                    mbp.move(randomDirection);
                    up.move(randomDirection);
                };
            } else {
                Direction offStep = level.getRandom().nextBoolean() ? randomDirection.getClockWise() : randomDirection.getCounterClockWise();
                RandomSource rand = level.getRandom();
                move = (mbp, up) -> {
                    Direction dir = rand.nextBoolean() ? randomDirection : offStep;
                    mbp.move(dir);
                    up.move(dir);
                };
            }

            BlockPos.MutableBlockPos movingDir = pos.mutable();
            BlockPos.MutableBlockPos movingDirAbove = pos.above().mutable();

            for(int i = 0; i < FlowingFluids.config.randomTickLevelingDistance; ++i) {
                move.accept(movingDir, movingDirAbove);
                BlockState stateDir = level.getBlockState(movingDir);
                if (!(stateDir.getBlock() instanceof LiquidBlock)) {
                    return;
                }

                FluidState fluidStateDir = stateDir.getFluidState();
                if (!fluidStateDir.getType().isSame(this)) {
                    return;
                }

                if (level.getFluidState(movingDirAbove).getType().isSame(this)) {
                    return;
                }

                int amountDir = fluidStateDir.getAmount();
                if (amountDir > amount) {
                    return;
                }

                if (amountDir < amountLess) {
                    FFFluidUtils.setFluidStateAtPosToNewAmount(level, movingDir, this, amountDir + 1);
                    FFFluidUtils.setFluidStateAtPosToNewAmount(level, pos, this, amountLess);
                    return;
                }
            }
        }

    }

    @Shadow
    protected abstract int getDropOff(LevelReader var1);

    @Shadow
    protected abstract void spreadTo(LevelAccessor var1, BlockPos var2, BlockState var3, Direction var4, FluidState var5);

    @Shadow
    protected abstract int getSlopeFindDistance(LevelReader var1);

    @Shadow
    public abstract int getAmount(FluidState var1);

    @Inject(
            method = {"getOwnHeight(Lnet/minecraft/world/level/material/FluidState;)F"},
            at = {@At("HEAD")},
            cancellable = true
    )
    private void ff$differentRenderHeight(FluidState state, CallbackInfoReturnable<Float> cir) {
        if (FlowingFluids.config.enableMod && FlowingFluids.config.isFluidAllowed(state) && FlowingFluids.config.fullLiquidHeight != LiquidHeight.REGULAR) {
            Float var10001;
            switch (FlowingFluids.config.fullLiquidHeight) {
                case BLOCK -> var10001 = (float)state.getAmount() / 8.0F;
                case SLAB -> var10001 = (float)state.getAmount() / 16.0F;
                case CARPET -> var10001 = 0.0625F;
                case REGULAR_LOWER_BOUND -> var10001 = ((float)state.getAmount() - 0.9F) * 0.8888889F / 7.0F;
                case BLOCK_LOWER_BOUND -> var10001 = ((float)state.getAmount() - 0.9F) / 7.0F;
                default -> var10001 = (float)state.getAmount() / 9.0F;
            }

            cir.setReturnValue(var10001);
        }

    }

    @Inject(
            method = {"tick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/material/FluidState;)V"},
            at = {@At("HEAD")},
            cancellable = true
    )
    private void ff$tickMixin(Level level, BlockPos blockPos, FluidState fluidState, CallbackInfo ci) {
        if (FlowingFluids.config.enableMod && FlowingFluids.config.isFluidAllowed(fluidState)) {
            ci.cancel();
            if (FlowingFluids.config.dontTickAtLocation(blockPos, level)) {
                level.scheduleTick(blockPos, this, 200 + level.random.nextInt(200));
            } else if (System.currentTimeMillis() >= FlowingFluids.debug_killFluidUpdatesUntilTime) {
                var managers = GameBusEvents.MANAGERS;
                WeatherHandler handle = (WeatherHandler) managers.get(level.dimension());
                float rainLevel = handle.getPrecipitation(blockPos.above().getBottomCenter());
                boolean isRaining = rainLevel > 0;
                FlowingFluids.isManeuveringFluids = true;
                boolean withinInfBiomeHeights = FlowingFluids.config.fastBiomeRefillAtSeaLevelOnly ? level.getSeaLevel() == blockPos.getY() || level.getSeaLevel() - 1 == blockPos.getY() : level.getSeaLevel() == blockPos.getY() && blockPos.getY() > 0;
                boolean isWaterAndInfiniteBiome = fluidState.is(FluidTags.WATER) && withinInfBiomeHeights && FFFluidUtils.matchInfiniteBiomes(level.getBiome(blockPos)) && level.getBrightness(LightLayer.SKY, blockPos) > 0;
                boolean dontConsumeWater = isWaterAndInfiniteBiome && level.getSeaLevel() != blockPos.getY() && level.getRandom().nextFloat() < FlowingFluids.config.infiniteWaterBiomeNonConsumeChance;
                BlockState thisState = level.getBlockState(blockPos);
                boolean var23 = false;

                label384: {
                    label410: {
                        try {
                            var23 = true;
                            BlockPos posDown = blockPos.below();
                            int remainingAmount = this.flowing_fluids$checkAndFlowDown(level, blockPos, fluidState, thisState, posDown, level.getBlockState(posDown), fluidState.getAmount());
                            if (remainingAmount <= 0) {
                                var23 = false;
                                break label384;
                            }

                            if (fluidState.getAmount() == 8 && thisState.liquid()) {
                                BlockPos abovePos = blockPos.above();
                                BlockState above = level.getBlockState(abovePos);
                                if (above.liquid()) {
                                    FluidState aboveF = above.getFluidState();
                                    int aboveAmount = aboveF.getAmount();
                                    if (aboveAmount > 0) {
                                        FlowingFluid flow = (FlowingFluid)aboveF.getType();
                                        if (FFFluidUtils.canFluidFlowFromPosToDirectionFitOverride(flow, level, abovePos, above, Direction.DOWN, blockPos, thisState)) {
                                            Pair<Integer, Runnable> remainder = FFFluidUtils.placeConnectedFluidAmountAndPlaceAction(level, blockPos, aboveAmount, flow, 40, false, !FlowingFluids.pistonTick);
                                            if ((Integer)remainder.first() < aboveAmount) {
                                                ((Runnable)remainder.second()).run();
                                                if (!dontConsumeWater) {
                                                    FFFluidUtils.setFluidStateAtPosToNewAmount(level, abovePos, flow, (Integer)remainder.first());
                                                    var23 = false;
                                                } else {
                                                    var23 = false;
                                                }
                                                break label410;
                                            }
                                        }
                                    }
                                }
                            }

                            if (remainingAmount > this.getDropOff(level)) {
                                this.ff$flowToSides(level, blockPos, fluidState, remainingAmount, thisState);
                                var23 = false;
                            } else if (FlowingFluids.config.flowToEdges) {
                                Direction dir = this.flowing_fluids$getLowestSpreadableLookingFor4BlockDrops(level, blockPos, fluidState, 1, true);
                                if (dir != null) {
                                    BlockPos pos = blockPos.relative(dir);
                                    this.flowing_fluids$setOrRemoveWaterAmountAt(level, blockPos, 0, thisState, dir);
                                    this.flowing_fluids$spreadTo2(level, pos, level.getBlockState(pos), dir, remainingAmount);
                                    var23 = false;
                                } else {
                                    var23 = false;
                                }
                            } else {
                                var23 = false;
                            }
                        } finally {
                            if (var23) {
                                if (isWaterAndInfiniteBiome) {
                                    if (level.getSeaLevel() == blockPos.getY() && (Config.waterDrainsRain || !isRaining)) {
                                        if (level.getRandom().nextFloat() < FlowingFluids.config.infiniteWaterBiomeDrainSurfaceChance) {
                                            int amount = level.getFluidState(blockPos).getAmount();
                                            if (amount > 0) {
                                                FluidState below = level.getFluidState(blockPos.below());
                                                if (below.getAmount() == 8 && below.is(FluidTags.WATER)) {
                                                    level.setBlockAndUpdate(blockPos, FFFluidUtils.getBlockForFluidByAmount(this, amount - 1));
                                                }
                                            }
                                        }
                                    } else if (dontConsumeWater) {
                                        level.setBlock(blockPos, thisState, 0);
                                    }
                                }

                                FlowingFluids.isManeuveringFluids = false;
                                FlowingFluids.pistonTick = false;
                            }
                        }

                        if (isWaterAndInfiniteBiome) {
                            if (level.getSeaLevel() == blockPos.getY()) {
                                if (level.getRandom().nextFloat() < FlowingFluids.config.infiniteWaterBiomeDrainSurfaceChance && (Config.waterDrainsRain || !isRaining)) {
                                    int amount = level.getFluidState(blockPos).getAmount();
                                    if (amount > 0) {
                                        FluidState below = level.getFluidState(blockPos.below());
                                        if (below.getAmount() == 8 && below.is(FluidTags.WATER)) {
                                            level.setBlockAndUpdate(blockPos, FFFluidUtils.getBlockForFluidByAmount(this, amount - 1));
                                        }
                                    }
                                }
                            } else if (dontConsumeWater) {
                                level.setBlock(blockPos, thisState, 0);
                            }
                        }

                        FlowingFluids.isManeuveringFluids = false;
                        FlowingFluids.pistonTick = false;
                        return;
                    }

                    if (isWaterAndInfiniteBiome) {
                        if (level.getSeaLevel() == blockPos.getY()) {
                            if (level.getRandom().nextFloat() < FlowingFluids.config.infiniteWaterBiomeDrainSurfaceChance && (Config.waterDrainsRain || !isRaining)) {
                                int amount = level.getFluidState(blockPos).getAmount();
                                if (amount > 0) {
                                    FluidState below = level.getFluidState(blockPos.below());
                                    if (below.getAmount() == 8 && below.is(FluidTags.WATER)) {
                                        level.setBlockAndUpdate(blockPos, FFFluidUtils.getBlockForFluidByAmount(this, amount - 1));
                                    }
                                }
                            }
                        } else if (dontConsumeWater) {
                            level.setBlock(blockPos, thisState, 0);
                        }
                    }

                    FlowingFluids.isManeuveringFluids = false;
                    FlowingFluids.pistonTick = false;
                    return;
                }

                if (isWaterAndInfiniteBiome) {
                    if (level.getSeaLevel() == blockPos.getY()) {
                        if (level.getRandom().nextFloat() < FlowingFluids.config.infiniteWaterBiomeDrainSurfaceChance && (Config.waterDrainsRain || !isRaining)) {
                            int amount = level.getFluidState(blockPos).getAmount();
                            if (amount > 0) {
                                FluidState below = level.getFluidState(blockPos.below());
                                if (below.getAmount() == 8 && below.is(FluidTags.WATER)) {
                                    level.setBlockAndUpdate(blockPos, FFFluidUtils.getBlockForFluidByAmount(this, amount - 1));
                                }
                            }
                        }
                    } else if (dontConsumeWater) {
                        level.setBlock(blockPos, thisState, 0);
                    }
                }

                FlowingFluids.isManeuveringFluids = false;
                FlowingFluids.pistonTick = false;
            }
        }
    }

    @Unique
    private void ff$flowToSides(Level level, BlockPos blockPos, FluidState fluidState, int amount, BlockState thisState) {
        Direction dir = this.flowing_fluids$getLowestSpreadableLookingFor4BlockDrops(level, blockPos, fluidState, amount, false);
        if (dir != null) {
            BlockPos posDir = blockPos.relative(dir);
            int destFluidAmount = level.getFluidState(posDir).getAmount();
            if (!ff$handleWaterLoggedFlowAndReturnIfHandled(level, blockPos, fluidState, amount, thisState, posDir, destFluidAmount, false)) {
                int difference = amount - destFluidAmount;
                int averageLevel = destFluidAmount + difference / 2;
                boolean hasRemainder = difference % 2 != 0;
                int toAmount;
                if (hasRemainder) {
                    toAmount = averageLevel + 1;
                } else {
                    toAmount = averageLevel;
                }

                FFFluidUtils.setFluidStateAtPosToNewAmount(level, blockPos, fluidState.getType(), averageLevel);
                FFFluidUtils.setFluidStateAtPosToNewAmount(level, posDir, fluidState.getType(), toAmount);
            }
        }
    }

    @Unique
    private int flowing_fluids$checkAndFlowDown(Level level, BlockPos blockPos, FluidState fluidState, BlockState thisState, BlockPos posDown, BlockState stateDown, int amount) {
        FluidState downFState = level.getFluidState(posDown);
        if (this.flowing_fluids$canSpreadTo(fluidState.getType(), fluidState.getAmount(), level, blockPos, thisState, Direction.DOWN, posDown, stateDown, downFState)) {
            if (!downFState.isEmpty() && !downFState.getType().isSame(fluidState.getType())) {
                this.flowing_fluids$setOrRemoveWaterAmountAt(level, blockPos, amount - 1, thisState, Direction.DOWN);
                this.flowing_fluids$spreadTo2(level, posDown, stateDown, Direction.DOWN, 1);
                return amount - 1;
            }

            if (FlowingFluids.config.easyPistonPump && FlowingFluids.config.enablePistonPushing) {
                BlockState block = level.getBlockState(posDown.below());
                if (block.is(Blocks.MOVING_PISTON) && block.getValue(DirectionalBlock.FACING) == Direction.UP) {
                    level.scheduleTick(blockPos, this, 10);
                    FlowingFluids.pistonTick = true;
                    return amount;
                }
            }

            int fluidDownAmount = downFState.getAmount();
            if (ff$handleWaterLoggedFlowAndReturnIfHandled(level, blockPos, fluidState, amount, thisState, posDown, fluidDownAmount, true)) {
                return level.getFluidState(blockPos).getAmount();
            }

            int amountDestCanAccept = Math.min(8 - fluidDownAmount, amount);
            if (amountDestCanAccept > 0) {
                int destNewAmount = fluidDownAmount + amountDestCanAccept;
                int sourceNewAmount = amount - amountDestCanAccept;
                this.flowing_fluids$setOrRemoveWaterAmountAt(level, blockPos, sourceNewAmount, thisState, Direction.DOWN);
                this.flowing_fluids$spreadTo2(level, posDown, stateDown, Direction.DOWN, destNewAmount);
                return sourceNewAmount;
            }
        }

        return amount;
    }

    @Unique
    private void flowing_fluids$setOrRemoveWaterAmountAt(Level level, BlockPos blockPos, int amount, BlockState thisState, Direction direction) {
        if (amount > 0) {
            this.flowing_fluids$spreadTo2(level, blockPos, thisState, direction, amount);
        } else {
            FFFluidUtils.removeAllFluidAtPos(level, blockPos, this);
        }

    }

    @Inject(
            method = {"getNewLiquid(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/world/level/material/FluidState;"},
            at = {@At("HEAD")},
            cancellable = true
    )
    private void flowing_fluids$validateLiquidMixin(Level level, BlockPos blockPos, BlockState blockState, CallbackInfoReturnable<FluidState> cir) {
        if (FlowingFluids.config.enableMod && FlowingFluids.config.isFluidAllowed(this)) {
            FluidState state = level.getFluidState(blockPos);
            cir.setReturnValue(FFFluidUtils.getStateForFluidByAmount(state.getType(), state.getAmount()));
        }

    }

    @Unique
    private @Nullable Direction flowing_fluids$getLowestSpreadableLookingFor4BlockDrops(Level level, BlockPos blockPos, FluidState fluidState, int amount, boolean requiresSlope) {
        Short2ObjectMap<com.mojang.datafixers.util.Pair<BlockState, FluidState>> statesAtPos = new Short2ObjectOpenHashMap();
        AtomicBoolean anyFlowableNeighbours2LevelsLowerOrMore = new AtomicBoolean(requiresSlope);
        List<Direction> directionsCanSpreadToSortedByAmount = FFFluidUtils.getCardinalsShuffle(level.random).stream().sorted(Comparator.comparingInt((dir1) -> level.getFluidState(blockPos.relative(dir1)).getAmount())).filter((dir) -> {
            BlockPos posDir = blockPos.relative(dir);
            short key = ffCacheKey(blockPos, posDir);
            com.mojang.datafixers.util.Pair<BlockState, FluidState> statesDir = this.flowing_fluids$getSetPosCache(key, level, statesAtPos, posDir);
            BlockState stateDir = (BlockState)statesDir.getFirst();
            FluidState fluidStateDir = (FluidState)statesDir.getSecond();
            int amountDir = fluidStateDir.getAmount();
            boolean canFlow = this.flowing_fluids$canSpreadToOptionallySameOrEmpty(fluidState.getType(), amount, level, blockPos, level.getBlockState(blockPos), dir, posDir, stateDir, fluidStateDir, requiresSlope);
            if (canFlow && !anyFlowableNeighbours2LevelsLowerOrMore.get()) {
                anyFlowableNeighbours2LevelsLowerOrMore.set(amountDir < amount - 1);
            }

            return canFlow;
        }).toList();
        if (directionsCanSpreadToSortedByAmount.isEmpty()) {
            return null;
        } else {
            boolean requiresSlopeWithOverride = requiresSlope || !anyFlowableNeighbours2LevelsLowerOrMore.get();
            Direction spreadDirection = this.flowing_fluids$getValidDirectionFromDeepSpreadSearch(level, blockPos, fluidState, amount, requiresSlopeWithOverride, directionsCanSpreadToSortedByAmount, statesAtPos);
            return spreadDirection == null && !requiresSlopeWithOverride ? (Direction)directionsCanSpreadToSortedByAmount.get(0) : spreadDirection;
        }
    }

    @Unique
    private @Nullable Direction flowing_fluids$getValidDirectionFromDeepSpreadSearch(Level level, BlockPos blockPos, FluidState fluidState, int amount, boolean requiresSlope, List<Direction> directionsCanSpreadToSortedByAmount, Short2ObjectMap<com.mojang.datafixers.util.Pair<BlockState, FluidState>> statesAtPos) {
        int slopeFindDistance = this.getSlopeFindDistance(level);
        if (slopeFindDistance < 1) {
            return null;
        } else {
            Short2BooleanMap posCanFlowDown = new Short2BooleanOpenHashMap();
            posCanFlowDown.put(ffCacheKey(blockPos, blockPos), false);
            return (Direction)directionsCanSpreadToSortedByAmount.stream().map((dir) -> {
                BlockPos posDir = blockPos.relative(dir);
                short key = ffCacheKey(blockPos, posDir);
                return level.getFluidState(posDir).getAmount() >= amount - 1 && !this.flowing_fluids$getSetFlowDownCache(key, level, posCanFlowDown, posDir, fluidState.getType(), requiresSlope) ? com.mojang.datafixers.util.Pair.of(dir, this.flowing_fluids$getSlopeDistance(level, blockPos, 1, dir.getOpposite(), fluidState.getType(), amount + 1, posDir, statesAtPos, posCanFlowDown, requiresSlope, slopeFindDistance)) : com.mojang.datafixers.util.Pair.of(dir, 0);
            }).filter((pair) -> !requiresSlope || (Integer)pair.getSecond() <= slopeFindDistance).min(Comparator.comparingInt(com.mojang.datafixers.util.Pair::getSecond)).map(com.mojang.datafixers.util.Pair::getFirst).orElse((Direction) null);
        }
    }

    @Unique
    protected int flowing_fluids$getSlopeDistance(LevelReader level, BlockPos sourcePosForKey, int distance, Direction fromDir, Fluid sourceFluid, int sourceAmount, BlockPos newPos, Short2ObjectMap<com.mojang.datafixers.util.Pair<BlockState, FluidState>> statesAtPos, Short2BooleanMap posCanFlowDown, boolean forceSlopeDownSameOrEmpty, int slopeFindDistance) {
        int smallest = 1000;
        int searchDistance = distance + 1;

        for(Direction searchDir : Plane.HORIZONTAL) {
            if (searchDir != fromDir) {
                BlockPos searchPos = newPos.relative(searchDir);
                short searchKey = ffCacheKey(sourcePosForKey, searchPos);
                com.mojang.datafixers.util.Pair<BlockState, FluidState> searchStates = this.flowing_fluids$getSetPosCache(searchKey, level, statesAtPos, searchPos);
                if (this.flowing_fluids$canSpreadToOptionallySameOrEmpty(sourceFluid, sourceAmount, level, newPos, level.getBlockState(newPos), searchDir, searchPos, (BlockState)searchStates.getFirst(), (FluidState)searchStates.getSecond(), forceSlopeDownSameOrEmpty)) {
                    if (((FluidState)searchStates.getSecond()).getAmount() < sourceAmount - 2 || this.flowing_fluids$getSetFlowDownCache(searchKey, level, posCanFlowDown, searchPos, sourceFluid, forceSlopeDownSameOrEmpty)) {
                        return searchDistance;
                    }

                    if (searchDistance < slopeFindDistance) {
                        int next = this.flowing_fluids$getSlopeDistance(level, sourcePosForKey, searchDistance, searchDir.getOpposite(), sourceFluid, sourceAmount, searchPos, statesAtPos, posCanFlowDown, forceSlopeDownSameOrEmpty, slopeFindDistance);
                        if (next < smallest) {
                            smallest = next;
                        }
                    }
                }
            }
        }

        return smallest;
    }

    @Unique
    private com.mojang.datafixers.util.Pair<BlockState, FluidState> flowing_fluids$getSetPosCache(short key, LevelReader level, Short2ObjectMap<com.mojang.datafixers.util.Pair<BlockState, FluidState>> statesAtPos, BlockPos pos) {
        return (com.mojang.datafixers.util.Pair)statesAtPos.computeIfAbsent(key, (sx) -> {
            BlockState blockState = level.getBlockState(pos);
            return com.mojang.datafixers.util.Pair.of(blockState, blockState.getFluidState());
        });
    }

    @Unique
    private boolean flowing_fluids$getSetFlowDownCache(short key, LevelReader level, Short2BooleanMap boolAtPos, BlockPos pos, Fluid sourceFluid, boolean forceSlopeDownSameOrEmpty) {
        return boolAtPos.computeIfAbsent(key, (sx) -> {
            BlockPos posDown = pos.below();
            return this.flowing_fluids$canSpreadToOptionallySameOrEmpty(sourceFluid, 8, level, pos, level.getBlockState(pos), Direction.DOWN, posDown, level.getBlockState(posDown), level.getFluidState(posDown), forceSlopeDownSameOrEmpty);
        });
    }

    @Unique
    protected void flowing_fluids$spreadTo2(LevelAccessor levelAccessor, BlockPos blockPos, BlockState blockState, Direction direction, int amount) {
        this.spreadTo(levelAccessor, blockPos, blockState, direction, FFFluidUtils.getStateForFluidByAmount(this, amount));
    }

    @Unique
    private boolean flowing_fluids$canSpreadToOptionallySameOrEmpty(Fluid sourceFluid, int sourceAmount, BlockGetter blockGetter, BlockPos blockPos, BlockState blockState, Direction direction, BlockPos blockPos2, BlockState blockState2, FluidState fluidState2, boolean enforceSameFluidOrEmpty) {
        return enforceSameFluidOrEmpty && !fluidState2.isEmpty() && !fluidState2.getType().isSame(sourceFluid) ? false : this.flowing_fluids$canSpreadTo(sourceFluid, sourceAmount, blockGetter, blockPos, blockState, direction, blockPos2, blockState2, fluidState2);
    }

    @Unique
    private boolean flowing_fluids$canSpreadTo(Fluid sourceFluid, int sourceAmount, BlockGetter blockGetter, BlockPos blockPos, BlockState blockState, Direction direction, BlockPos blockPos2, BlockState blockState2, FluidState fluidState2) {
        return FFFluidUtils.canFluidFlowFromPosToDirection((FlowingFluid)sourceFluid, sourceAmount, blockGetter, blockPos, blockState, direction, blockPos2, blockState2, fluidState2);
    }
}
