package com.stockmarketmod;

import com.mojang.logging.LogUtils;
import com.stockmarketmod.block.NasdaqTerminalBlock;
import com.stockmarketmod.item.NasdaqTerminalItem;
import com.stockmarketmod.screen.NasdaqTerminalMenu;
import com.stockmarketmod.service.StockMarketService;
import com.stockmarketmod.sound.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

@Mod(StockMarketMod.MODID)
public class StockMarketMod {
    public static final String MODID = "stockmarketmod";
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // Create DeferredRegisters
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    
    // Register blocks and items
    public static final RegistryObject<NasdaqTerminalBlock> NASDAQ_TERMINAL = BLOCKS.register("nasdaq_terminal", 
        NasdaqTerminalBlock::new);
    public static final RegistryObject<Item> NASDAQ_TERMINAL_ITEM = ITEMS.register("nasdaq_terminal", 
        () -> new NasdaqTerminalItem(NASDAQ_TERMINAL.get()));
    
    private final StockMarketService stockMarketService;
    
    public StockMarketMod(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);
        
        // Register DeferredRegisters
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        ModSounds.SOUND_EVENTS.register(modEventBus);
        NasdaqTerminalMenu.MENU_TYPES.register(modEventBus);
        
        // Register creative tab
        modEventBus.addListener(this::addCreative);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        
        // Initialize services
        stockMarketService = StockMarketService.getInstance();
    }
    
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(NASDAQ_TERMINAL_ITEM);
        }
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("Stock Market Mod initialized");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("Stock Market Mod server starting");
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            stockMarketService.updateMarket(event.getServer().overworld());
        }
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            // Some client setup code
            LOGGER.info("Stock Market Mod client setup");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }
    }
} 