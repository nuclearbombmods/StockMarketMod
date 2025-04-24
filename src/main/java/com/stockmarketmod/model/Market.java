package com.stockmarketmod.model;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.*;
import java.util.stream.Collectors;

public class Market implements INBTSerializable<CompoundTag> {
    private final Map<String, OrderBook> orderBooks;
    private final Map<String, Double> basePrices;
    private final Map<String, Integer> totalSupply;
    private final Map<String, Integer> totalDemand;
    private final Map<String, Double> priceVolatility;
    private final Random random;

    public Market() {
        this.orderBooks = new HashMap<>();
        this.basePrices = new HashMap<>();
        this.totalSupply = new HashMap<>();
        this.totalDemand = new HashMap<>();
        this.priceVolatility = new HashMap<>();
        this.random = new Random();
    }

    public void registerStock(String symbol, double basePrice, double volatility) {
        orderBooks.put(symbol, new OrderBook(symbol));
        basePrices.put(symbol, basePrice);
        totalSupply.put(symbol, 0);
        totalDemand.put(symbol, 0);
        priceVolatility.put(symbol, volatility);
    }

    public void placeOrder(Order order) {
        String symbol = order.getSymbol();
        OrderBook orderBook = orderBooks.get(symbol);
        if (orderBook == null) {
            throw new IllegalArgumentException("Stock " + symbol + " not registered");
        }

        // Update supply/demand
        if (order.getType() == Order.OrderType.BUY) {
            totalDemand.merge(symbol, order.getQuantity(), Integer::sum);
        } else {
            totalSupply.merge(symbol, order.getQuantity(), Integer::sum);
        }

        // Add order to order book
        orderBook.addOrder(order);

        // Update price based on market conditions
        updatePrice(symbol);
    }

    public void updatePrice(String symbol) {
        OrderBook orderBook = orderBooks.get(symbol);
        double basePrice = basePrices.get(symbol);
        double volatility = priceVolatility.get(symbol);
        
        // Calculate supply/demand ratio
        int supply = totalSupply.get(symbol);
        int demand = totalDemand.get(symbol);
        double ratio = demand > 0 ? (double) supply / demand : 1.0;

        // Calculate volume impact
        int volume = orderBook.getVolume();
        double volumeImpact = Math.log1p(volume) / 100.0;

        // Calculate price adjustment
        double priceAdjustment = (ratio - 1.0) * volatility;
        double randomFactor = 1.0 + (random.nextDouble() - 0.5) * volatility;
        
        // Update base price
        double newBasePrice = basePrice * (1.0 + priceAdjustment + volumeImpact) * randomFactor;
        basePrices.put(symbol, newBasePrice);
    }

    public double getCurrentPrice(String symbol) {
        OrderBook orderBook = orderBooks.get(symbol);
        if (orderBook == null) {
            throw new IllegalArgumentException("Stock " + symbol + " not registered");
        }
        return orderBook.getLastTradePrice() > 0 ? orderBook.getLastTradePrice() : basePrices.get(symbol);
    }

    public MarketDepth getMarketDepth(String symbol) {
        OrderBook orderBook = orderBooks.get(symbol);
        if (orderBook == null) {
            throw new IllegalArgumentException("Stock " + symbol + " not registered");
        }

        List<Order> buyOrders = orderBook.getBuyOrders();
        List<Order> sellOrders = orderBook.getSellOrders();

        return new MarketDepth(
            aggregateOrders(buyOrders),
            aggregateOrders(sellOrders)
        );
    }

    private List<PriceLevel> aggregateOrders(List<Order> orders) {
        Map<Double, Integer> aggregated = orders.stream()
            .collect(Collectors.groupingBy(
                Order::getPrice,
                Collectors.summingInt(Order::getQuantity)
            ));

        // Sort buy orders by price descending, sell orders by price ascending
        Comparator<Map.Entry<Double, Integer>> comparator = Map.Entry.<Double, Integer>comparingByKey();
        if (orders.stream().findFirst().map(o -> o.getType() == Order.OrderType.BUY).orElse(false)) {
            comparator = comparator.reversed();
        }

        return aggregated.entrySet().stream()
            .sorted(comparator)
            .map(entry -> new PriceLevel(entry.getKey(), entry.getValue()))
            .collect(Collectors.toList());
    }

    public Map<String, OrderBook> getOrderBooks() {
        return Collections.unmodifiableMap(orderBooks);
    }

