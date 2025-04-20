package com.stockmarketmod.block;

import com.mojang.logging.LogUtils;
import com.stockmarketmod.screen.NasdaqTerminalScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import org.slf4j.Logger;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class NasdaqTerminalBlock extends Block {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    public NasdaqTerminalBlock() {
        super(Properties.of()
            .mapColor(MapColor.METAL)
            .strength(3.0f, 6.0f)
            .requiresCorrectToolForDrops());
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, 
            InteractionHand hand, BlockHitResult hit) {
        LOGGER.info("Nasdaq Terminal used by player: {}", player.getName().getString());
        
        if (level.isClientSide) {
            Minecraft.getInstance().setScreen(new NasdaqTerminalScreen());
            return InteractionResult.SUCCESS;
        }
        
        return InteractionResult.CONSUME;
    }
} 