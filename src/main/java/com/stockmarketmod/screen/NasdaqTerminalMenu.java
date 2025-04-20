package com.stockmarketmod.screen;

import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

public class NasdaqTerminalMenu extends AbstractContainerMenu {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(ForgeRegistries.MENU_TYPES, "stockmarketmod");
    public static final RegistryObject<MenuType<NasdaqTerminalMenu>> TYPE = MENU_TYPES.register("nasdaq_terminal", 
        () -> IForgeMenuType.create((windowId, inv, data) -> new NasdaqTerminalMenu(windowId, inv)));
    
    public NasdaqTerminalMenu(int containerId, Inventory inventory) {
        super(TYPE.get(), containerId);
        LOGGER.info("Initializing Nasdaq Terminal menu with ID: {}", containerId);
        // We don't add any inventory slots since this is a trading terminal
        LOGGER.info("Nasdaq Terminal menu initialized with no inventory slots");
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
} 