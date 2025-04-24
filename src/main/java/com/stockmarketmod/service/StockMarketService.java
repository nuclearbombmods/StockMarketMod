package com.stockmarketmod.service;

import com.stockmarketmod.model.Market;
import com.stockmarketmod.model.MarketHistory;
import com.stockmarketmod.model.Portfolio;
import com.stockmarketmod.model.Stock;
import com.stockmarketmod.model.Order;
import com.stockmarketmod.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class StockMarketService extends SavedData {
    private static final String DATA_NAME = "stock_market_data";
    private static StockMarketService INSTANCE;
    private final Map<String, Stock> stocks = new HashMap<>();
    private final Map<UUID, Portfolio> portfolios = new HashMap<>();
    private final MarketHistory marketHistory = new MarketHistory();
    private final Random random = new Random();
    private long lastUpdateTime = 0;
    private static final long UPDATE_INTERVAL = 20 * 60; // 1 minute in ticks
    private static final double EVENT_CHANCE = 0.1; // 10% chance of market event
    private Market market;

    private StockMarketService() {
        initializeStocks();
        initializeMarket();
    }

    private void initializeMarket() {
        if (market == null) {
            market = new Market();
            // Initialize stocks in the market
            for (Map.Entry<String, Stock> entry : stocks.entrySet()) {
                market.registerStock(entry.getKey(), entry.getValue().getCurrentPrice(), 0.1); // Default volatility of 0.1
            }
        }
    }

    public static StockMarketService getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new StockMarketService();
        }
        return INSTANCE;
    }

    public static StockMarketService get(ServerLevel level) {
        if (INSTANCE != null) {
            return INSTANCE;
        }
        DimensionDataStorage storage = level.getDataStorage();
        INSTANCE = storage.computeIfAbsent(StockMarketService::load, StockMarketService::new, DATA_NAME);
        return INSTANCE;
    }

    public static StockMarketService load(CompoundTag tag) {
        StockMarketService service = new StockMarketService();
        
        // Load stocks
        ListTag stocksList = tag.getList("stocks", Tag.TAG_COMPOUND);
        for (int i = 0; i < stocksList.size(); i++) {
            CompoundTag stockTag = stocksList.getCompound(i);
            Stock stock = new Stock(stockTag.getString("symbol"), stockTag.getString("name"), 0.0);
            stock.deserializeNBT(stockTag);
            service.stocks.put(stock.getSymbol(), stock);
        }
        
        // Load portfolios
        ListTag portfoliosList = tag.getList("portfolios", Tag.TAG_COMPOUND);
        for (int i = 0; i < portfoliosList.size(); i++) {
            CompoundTag portfolioTag = portfoliosList.getCompound(i);
            UUID playerId = portfolioTag.getUUID("playerId");
            Portfolio portfolio = new Portfolio(playerId);
            portfolio.deserializeNBT(portfolioTag);
            service.portfolios.put(playerId, portfolio);
        }
        
        // Load market history
        service.marketHistory.deserializeNBT(tag.getCompound("marketHistory"));
        
        // Load other data
        service.lastUpdateTime = tag.getLong("lastUpdateTime");

        // Load market data
        if (tag.contains("market")) {
            service.market = new Market();
            service.market.deserializeNBT(tag.getCompound("market"));
        } else {
            service.initializeMarket();
        }
        
        return service;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        // Save stocks
        ListTag stocksList = new ListTag();
        for (Stock stock : stocks.values()) {
            stocksList.add(stock.serializeNBT());
        }
        tag.put("stocks", stocksList);
        
        // Save portfolios
        ListTag portfoliosList = new ListTag();
        for (Portfolio portfolio : portfolios.values()) {
            portfoliosList.add(portfolio.serializeNBT());
        }
        tag.put("portfolios", portfoliosList);
        
        // Save market history
        tag.put("marketHistory", marketHistory.serializeNBT());
        
        // Save other data
        tag.putLong("lastUpdateTime", lastUpdateTime);

        // Save market data
        if (market != null) {
            tag.put("market", market.serializeNBT());
        }
        
        return tag;
    }

    private void initializeStocks() {
        // Mining stocks
        addStock("DIAM", "Diamond Corp", 100.0);
        addStock("GOLD", "Gold Inc", 150.0);
        addStock("IRON", "Iron Works", 75.0);
        addStock("EMER", "Emerald Ltd", 200.0);
        addStock("COAL", "Coal Energy", 50.0);
        
        // Farming stocks
        addStock("WHEAT", "Wheat Fields", 60.0);
        addStock("CARROT", "Carrot Co", 45.0);
        addStock("POTATO", "Potato Farms", 40.0);
        addStock("SUGAR", "Sugar Cane", 55.0);
        
        // Technology stocks
        addStock("REDST", "Redstone Tech", 120.0);
        addStock("OBSID", "Obsidian Systems", 180.0);
        addStock("ENCHT", "Enchantment Corp", 250.0);
        
        // Transportation stocks
        addStock("RAIL", "Railway Co", 80.0);
        addStock("BOAT", "Boat Works", 65.0);
        addStock("HORSE", "Horse Power", 90.0);
    }

    private void addStock(String symbol, String name, double initialPrice) {
        stocks.put(symbol, new Stock(symbol, name, initialPrice));
    }

    public void updateMarket(Level level) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        
        long currentTime = level.getGameTime();
        if (currentTime - lastUpdateTime < UPDATE_INTERVAL) return;
        
        lastUpdateTime = currentTime;
        
        // Check for market events
        if (random.nextDouble() < EVENT_CHANCE) {
            triggerMarketEvent(serverLevel);
        }
        
        // Update stock prices
        for (Stock stock : stocks.values()) {
            updateStockPrice(stock);
            marketHistory.addPricePoint(stock.getSymbol(), stock.getCurrentPrice(), currentTime);
        }
        
        // Play market update sound
        serverLevel.playSound(null, BlockPos.ZERO, ModSounds.MARKET_UPDATE.get(), 
            SoundSource.MASTER, 1.0f, 1.0f);
    }

    private void triggerMarketEvent(ServerLevel level) {
        // Randomly select a market event
        int eventType = random.nextInt(3);
        switch (eventType) {
            case 0 -> triggerMarketCrash(level);
            case 1 -> triggerMarketBoom(level);
            case 2 -> triggerSectorEvent(level);
        }
    }

    private void triggerMarketCrash(ServerLevel level) {
        for (Stock stock : stocks.values()) {
            double currentPrice = stock.getCurrentPrice();
            double newPrice = currentPrice * (0.5 + random.nextDouble() * 0.3); // 50-80% of current price
            stock.setCurrentPrice(newPrice);
        }
        level.playSound(null, BlockPos.ZERO, ModSounds.TRADE_FAIL.get(), 
            SoundSource.MASTER, 1.0f, 0.8f);
    }

    private void triggerMarketBoom(ServerLevel level) {
        for (Stock stock : stocks.values()) {
            double currentPrice = stock.getCurrentPrice();
            double newPrice = currentPrice * (1.2 + random.nextDouble() * 0.3); // 120-150% of current price
            stock.setCurrentPrice(newPrice);
        }
        level.playSound(null, BlockPos.ZERO, ModSounds.TRADE_SUCCESS.get(), 
            SoundSource.MASTER, 1.0f, 1.2f);
    }

    private void triggerSectorEvent(ServerLevel level) {
        String[] sectors = {"mining", "farming", "tech", "transport"};
        String affectedSector = sectors[random.nextInt(sectors.length)];
        double multiplier = 0.5 + random.nextDouble(); // 50-150% change
        
        for (Stock stock : stocks.values()) {
            String symbol = stock.getSymbol();
            if ((affectedSector.equals("mining") && symbol.matches("DIAM|GOLD|IRON|EMER|COAL")) ||
                (affectedSector.equals("farming") && symbol.matches("WHEAT|CARROT|POTATO|SUGAR")) ||
                (affectedSector.equals("tech") && symbol.matches("REDST|OBSID|ENCHT")) ||
                (affectedSector.equals("transport") && symbol.matches("RAIL|BOAT|HORSE"))) {
                double currentPrice = stock.getCurrentPrice();
                double newPrice = currentPrice * multiplier;
                stock.setCurrentPrice(newPrice);
            }
        }
        level.playSound(null, BlockPos.ZERO, ModSounds.MARKET_UPDATE.get(), 
            SoundSource.MASTER, 1.0f, 1.0f);
    }

    private void updateStockPrice(Stock stock) {
        double currentPrice = stock.getCurrentPrice();
        double change = (random.nextDouble() - 0.5) * 10.0; // Random change between -5 and +5
        double newPrice = Math.max(1.0, currentPrice + change); // Ensure price never goes below 1
        
        // Simulate some volume
        long volume = random.nextInt(1000) + 100;
        stock.addVolume(volume);
        
        // Update stock price
        stock.setCurrentPrice(newPrice);
        
        // Add simulated orders to create market depth
        if (market != null) {
            // Add some buy orders
            for (int i = 0; i < 5; i++) {
                double buyPrice = newPrice * (0.95 + random.nextDouble() * 0.05); // 95-100% of current price
                int buyQuantity = random.nextInt(100) + 10;
                Order buyOrder = new Order(UUID.randomUUID(), UUID.randomUUID(), stock.getSymbol(), 
                    Order.OrderType.BUY, buyPrice, buyQuantity);
                market.addOrder(buyOrder);
            }
            
            // Add some sell orders
            for (int i = 0; i < 5; i++) {
                double sellPrice = newPrice * (1.0 + random.nextDouble() * 0.05); // 100-105% of current price
                int sellQuantity = random.nextInt(100) + 10;
                Order sellOrder = new Order(UUID.randomUUID(), UUID.randomUUID(), stock.getSymbol(), 
                    Order.OrderType.SELL, sellPrice, sellQuantity);
                market.addOrder(sellOrder);
            }
        }
    }

    public Stock getStock(String symbol) {
        return stocks.get(symbol);
    }

    public Map<String, Stock> getAllStocks() {
        return new HashMap<>(stocks);
    }

    public Portfolio getPortfolio(Player player) {
        return portfolios.computeIfAbsent(player.getUUID(), Portfolio::new);
    }

    public boolean buyStock(Player player, String symbol, int quantity) {
        Stock stock = stocks.get(symbol);
        if (stock == null) return false;
        
        Portfolio portfolio = getPortfolio(player);
        double totalCost = stock.getCurrentPrice() * quantity;
        
        if (portfolio.subtractBalance(totalCost)) {
            portfolio.addHolding(symbol, quantity);
            stock.addVolume(quantity);
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.level().playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                    ModSounds.TRADE_SUCCESS.get(), SoundSource.MASTER, 1.0f, 1.0f);
            }
            return true;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.level().playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                ModSounds.TRADE_FAIL.get(), SoundSource.MASTER, 1.0f, 1.0f);
        }
        return false;
    }

    public boolean sellStock(Player player, String symbol, int quantity) {
        Stock stock = stocks.get(symbol);
        if (stock == null) return false;
        
        Portfolio portfolio = getPortfolio(player);
        
        if (portfolio.removeHolding(symbol, quantity)) {
            double totalValue = stock.getCurrentPrice() * quantity;
            portfolio.addBalance(totalValue);
            stock.addVolume(quantity);
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.level().playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                    ModSounds.TRADE_SUCCESS.get(), SoundSource.MASTER, 1.0f, 1.0f);
            }
            return true;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.level().playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                ModSounds.TRADE_FAIL.get(), SoundSource.MASTER, 1.0f, 1.0f);
        }
        return false;
    }

    public boolean depositEmeralds(Player player, int count) {
        Portfolio portfolio = getPortfolio(player);
        boolean success = portfolio.depositEmeralds(player, count);
        if (success) {
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.level().playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                    ModSounds.TRADE_SUCCESS.get(), SoundSource.MASTER, 1.0f, 1.0f);
            }
        } else {
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.level().playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                    ModSounds.TRADE_FAIL.get(), SoundSource.MASTER, 1.0f, 1.0f);
            }
        }
        return success;
    }

    public boolean withdrawEmeralds(Player player, int count) {
        if (!(player instanceof ServerPlayer serverPlayer)) return false;
        Portfolio portfolio = getPortfolio(player);
        boolean success = portfolio.withdrawEmeralds(player, count);
        if (success) {
            serverPlayer.level().playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                ModSounds.TRADE_SUCCESS.get(), SoundSource.MASTER, 1.0f, 1.0f);
        } else {
            serverPlayer.level().playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                ModSounds.TRADE_FAIL.get(), SoundSource.MASTER, 1.0f, 1.0f);
        }
        return success;
    }

    public MarketHistory getMarketHistory() {
        return marketHistory;
    }

    public void refreshStocks() {
        // Force an immediate market update by resetting the last update time
        lastUpdateTime = 0;
        // The actual update will happen on the next tick through the normal update mechanism
    }

    public Market getMarket() {
        if (market == null) {
            market = new Market();
            // Re-initialize stocks in case this is called after initialization
            for (Map.Entry<String, Stock> entry : stocks.entrySet()) {
                market.registerStock(entry.getKey(), entry.getValue().getCurrentPrice(), 0.1);
            }
        }
        return market;
    }
} 