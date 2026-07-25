package com.searmr.pmweatherflowingfluidscompat;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlockTypes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoubleBlockCombiner;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public class Utils {
    public static BlockPos getTopBlock(Level level, BlockPos blockPos) {

        if (level.getMaxBuildHeight() > blockPos.getY()) {
            BlockState blockState = level.getBlockState(blockPos);
            if (blockState.getFluidState().is(Fluids.WATER)) {
                return getTopBlock(level,blockPos.above());
            }
            else return blockPos;
        }
       else return blockPos;
    }
}
