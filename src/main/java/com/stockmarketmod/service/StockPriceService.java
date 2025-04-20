package com.stockmarketmod.service;

import com.stockmarketmod.StockMarketMod;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StockPriceService {
    private static final Logger LOGGER = LoggerFactory.getLogger(StockPriceService.class);
    private static final int UPDATE_INTERVAL = 600; // 10 minutes in ticks (20 ticks per second)
    private int tickCounter = 0;
    
    // Cache for stock prices
    private final Map<String, Double> priceCache = new ConcurrentHashMap<>();
    // Cache for simulated modifiers
    private final Map<String, Double> modifierCache = new ConcurrentHashMap<>();
    
    public StockPriceService() {
        MinecraftForge.EVENT_BUS.register(this);
        LOGGER.info("StockPriceService initialized");
    }
    
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            tickCounter++;
            
            if (tickCounter >= UPDATE_INTERVAL) {
                updatePrices();
                tickCounter = 0;
            }
        }
    }
    
    private void updatePrices() {
        // TODO: Implement real API calls
        // For now, we'll use simulated data
        simulatePriceUpdates();
    }
    
    public void simulatePriceUpdates() {
        // Simulate some stock prices
        priceCache.put("AAPL", 150.0 + (Math.random() * 10 - 5));
        priceCache.put("MSFT", 300.0 + (Math.random() * 15 - 7.5));
        priceCache.put("GOOGL", 2800.0 + (Math.random() * 50 - 25));
        
        LOGGER.debug("Updated stock prices: {}", priceCache);
    }
    
    public double getStockPrice(String symbol) {
        return priceCache.getOrDefault(symbol, 0.0);
    }
    
    public double getModifier(String symbol) {
        return modifierCache.getOrDefault(symbol, 0.0);
    }
    
    public Map<String, Double> getAllPrices() {
        return new HashMap<>(priceCache);
    }
} 