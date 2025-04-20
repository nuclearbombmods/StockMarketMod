package com.stockmarketmod;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = StockMarketMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.BooleanValue ENABLE_STOCK_MARKET = BUILDER
            .comment("Whether to enable the stock market feature")
            .define("enableStockMarket", true);

    private static final ForgeConfigSpec.IntValue INITIAL_BALANCE = BUILDER
            .comment("Initial balance for players in the stock market")
            .defineInRange("initialBalance", 1000, 0, Integer.MAX_VALUE);

    public static final ForgeConfigSpec.ConfigValue<String> CURRENCY_SYMBOL = BUILDER
            .comment("The currency symbol to use in the stock market")
            .define("currencySymbol", "$");

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean enableStockMarket;
    public static int initialBalance;
    public static String currencySymbol;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        enableStockMarket = ENABLE_STOCK_MARKET.get();
        initialBalance = INITIAL_BALANCE.get();
        currencySymbol = CURRENCY_SYMBOL.get();
    }
} 