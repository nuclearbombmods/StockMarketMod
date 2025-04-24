package com.stockmarketmod.model;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.UUID;

public class Order implements INBTSerializable<CompoundTag> {
    private final UUID orderId;
    private final UUID playerId;
    private final String symbol;
    private final OrderType type;
    private final double price;
    private final int quantity;
    private final long timestamp;

    public enum OrderType {
        BUY,
        SELL
    }

    public Order(UUID orderId, UUID playerId, String symbol, OrderType type, double price, int quantity) {
        this.orderId = orderId;
        this.playerId = playerId;
        this.symbol = symbol;
        this.type = type;
        this.price = price;
        this.quantity = quantity;
        this.timestamp = System.currentTimeMillis();
    }

    public UUID getOrderId() {
        return orderId;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getSymbol() {
        return symbol;
    }

    public OrderType getType() {
        return type;
    }

    public double getPrice() {
        return price;
    }

    public int getQuantity() {
        return quantity;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("orderId", orderId);
        tag.putUUID("playerId", playerId);
        tag.putString("symbol", symbol);
        tag.putString("type", type.name());
        tag.putDouble("price", price);
        tag.putInt("quantity", quantity);
        tag.putLong("timestamp", timestamp);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        // This method is not needed since we're using a constructor-based approach
        throw new UnsupportedOperationException("Order deserialization is not supported");
    }
} 