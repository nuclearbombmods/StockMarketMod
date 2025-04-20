package com.stockmarketmod.item;

import com.stockmarketmod.block.NasdaqTerminalBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

public class NasdaqTerminalItem extends BlockItem {
    public NasdaqTerminalItem(NasdaqTerminalBlock block) {
        super(block, new Item.Properties()
            .rarity(Rarity.UNCOMMON)
            .stacksTo(64));
    }
} 