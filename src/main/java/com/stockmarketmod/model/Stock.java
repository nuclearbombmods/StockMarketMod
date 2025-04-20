package com.stockmarketmod.model;

public class Stock {
    private final String symbol;
    private final String name;
    private double currentPrice;
    private double previousPrice;
    private double highPrice;
    private double lowPrice;
    private long volume;

    public Stock(String symbol, String name, double initialPrice) {
        this.symbol = symbol;
        this.name = name;
        this.currentPrice = initialPrice;
        this.previousPrice = initialPrice;
        this.highPrice = initialPrice;
        this.lowPrice = initialPrice;
        this.volume = 0;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getName() {
        return name;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(double price) {
        this.previousPrice = this.currentPrice;
        this.currentPrice = price;
        this.highPrice = Math.max(this.highPrice, price);
        this.lowPrice = Math.min(this.lowPrice, price);
    }

    public double getPreviousPrice() {
        return previousPrice;
    }

    public double getHighPrice() {
        return highPrice;
    }

    public double getLowPrice() {
        return lowPrice;
    }

    public long getVolume() {
        return volume;
    }

    public void addVolume(long amount) {
        this.volume += amount;
    }

    public double getPriceChange() {
        return currentPrice - previousPrice;
    }

    public double getPriceChangePercentage() {
        return (getPriceChange() / previousPrice) * 100;
    }
} 