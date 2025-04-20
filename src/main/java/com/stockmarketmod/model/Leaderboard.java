package com.stockmarketmod.model;

import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Leaderboard {
    private static final int MAX_ENTRIES = 10;
    private final List<LeaderboardEntry> entries = new ArrayList<>();

    public void update(ServerPlayer player, Portfolio portfolio, Map<String, Stock> stocks) {
        double totalValue = portfolio.getTotalValue(stocks);
        UUID playerId = player.getUUID();
        String playerName = player.getGameProfile().getName();
        
        // Remove existing entry if present
        entries.removeIf(entry -> entry.playerId.equals(playerId));
        
        // Add new entry
        entries.add(new LeaderboardEntry(playerId, playerName, totalValue));
        
        // Sort and trim
        entries.sort(Comparator.comparing(LeaderboardEntry::getTotalValue).reversed());
        if (entries.size() > MAX_ENTRIES) {
            entries.remove(entries.size() - 1);
        }
    }

    public List<LeaderboardEntry> getTopPlayers() {
        return new ArrayList<>(entries);
    }

    public static class LeaderboardEntry {
        private final UUID playerId;
        private final String playerName;
        private final double totalValue;

        public LeaderboardEntry(UUID playerId, String playerName, double totalValue) {
            this.playerId = playerId;
            this.playerName = playerName;
            this.totalValue = totalValue;
        }

        public UUID getPlayerId() {
            return playerId;
        }

        public String getPlayerName() {
            return playerName;
        }

        public double getTotalValue() {
            return totalValue;
        }
    }
} 