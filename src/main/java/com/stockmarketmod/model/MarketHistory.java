package com.stockmarketmod.model;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MarketHistory implements INBTSerializable<CompoundTag> {
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

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        for (Map.Entry<String, List<PricePoint>> entry : history.entrySet()) {
            ListTag pricePointsList = new ListTag();
            for (PricePoint point : entry.getValue()) {
                CompoundTag pointTag = new CompoundTag();
                pointTag.putDouble("price", point.price);
                pointTag.putLong("timestamp", point.timestamp);
                pricePointsList.add(pointTag);
            }
            tag.put(entry.getKey(), pricePointsList);
        }
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        history.clear();
        for (String key : tag.getAllKeys()) {
            ListTag pricePointsList = tag.getList(key, Tag.TAG_COMPOUND);
            List<PricePoint> points = new ArrayList<>();
            for (int i = 0; i < pricePointsList.size(); i++) {
                CompoundTag pointTag = pricePointsList.getCompound(i);
                points.add(new PricePoint(
                    pointTag.getDouble("price"),
                    pointTag.getLong("timestamp")
                ));
            }
            history.put(key, points);
        }
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