package com.stockmarketmod.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StockPriceServiceTest {
    private StockPriceService stockPriceService;

    @BeforeEach
    void setUp() {
        stockPriceService = new StockPriceService();
    }

    @Test
    void testGetStockPrice() {
        // Initial prices should be 0.0 for unknown symbols
        assertEquals(0.0, stockPriceService.getStockPrice("UNKNOWN"));
        
        // After simulation, we should get valid prices
        stockPriceService.simulatePriceUpdates();
        
        double aaplPrice = stockPriceService.getStockPrice("AAPL");
        assertTrue(aaplPrice >= 145.0 && aaplPrice <= 155.0, 
            "AAPL price should be between 145 and 155");
        
        double msftPrice = stockPriceService.getStockPrice("MSFT");
        assertTrue(msftPrice >= 292.5 && msftPrice <= 307.5,
            "MSFT price should be between 292.5 and 307.5");
    }

    @Test
    void testGetAllPrices() {
        stockPriceService.simulatePriceUpdates();
        var prices = stockPriceService.getAllPrices();
        
        assertTrue(prices.containsKey("AAPL"));
        assertTrue(prices.containsKey("MSFT"));
        assertTrue(prices.containsKey("GOOGL"));
        assertEquals(3, prices.size());
    }

    @Test
    void testGetModifier() {
        // Modifiers should be 0.0 initially
        assertEquals(0.0, stockPriceService.getModifier("AAPL"));
        assertEquals(0.0, stockPriceService.getModifier("MSFT"));
    }
} 