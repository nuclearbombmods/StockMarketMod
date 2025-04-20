package com.stockmarketmod.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MarketHistory {
    private static final int MAX_HISTORY_SIZE = 100;
    private final Map<String, List<PricePoint>> history = new HashMap<>();

    public void addPricePoint(String symbol, double price, long timestamp) {
        history.computeIfAbsent(symbol, k -> new ArrayList<>()).add(new PricePoint(price, timestamp));
        
        // Trim history if it gets too large
        List<PricePoint> points = history.get(symbol);
        if (points.size() > MAX_HISTORY_SIZE) {
            points.remove(0);
        }
    }

    public List<PricePoint> getHistory(String symbol) {
        return new ArrayList<>(history.getOrDefault(symbol, new ArrayList<>()));
    }

    public double getAveragePrice(String symbol, int points) {
        List<PricePoint> history = getHistory(symbol);
        if (history.isEmpty()) return 0.0;
        
        points = Math.min(points, history.size());
        double sum = 0.0;
        for (int i = history.size() - 1; i >= history.size() - points; i--) {
            sum += history.get(i).price;
        }
        return sum / points;
    }

    public static class PricePoint {
        public final double price;
        public final long timestamp;

        public PricePoint(double price, long timestamp) {
            this.price = price;
            this.timestamp = timestamp;
        }
    }
} 