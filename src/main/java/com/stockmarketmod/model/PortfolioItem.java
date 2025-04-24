package com.stockmarketmod.model;

public class PortfolioItem {
    private final Stock stock;
    private final int quantity;
    private final double averagePrice;

    public PortfolioItem(Stock stock, int quantity, double averagePrice) {
        this.stock = stock;
        this.quantity = quantity;
        this.averagePrice = averagePrice;
    }

    public Stock getStock() {
        return stock;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getAveragePrice() {
        return averagePrice;
    }

    public double getCurrentValue() {
        return stock.getCurrentPrice() * quantity;
    }

    public double getTotalProfit() {
        return getCurrentValue() - (averagePrice * quantity);
    }

    public double getProfitPercentage() {
        if (averagePrice == 0) return 0;
        return ((stock.getCurrentPrice() - averagePrice) / averagePrice) * 100;
    }
} 