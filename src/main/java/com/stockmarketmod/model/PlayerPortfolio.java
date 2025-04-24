package com.stockmarketmod.model;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.HashMap;
import java.util.Map;

public class PlayerPortfolio implements INBTSerializable<CompoundTag> {
    private double balance;
    private final Map<String, Integer> stockHoldings;

    public PlayerPortfolio() {
        this.balance = 10000.0; // Starting balance
        this.stockHoldings = new HashMap<>();
    }

    public double getBalance() {
        return balance;
    }

    public void addBalance(double amount) {
        balance += amount;
    }

    public void deductBalance(double amount) {
        balance -= amount;
    }

    public int getStockQuantity(String symbol) {
        return stockHoldings.getOrDefault(symbol, 0);
    }

    public void addStock(String symbol, int quantity) {
        stockHoldings.merge(symbol, quantity, Integer::sum);
    }

    public void removeStock(String symbol, int quantity) {
        int currentQuantity = getStockQuantity(symbol);
        if (currentQuantity >= quantity) {
            stockHoldings.put(symbol, currentQuantity - quantity);
        }
    }

    public Map<String, Integer> getStockHoldings() {
        return new HashMap<>(stockHoldings);
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("balance", balance);

        CompoundTag holdingsTag = new CompoundTag();
        stockHoldings.forEach((symbol, quantity) -> holdingsTag.putInt(symbol, quantity));
        tag.put("holdings", holdingsTag);

        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        balance = tag.getDouble("balance");
        stockHoldings.clear();

        CompoundTag holdingsTag = tag.getCompound("holdings");
        for (String symbol : holdingsTag.getAllKeys()) {
            stockHoldings.put(symbol, holdingsTag.getInt(symbol));
        }
    }
} 