    public int getTotalSupply(String symbol) {
        OrderBook orderBook = orderBooks.get(symbol);
        if (orderBook == null) return 0;
        
        return orderBook.getSellOrders().stream()
            .mapToInt(Order::getQuantity)
            .sum();
    }

    public int getTotalDemand(String symbol) {
        OrderBook orderBook = orderBooks.get(symbol);
        if (orderBook == null) return 0;
        
        return orderBook.getBuyOrders().stream()
            .mapToInt(Order::getQuantity)
            .sum();
    }

    public void addOrder(Order order) {
        OrderBook orderBook = orderBooks.get(order.getSymbol());
        if (orderBook == null) {
            throw new IllegalArgumentException("Stock " + order.getSymbol() + " not registered");
        }
        
        if (order.getType() == Order.OrderType.BUY) {
            orderBook.addBuyOrder(order);
            totalDemand.merge(order.getSymbol(), order.getQuantity(), Integer::sum);
        } else {
            orderBook.addSellOrder(order);
            totalSupply.merge(order.getSymbol(), order.getQuantity(), Integer::sum);
        }
        
        // Update price based on new order
        updatePrice(order.getSymbol());
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        
        // Serialize order books
        CompoundTag orderBooksTag = new CompoundTag();
        orderBooks.forEach((symbol, book) -> orderBooksTag.put(symbol, book.serializeNBT()));
        tag.put("orderBooks", orderBooksTag);

        // Serialize base prices
        CompoundTag basePricesTag = new CompoundTag();
        basePrices.forEach((symbol, price) -> basePricesTag.putDouble(symbol, price));
        tag.put("basePrices", basePricesTag);

        // Serialize supply/demand
        CompoundTag supplyTag = new CompoundTag();
        totalSupply.forEach((symbol, supply) -> supplyTag.putInt(symbol, supply));
        tag.put("totalSupply", supplyTag);

        CompoundTag demandTag = new CompoundTag();
        totalDemand.forEach((symbol, demand) -> demandTag.putInt(symbol, demand));
        tag.put("totalDemand", demandTag);

        // Serialize volatility
        CompoundTag volatilityTag = new CompoundTag();
        priceVolatility.forEach((symbol, volatility) -> volatilityTag.putDouble(symbol, volatility));
        tag.put("priceVolatility", volatilityTag);

        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        // Deserialize order books
        CompoundTag orderBooksTag = tag.getCompound("orderBooks");
        orderBooks.clear();
        for (String symbol : orderBooksTag.getAllKeys()) {
            OrderBook book = new OrderBook(symbol);
            book.deserializeNBT(orderBooksTag.getCompound(symbol));
            orderBooks.put(symbol, book);
        }

        // Deserialize base prices
        CompoundTag basePricesTag = tag.getCompound("basePrices");
        basePrices.clear();
        for (String symbol : basePricesTag.getAllKeys()) {
            basePrices.put(symbol, basePricesTag.getDouble(symbol));
        }

        // Deserialize supply/demand
        CompoundTag supplyTag = tag.getCompound("totalSupply");
        totalSupply.clear();
        for (String symbol : supplyTag.getAllKeys()) {
            totalSupply.put(symbol, supplyTag.getInt(symbol));
        }

        CompoundTag demandTag = tag.getCompound("totalDemand");
        totalDemand.clear();
        for (String symbol : demandTag.getAllKeys()) {
            totalDemand.put(symbol, demandTag.getInt(symbol));
        }

        // Deserialize volatility
        CompoundTag volatilityTag = tag.getCompound("priceVolatility");
        priceVolatility.clear();
        for (String symbol : volatilityTag.getAllKeys()) {
            priceVolatility.put(symbol, volatilityTag.getDouble(symbol));
        }
    }

    public static class MarketDepth {
        private final List<PriceLevel> buyLevels;
        private final List<PriceLevel> sellLevels;

        public MarketDepth(List<PriceLevel> buyLevels, List<PriceLevel> sellLevels) {
            this.buyLevels = buyLevels;
            this.sellLevels = sellLevels;
        }

        public List<PriceLevel> getBuyLevels() {
            return buyLevels;
        }

        public List<PriceLevel> getSellLevels() {
            return sellLevels;
        }
    }

    public static class PriceLevel {
        private final double price;
        private final int quantity;

        public PriceLevel(double price, int quantity) {
            this.price = price;
            this.quantity = quantity;
        }

        public double getPrice() {
            return price;
        }

        public int getQuantity() {
            return quantity;
        }
    }
} 