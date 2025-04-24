package com.stockmarketmod.model;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.*;
import java.util.stream.Collectors;

public class OrderBook implements INBTSerializable<CompoundTag> {
    private final String symbol;
    private final TreeMap<Double, List<Order>> buyOrders;  // Sorted by price (descending)
    private final TreeMap<Double, List<Order>> sellOrders; // Sorted by price (ascending)
    private double lastTradePrice;
    private int volume;

    public OrderBook(String symbol) {
        this.symbol = symbol;
        this.buyOrders = new TreeMap<>(Comparator.reverseOrder());
        this.sellOrders = new TreeMap<>();
        this.lastTradePrice = 0;
        this.volume = 0;
    }

    public void addOrder(Order order) {
        TreeMap<Double, List<Order>> orders = order.getType() == Order.OrderType.BUY ? buyOrders : sellOrders;
        orders.computeIfAbsent(order.getPrice(), k -> new ArrayList<>()).add(order);
        matchOrders();
    }

    private void matchOrders() {
        while (!buyOrders.isEmpty() && !sellOrders.isEmpty()) {
            Map.Entry<Double, List<Order>> bestBid = buyOrders.firstEntry();
            Map.Entry<Double, List<Order>> bestAsk = sellOrders.firstEntry();

            if (bestBid.getKey() >= bestAsk.getKey()) {
                // Match orders
                Order buyOrder = bestBid.getValue().get(0);
                Order sellOrder = bestAsk.getValue().get(0);
                int quantity = Math.min(buyOrder.getQuantity(), sellOrder.getQuantity());

                // Execute trade
                executeTrade(buyOrder, sellOrder, quantity);

                // Remove or update orders
                if (buyOrder.getQuantity() == 0) {
                    bestBid.getValue().remove(0);
                    if (bestBid.getValue().isEmpty()) {
                        buyOrders.remove(bestBid.getKey());
                    }
                }
                if (sellOrder.getQuantity() == 0) {
                    bestAsk.getValue().remove(0);
                    if (bestAsk.getValue().isEmpty()) {
                        sellOrders.remove(bestAsk.getKey());
                    }
                }
            } else {
                break;
            }
        }
    }

    private void executeTrade(Order buyOrder, Order sellOrder, int quantity) {
        double price = (buyOrder.getPrice() + sellOrder.getPrice()) / 2;
        lastTradePrice = price;
        volume += quantity;

        // Create new orders with updated quantities
        Order newBuyOrder = new Order(buyOrder.getOrderId(), buyOrder.getPlayerId(), buyOrder.getSymbol(), 
            Order.OrderType.BUY, buyOrder.getPrice(), buyOrder.getQuantity() - quantity);
        Order newSellOrder = new Order(sellOrder.getOrderId(), sellOrder.getPlayerId(), sellOrder.getSymbol(), 
            Order.OrderType.SELL, sellOrder.getPrice(), sellOrder.getQuantity() - quantity);

        // Replace old orders with new ones
        List<Order> buyList = buyOrders.get(buyOrder.getPrice());
        List<Order> sellList = sellOrders.get(sellOrder.getPrice());
        
        if (buyList != null) {
            buyList.remove(buyOrder);
            if (newBuyOrder.getQuantity() > 0) {
                buyList.add(newBuyOrder);
            }
        }
        
        if (sellList != null) {
            sellList.remove(sellOrder);
            if (newSellOrder.getQuantity() > 0) {
                sellList.add(newSellOrder);
            }
        }
    }

    public double getLastTradePrice() {
        return lastTradePrice;
    }

    public int getVolume() {
        return volume;
    }

    public List<Order> getBuyOrders() {
        return buyOrders.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    public List<Order> getSellOrders() {
        return sellOrders.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    public void addBuyOrder(Order order) {
        buyOrders.computeIfAbsent(order.getPrice(), k -> new ArrayList<>()).add(order);
        volume += order.getQuantity();
    }

    public void addSellOrder(Order order) {
        sellOrders.computeIfAbsent(order.getPrice(), k -> new ArrayList<>()).add(order);
        volume += order.getQuantity();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("symbol", symbol);
        tag.putDouble("lastTradePrice", lastTradePrice);
        tag.putInt("volume", volume);

        ListTag buyOrdersTag = new ListTag();
        buyOrders.values().stream()
                .flatMap(List::stream)
                .forEach(order -> buyOrdersTag.add(order.serializeNBT()));
        tag.put("buyOrders", buyOrdersTag);

        ListTag sellOrdersTag = new ListTag();
        sellOrders.values().stream()
                .flatMap(List::stream)
                .forEach(order -> sellOrdersTag.add(order.serializeNBT()));
        tag.put("sellOrders", sellOrdersTag);

        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        lastTradePrice = tag.getDouble("lastTradePrice");
        volume = tag.getInt("volume");

        buyOrders.clear();
        ListTag buyOrdersTag = tag.getList("buyOrders", Tag.TAG_COMPOUND);
        for (int i = 0; i < buyOrdersTag.size(); i++) {
            CompoundTag orderTag = buyOrdersTag.getCompound(i);
            Order order = new Order(
                orderTag.getUUID("orderId"),
                orderTag.getUUID("playerId"),
                symbol,
                Order.OrderType.BUY,
                orderTag.getDouble("price"),
                orderTag.getInt("quantity")
            );
            buyOrders.computeIfAbsent(order.getPrice(), k -> new ArrayList<>()).add(order);
        }

        sellOrders.clear();
        ListTag sellOrdersTag = tag.getList("sellOrders", Tag.TAG_COMPOUND);
        for (int i = 0; i < sellOrdersTag.size(); i++) {
            CompoundTag orderTag = sellOrdersTag.getCompound(i);
            Order order = new Order(
                orderTag.getUUID("orderId"),
                orderTag.getUUID("playerId"),
                symbol,
                Order.OrderType.SELL,
                orderTag.getDouble("price"),
                orderTag.getInt("quantity")
            );
            sellOrders.computeIfAbsent(order.getPrice(), k -> new ArrayList<>()).add(order);
        }
    }
} 