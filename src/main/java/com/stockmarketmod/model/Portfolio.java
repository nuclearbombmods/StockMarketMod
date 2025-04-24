package com.stockmarketmod.model;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Portfolio implements INBTSerializable<CompoundTag> {
    private final UUID playerId;
    private double balance;
    private final Map<String, Integer> holdings;
    private static final double EMERALD_VALUE = 100.0; // Each emerald is worth 100 currency units

    public Portfolio(UUID playerId) {
        this.playerId = playerId;
        this.balance = 0.0;
        this.holdings = new HashMap<>();
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public double getBalance() {
        return balance;
    }

    public void addBalance(double amount) {
        this.balance += amount;
    }

    public boolean subtractBalance(double amount) {
        if (this.balance >= amount) {
            this.balance -= amount;
            return true;
        }
        return false;
    }

    public int getHolding(String symbol) {
        return holdings.getOrDefault(symbol, 0);
    }

    public void addHolding(String symbol, int quantity) {
        holdings.merge(symbol, quantity, Integer::sum);
    }

    public boolean removeHolding(String symbol, int quantity) {
        int current = holdings.getOrDefault(symbol, 0);
        if (current >= quantity) {
            holdings.put(symbol, current - quantity);
            return true;
        }
        return false;
    }

    public Map<String, Integer> getAllHoldings() {
        return new HashMap<>(holdings);
    }

    public double getTotalValue(Map<String, Stock> stocks) {
        double total = balance;
        for (Map.Entry<String, Integer> entry : holdings.entrySet()) {
            Stock stock = stocks.get(entry.getKey());
            if (stock != null) {
                total += stock.getCurrentPrice() * entry.getValue();
            }
        }
        return total;
    }

    public boolean depositEmeralds(Player player, int count) {
        int emeraldsToRemove = 0;
        
        // Check all inventory slots including hotbar
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() == Items.EMERALD) {
                int removeAmount = Math.min(count - emeraldsToRemove, stack.getCount());
                stack.shrink(removeAmount);
                emeraldsToRemove += removeAmount;
                if (emeraldsToRemove >= count) break;
            }
        }
        
        // Check offhand if we still need more emeralds
        if (emeraldsToRemove < count) {
            ItemStack offhandStack = player.getOffhandItem();
            if (offhandStack.getItem() == Items.EMERALD) {
                int removeAmount = Math.min(count - emeraldsToRemove, offhandStack.getCount());
                offhandStack.shrink(removeAmount);
                emeraldsToRemove += removeAmount;
            }
        }
        
        if (emeraldsToRemove > 0) {
            addBalance(emeraldsToRemove * EMERALD_VALUE);
            return true;
        }
        return false;
    }

    public boolean withdrawEmeralds(Player player, int count) {
        double emeraldValue = count * EMERALD_VALUE;
        if (subtractBalance(emeraldValue)) {
            int remaining = count;
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (stack.isEmpty() || stack.getItem() == Items.EMERALD) {
                    int addAmount = Math.min(remaining, 64 - (stack.isEmpty() ? 0 : stack.getCount()));
                    if (addAmount > 0) {
                        if (stack.isEmpty()) {
                            player.getInventory().setItem(i, new ItemStack(Items.EMERALD, addAmount));
                        } else {
                            stack.grow(addAmount);
                        }
                        remaining -= addAmount;
                        if (remaining == 0) break;
                    }
                }
            }
            return remaining == 0;
        }
        return false;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("playerId", playerId);
        tag.putDouble("balance", balance);
        
        ListTag holdingsList = new ListTag();
        for (Map.Entry<String, Integer> entry : holdings.entrySet()) {
            CompoundTag holdingTag = new CompoundTag();
            holdingTag.putString("symbol", entry.getKey());
            holdingTag.putInt("quantity", entry.getValue());
            holdingsList.add(holdingTag);
        }
        tag.put("holdings", holdingsList);
        
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        // playerId is set in constructor
        balance = tag.getDouble("balance");
        
        holdings.clear();
        ListTag holdingsList = tag.getList("holdings", Tag.TAG_COMPOUND);
        for (int i = 0; i < holdingsList.size(); i++) {
            CompoundTag holdingTag = holdingsList.getCompound(i);
            String symbol = holdingTag.getString("symbol");
            int quantity = holdingTag.getInt("quantity");
            holdings.put(symbol, quantity);
        }
    }
} 