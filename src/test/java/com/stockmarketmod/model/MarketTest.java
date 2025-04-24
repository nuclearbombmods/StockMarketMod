package com.stockmarketmod.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import java.util.List;

public class MarketTest {
    private Market market;
    private String testSymbol = "TEST";
    private UUID testPlayerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        market = new Market();
        market.registerStock(testSymbol, 100.0, 0.1);
    }

    @Test
    void testSupplyAndDemandCalculation() {
        // Create some buy orders
        Order buyOrder1 = new Order(testPlayerId, testSymbol, Order.OrderType.BUY, 100.0, 10);
        Order buyOrder2 = new Order(testPlayerId, testSymbol, Order.OrderType.BUY, 105.0, 15);
        
        // Create some sell orders
        Order sellOrder1 = new Order(testPlayerId, testSymbol, Order.OrderType.SELL, 110.0, 20);
        Order sellOrder2 = new Order(testPlayerId, testSymbol, Order.OrderType.SELL, 115.0, 25);

        // Place the orders
        market.placeOrder(buyOrder1);
        market.placeOrder(buyOrder2);
        market.placeOrder(sellOrder1);
        market.placeOrder(sellOrder2);

        // Verify total demand (sum of all buy orders)
        assertEquals(25, market.getTotalDemand(testSymbol));

        // Verify total supply (sum of all sell orders)
        assertEquals(45, market.getTotalSupply(testSymbol));
    }

    @Test
    void testMarketDepth() {
        // Create orders at different price levels
        Order buyOrder1 = new Order(testPlayerId, testSymbol, Order.OrderType.BUY, 100.0, 10);
        Order buyOrder2 = new Order(testPlayerId, testSymbol, Order.OrderType.BUY, 95.0, 15);
        Order sellOrder1 = new Order(testPlayerId, testSymbol, Order.OrderType.SELL, 110.0, 20);
        Order sellOrder2 = new Order(testPlayerId, testSymbol, Order.OrderType.SELL, 115.0, 25);

        // Place the orders
        market.placeOrder(buyOrder1);
        market.placeOrder(buyOrder2);
        market.placeOrder(sellOrder1);
        market.placeOrder(sellOrder2);

        // Get market depth
        Market.MarketDepth depth = market.getMarketDepth(testSymbol);
        assertNotNull(depth);

        // Verify buy levels are sorted by price (highest to lowest)
        List<Market.PriceLevel> buyLevels = depth.getBuyLevels();
        assertEquals(2, buyLevels.size());
        assertEquals(100.0, buyLevels.get(0).getPrice());
        assertEquals(95.0, buyLevels.get(1).getPrice());

        // Verify sell levels are sorted by price (lowest to highest)
        List<Market.PriceLevel> sellLevels = depth.getSellLevels();
        assertEquals(2, sellLevels.size());
        assertEquals(110.0, sellLevels.get(0).getPrice());
        assertEquals(115.0, sellLevels.get(1).getPrice());
    }

    @Test
    void testPriceUpdate() {
        // Create initial orders
        Order buyOrder = new Order(testPlayerId, testSymbol, Order.OrderType.BUY, 100.0, 100);
        Order sellOrder = new Order(testPlayerId, testSymbol, Order.OrderType.SELL, 110.0, 50);

        // Place the orders
        market.placeOrder(buyOrder);
        market.placeOrder(sellOrder);

        // Get initial price
        double initialPrice = market.getCurrentPrice(testSymbol);

        // Update price
        market.updatePrice(testSymbol);

        // Get new price
        double newPrice = market.getCurrentPrice(testSymbol);

        // Verify price has changed
        assertNotEquals(initialPrice, newPrice);
    }
} 