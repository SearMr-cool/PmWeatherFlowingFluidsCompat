package com.searmr.pmweatherflowingfluidscompat;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlockTypes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoubleBlockCombiner;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec2;

public class Utils {
    public static boolean withinPlayerRadius(ServerLevel level, BlockPos centerPos, float radius) {
        boolean validChunk = false;
        Vec2 vec2CenterPos = new Vec2(centerPos.getX(),centerPos.getZ());
        for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
            if (level.dimension() == p.serverLevel().dimension()) {
                Vec2 compareVec = new Vec2((float) p.position().x, (float) p.position().z);
                float test = vec2CenterPos.distanceToSqr(compareVec);
                if (test < radius) {
                    validChunk = true;
                    break;
                }
            }
        }
        return validChunk;
    }
}
