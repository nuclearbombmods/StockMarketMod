package com.stockmarketmod.network;

import com.stockmarketmod.model.Market;
import com.stockmarketmod.model.PlayerPortfolio;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

public class NetworkManager {
    private static final String PROTOCOL_VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation("stockmarketmod", "main"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );

    public static void register() {
        CHANNEL.registerMessage(0, MarketDataPacket.class,
            MarketDataPacket::encode,
            MarketDataPacket::decode,
            MarketDataPacket::handle);
        
        CHANNEL.registerMessage(1, PortfolioDataPacket.class,
            PortfolioDataPacket::encode,
            PortfolioDataPacket::decode,
            PortfolioDataPacket::handle);
    }

    public static void sendMarketData(ServerPlayer player, Market market) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new MarketDataPacket(market));
    }

    public static void sendPortfolioData(ServerPlayer player, PlayerPortfolio portfolio) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new PortfolioDataPacket(portfolio));
    }

    public static class MarketDataPacket {
        private final Market market;

        public MarketDataPacket(Market market) {
            this.market = market;
        }

        public void encode(FriendlyByteBuf buf) {
            // Serialize market data
            // Implementation depends on what data needs to be sent
        }

        public static MarketDataPacket decode(FriendlyByteBuf buf) {
            // Deserialize market data
            return new MarketDataPacket(null); // Placeholder
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                // Handle market data on client
            });
            ctx.get().setPacketHandled(true);
        }
    }

    public static class PortfolioDataPacket {
        private final PlayerPortfolio portfolio;

        public PortfolioDataPacket(PlayerPortfolio portfolio) {
            this.portfolio = portfolio;
        }

        public void encode(FriendlyByteBuf buf) {
            // Serialize portfolio data
            // Implementation depends on what data needs to be sent
        }

        public static PortfolioDataPacket decode(FriendlyByteBuf buf) {
            // Deserialize portfolio data
            return new PortfolioDataPacket(null); // Placeholder
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                // Handle portfolio data on client
            });
            ctx.get().setPacketHandled(true);
        }
    }
} 