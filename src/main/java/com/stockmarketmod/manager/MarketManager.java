package com.stockmarketmod.manager;

import com.stockmarketmod.model.Market;
import com.stockmarketmod.model.Order;
import com.stockmarketmod.model.PlayerPortfolio;
import com.stockmarketmod.network.NetworkManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber
public class MarketManager extends SavedData {
    private static MarketManager instance;
    private final Market market;
    private final Map<UUID, PlayerPortfolio> playerPortfolios;
    private int tickCounter;

    private MarketManager() {
        this.market = new Market();
        this.playerPortfolios = new HashMap<>();
        this.tickCounter = 0;
        initializeMarket();
    }

    public static MarketManager getInstance() {
        if (instance == null) {
            instance = new MarketManager();
        }
        return instance;
    }

    private void initializeMarket() {
        // Register initial stocks
        market.registerStock("DIAMOND", 100.0, 0.1);  // High value, low volatility
        market.registerStock("IRON", 50.0, 0.2);      // Medium value, medium volatility
        market.registerStock("COAL", 25.0, 0.3);      // Low value, high volatility
    }

    public void placeOrder(UUID playerId, String symbol, Order.OrderType type, double price, int quantity) {
        Order order = new Order(UUID.randomUUID(), playerId, symbol, type, price, quantity);
        market.placeOrder(order);
    }

    public PlayerPortfolio getPlayerPortfolio(UUID playerId) {
        return playerPortfolios.computeIfAbsent(playerId, k -> new PlayerPortfolio());
    }

    public Market getMarket() {
        return market;
    }

    public Set<String> getRegisteredSymbols() {
        return market.getOrderBooks().keySet();
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            MarketManager manager = getInstance();
            manager.tickCounter++;

            // Update market every 20 ticks (1 second)
            if (manager.tickCounter % 20 == 0) {
                manager.updateMarket();
            }
        }
    }

    private void updateMarket() {
        // Update market prices and handle any periodic events
        for (String symbol : getRegisteredSymbols()) {
            market.updatePrice(symbol);
        }
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        // Save market state
        tag.put("market", market.serializeNBT());

        // Save player portfolios
        CompoundTag portfoliosTag = new CompoundTag();
        playerPortfolios.forEach((playerId, portfolio) -> {
            CompoundTag portfolioTag = portfolio.serializeNBT();
            portfoliosTag.put(playerId.toString(), portfolioTag);
        });
        tag.put("portfolios", portfoliosTag);

        return tag;
    }

    public static MarketManager load(CompoundTag tag) {
        MarketManager manager = new MarketManager();
        
        // Load market state
        manager.market.deserializeNBT(tag.getCompound("market"));

        // Load player portfolios
        CompoundTag portfoliosTag = tag.getCompound("portfolios");
        for (String playerIdStr : portfoliosTag.getAllKeys()) {
            UUID playerId = UUID.fromString(playerIdStr);
            PlayerPortfolio portfolio = new PlayerPortfolio();
            portfolio.deserializeNBT(portfoliosTag.getCompound(playerIdStr));
            manager.playerPortfolios.put(playerId, portfolio);
        }

        return manager;
    }

    public void syncToPlayer(ServerPlayer player) {
        // Send market data to player
        NetworkManager.sendMarketData(player, market);
        
        // Send player's portfolio data
        PlayerPortfolio portfolio = getPlayerPortfolio(player.getUUID());
        NetworkManager.sendPortfolioData(player, portfolio);
    }
} 