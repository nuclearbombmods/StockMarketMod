package com.stockmarketmod.screen;

import com.mojang.logging.LogUtils;
import com.stockmarketmod.model.Portfolio;
import com.stockmarketmod.model.Stock;
import com.stockmarketmod.service.StockMarketService;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;

import java.text.DecimalFormat;
import java.util.Map;

public class NasdaqTerminalScreen extends Screen {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final DecimalFormat PRICE_FORMAT = new DecimalFormat("#,##0.00");
    private static final DecimalFormat CHANGE_FORMAT = new DecimalFormat("+#,##0.00;-#,##0.00");
    private static final int SECTION_SPACING = 10;
    private static final int ROW_HEIGHT = 15;
    private static final int COLUMN_SPACING = 10;
    private static final int ACTION_ICON_SIZE = 10;
    private static final int POPUP_WIDTH = 200;
    private static final int POPUP_HEIGHT = 100;
    
    // Column widths
    private static final int SYMBOL_WIDTH = 60;
    private static final int NAME_WIDTH = 120;
    private static final int PRICE_WIDTH = 70;
    private static final int CHANGE_WIDTH = 70;
    private static final int VOLUME_WIDTH = 80;
    private static final int ACTIONS_WIDTH = 30;
    private static final int SCREEN_WIDTH = 
        SYMBOL_WIDTH + NAME_WIDTH + PRICE_WIDTH + CHANGE_WIDTH + VOLUME_WIDTH + ACTIONS_WIDTH + (COLUMN_SPACING * 8);
    
    private final StockMarketService stockMarketService;
    private String selectedStock = null;

    // Transaction popup state
    private boolean showTransactionPopup = false;
    private String transactionType = ""; // "buy" or "sell"
    private EditBox quantityInput;
    private Button confirmButton;
    private Button cancelButton;

    public NasdaqTerminalScreen() {
        super(Component.literal("Terminal"));
        this.stockMarketService = StockMarketService.getInstance();
        LOGGER.info("Initializing full-screen Terminal");
    }

    @Override
    protected void init() {
        super.init();
        LOGGER.info("Initializing Terminal screen components");
        
        // Initialize transaction popup components
        quantityInput = new EditBox(this.font, 
            width / 2 - POPUP_WIDTH / 2 + 10, 
            height / 2 - POPUP_HEIGHT / 2 + 30, 
            POPUP_WIDTH - 20, 20, 
            Component.translatable("container.stockmarketmod.quantity"));
        quantityInput.setValue("1");
        quantityInput.setTextColor(0x000000);
        quantityInput.setBordered(true);
        
        // Create buttons with explicit onPress handlers
        confirmButton = Button.builder(Component.literal("Confirm"), button -> {
            LOGGER.info("Confirm button clicked");
            handleTransaction();
        })
        .pos(width / 2 - POPUP_WIDTH / 2 + 10, 
            height / 2 - POPUP_HEIGHT / 2 + 60)
        .size(80, 20)
        .build();
            
        cancelButton = Button.builder(Component.literal("Cancel"), button -> {
            LOGGER.info("Cancel button clicked");
            hideTransactionPopup();
        })
        .pos(width / 2 + POPUP_WIDTH / 2 - 90, 
            height / 2 - POPUP_HEIGHT / 2 + 60)
        .size(80, 20)
        .build();
    }

    private void showTransactionPopup(String type) {
        LOGGER.info("Showing transaction popup for type: {}", type);
        showTransactionPopup = true;
        transactionType = type;
        quantityInput.setValue("1");
        
        // Clear existing widgets
        clearWidgets();
        
        // Add popup widgets
        addRenderableWidget(quantityInput);
        addRenderableWidget(confirmButton);
        addRenderableWidget(cancelButton);
    }

    private void hideTransactionPopup() {
        showTransactionPopup = false;
        clearWidgets();
    }

    private void handleTransaction() {
        LOGGER.info("Handling transaction for stock: {}", selectedStock);
        if (selectedStock != null) {
            try {
                int quantity = Integer.parseInt(quantityInput.getValue());
                LOGGER.info("Transaction quantity: {}", quantity);
                if (quantity > 0) {
                    boolean success = false;
                    if (transactionType.equals("buy")) {
                        success = stockMarketService.buyStock(minecraft.player, selectedStock, quantity);
                        LOGGER.info("Buy transaction result: {}", success);
                    } else if (transactionType.equals("sell")) {
                        success = stockMarketService.sellStock(minecraft.player, selectedStock, quantity);
                        LOGGER.info("Sell transaction result: {}", success);
                    }
                    
                    if (success) {
                        hideTransactionPopup();
                    }
                }
            } catch (NumberFormatException e) {
                LOGGER.error("Invalid quantity input: {}", quantityInput.getValue());
            }
        }
    }

    private void handleRefresh() {
        stockMarketService.updateMarket(minecraft.level);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Draw the background terminal first
        renderBackground(guiGraphics);
        
        if (showTransactionPopup) {
            // Draw solid black background for the entire screen
            guiGraphics.fill(0, 0, this.width, this.height, 0xFF000000);
            
            // Draw the popup with a solid white background
            int popupX = width / 2 - POPUP_WIDTH / 2;
            int popupY = height / 2 - POPUP_HEIGHT / 2;
            
            // Draw solid white background for popup
            guiGraphics.fill(popupX, popupY, popupX + POPUP_WIDTH, popupY + POPUP_HEIGHT, 0xFFFFFFFF);
            
            // Draw popup title with black text
            String title = transactionType.equals("buy") ? "Buy Stock" : "Sell Stock";
            guiGraphics.drawString(font, title, 
                width / 2 - font.width(title) / 2,
                popupY + 10,
                0x000000, false);
            
            // Draw stock info in black
            Stock stock = stockMarketService.getStock(selectedStock);
            if (stock != null) {
                String stockInfo = String.format("%s - %s", stock.getSymbol(), stock.getName());
                guiGraphics.drawString(font, stockInfo,
                    width / 2 - font.width(stockInfo) / 2,
                    popupY + 30,
                    0x000000, false);
            }
        } else {
            // Draw market section
            int marketY = 10; // Reduced from HEADER_HEIGHT
            int marketHeight = (height - 10) / 2 - SECTION_SPACING;
            renderMarketSection(guiGraphics, 0, marketY, width, marketHeight);

            // Draw portfolio section
            int portfolioY = marketY + marketHeight + SECTION_SPACING;
            int portfolioHeight = height - portfolioY - 20;
            renderPortfolioSection(guiGraphics, 0, portfolioY, width, portfolioHeight);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderMarketSection(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        Portfolio portfolio = stockMarketService.getPortfolio(minecraft.player);
        
        // Section title
        int marketY = y + 2; // Using inline value instead of HEADER_HEIGHT constant
        guiGraphics.drawString(font, "MARKET OVERVIEW", x + 10, marketY, 0xFFFFFF);
        
        // Draw headers
        int headerY = marketY + 16;
        int columnX = x + 10;
        
        guiGraphics.fillGradient(x, headerY - 2, x + SCREEN_WIDTH, headerY + ROW_HEIGHT - 4, 0x40FFFFFF, 0x40FFFFFF);
        
        guiGraphics.drawString(font, "Symbol", columnX, headerY, 0xFFFFFF);
        columnX += SYMBOL_WIDTH + COLUMN_SPACING;
        
        guiGraphics.drawString(font, "Name", columnX, headerY, 0xFFFFFF);
        columnX += NAME_WIDTH + COLUMN_SPACING;
        
        guiGraphics.drawString(font, "Price", columnX, headerY, 0xFFFFFF);
        columnX += PRICE_WIDTH + COLUMN_SPACING;
        
        guiGraphics.drawString(font, "Change", columnX, headerY, 0xFFFFFF);
        columnX += CHANGE_WIDTH + COLUMN_SPACING;
        
        guiGraphics.drawString(font, "Volume", columnX, headerY, 0xFFFFFF);
        columnX += VOLUME_WIDTH + COLUMN_SPACING;
        
        guiGraphics.drawString(font, "Actions", columnX, headerY, 0xFFFFFF);

        // Draw stocks
        int rowY = headerY + ROW_HEIGHT;
        Map<String, Stock> stocks = stockMarketService.getAllStocks();
        
        for (Stock stock : stocks.values()) {
            if (rowY + ROW_HEIGHT > y + height) break;
            
            // Highlight selected stock
            if (selectedStock != null && stock.getSymbol().equals(selectedStock)) {
                guiGraphics.fillGradient(x + 6, rowY - 3, x + SCREEN_WIDTH, rowY + ROW_HEIGHT - 4, 0x40FFFFFF, 0x40FFFFFF);
            }

            columnX = x + 10;
            
            guiGraphics.drawString(font, stock.getSymbol(), columnX, rowY, 0xFFFFFF);
            columnX += SYMBOL_WIDTH + COLUMN_SPACING;
            
            String name = stock.getName();
            if (font.width(name) > NAME_WIDTH) {
                name = font.plainSubstrByWidth(name, NAME_WIDTH - 10) + "...";
            }
            guiGraphics.drawString(font, name, columnX, rowY, 0xFFFFFF);
            columnX += NAME_WIDTH + COLUMN_SPACING;
            
            guiGraphics.drawString(font, PRICE_FORMAT.format(stock.getCurrentPrice()), columnX, rowY, 0xFFFFFF);
            columnX += PRICE_WIDTH + COLUMN_SPACING;
            
            double change = stock.getPriceChangePercentage();
            int changeColor = change >= 0 ? 0x00FF00 : 0xFF0000;
            String changeText = CHANGE_FORMAT.format(change) + "%";
            guiGraphics.drawString(font, changeText, columnX, rowY, changeColor);
            columnX += CHANGE_WIDTH + COLUMN_SPACING;
            
            guiGraphics.drawString(font, String.format("%,d", stock.getVolume()), columnX, rowY, 0xFFFFFF);
            columnX += VOLUME_WIDTH + COLUMN_SPACING;
            
            // Draw action icons
            int iconY = rowY + (ROW_HEIGHT - ACTION_ICON_SIZE) / 2 - 6;
            
            // Buy icon (always visible)
            guiGraphics.renderItem(new ItemStack(Items.EMERALD), 
                columnX, iconY);
            
            // Sell icon (only if owned)
            if (portfolio.getHolding(stock.getSymbol()) > 0) {
                guiGraphics.renderItem(new ItemStack(Items.REDSTONE), 
                    columnX + ACTION_ICON_SIZE + 2, iconY);
            }
            
            rowY += ROW_HEIGHT;
        }
    }

    private void renderPortfolioSection(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        // Section title and balance
        Portfolio portfolio = stockMarketService.getPortfolio(minecraft.player);
        guiGraphics.drawString(font, "YOUR PORTFOLIO", x + 10, y, 0xFFFFFF);
        
        // Calculate emerald balance
        int emeraldCount = 0;
        for (ItemStack stack : minecraft.player.getInventory().items) {
            if (stack.getItem() == Items.EMERALD) {
                emeraldCount += stack.getCount();
            }
        }
        
        // Calculate total stock value
        double stockValue = 0.0;
        Map<String, Integer> holdings = portfolio.getAllHoldings();
        Map<String, Stock> stocks = stockMarketService.getAllStocks();
        for (Map.Entry<String, Integer> entry : holdings.entrySet()) {
            Stock stock = stocks.get(entry.getKey());
            if (stock != null) {
                stockValue += stock.getCurrentPrice() * entry.getValue();
            }
        }
        
        // Display wealth breakdown
        String wealthText = String.format("Wealth: %d emeralds (Bank: %.2f, Stocks: %.2f)", 
            emeraldCount, portfolio.getBalance(), stockValue);
        guiGraphics.drawString(font, wealthText, x + width - font.width(wealthText) - 10, y, 0xFFFFFF);
        
        // Draw headers
        int headerY = y + 16;
        int columnX = x + 10;

        guiGraphics.fillGradient(x, headerY - 2, x + SCREEN_WIDTH, headerY + ROW_HEIGHT - 4, 0x40FFFFFF, 0x40FFFFFF);
        
        guiGraphics.drawString(font, "Symbol", columnX, headerY, 0xFFFFFF);
        columnX += SYMBOL_WIDTH + COLUMN_SPACING;
        
        guiGraphics.drawString(font, "Quantity", columnX, headerY, 0xFFFFFF);
        columnX += PRICE_WIDTH + COLUMN_SPACING;
        
        guiGraphics.drawString(font, "Avg Price", columnX, headerY, 0xFFFFFF);
        columnX += PRICE_WIDTH + COLUMN_SPACING;
        
        guiGraphics.drawString(font, "Current", columnX, headerY, 0xFFFFFF);
        columnX += PRICE_WIDTH + COLUMN_SPACING;
        
        guiGraphics.drawString(font, "P/L", columnX, headerY, 0xFFFFFF);

        // Draw holdings
        int rowY = headerY + ROW_HEIGHT;
        for (Map.Entry<String, Integer> entry : holdings.entrySet()) {
            if (rowY + ROW_HEIGHT > y + height) break;
            
            String symbol = entry.getKey();
            int quantity = entry.getValue();
            Stock stock = stocks.get(symbol);
            
            if (stock != null) {
                columnX = x + 10;
                
                guiGraphics.drawString(font, symbol, columnX, rowY, 0xFFFFFF);
                columnX += SYMBOL_WIDTH + COLUMN_SPACING;
                
                guiGraphics.drawString(font, String.format("%,d", quantity), columnX, rowY, 0xFFFFFF);
                columnX += PRICE_WIDTH + COLUMN_SPACING;
                
                guiGraphics.drawString(font, PRICE_FORMAT.format(stock.getPreviousPrice()), columnX, rowY, 0xFFFFFF);
                columnX += PRICE_WIDTH + COLUMN_SPACING;
                
                double currentPrice = stock.getCurrentPrice();
                guiGraphics.drawString(font, PRICE_FORMAT.format(currentPrice), columnX, rowY, 0xFFFFFF);
                columnX += PRICE_WIDTH + COLUMN_SPACING;
                
                double profitLoss = (currentPrice - stock.getPreviousPrice()) * quantity;
                int profitColor = profitLoss >= 0 ? 0x00FF00 : 0xFF0000;
                guiGraphics.drawString(font, PRICE_FORMAT.format(profitLoss), columnX, rowY, profitColor);
            }
            
            rowY += ROW_HEIGHT;
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { // ESC key
            this.minecraft.setScreen(null);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (showTransactionPopup) {
            // Only handle popup interactions when it's visible
            int popupX = width / 2 - POPUP_WIDTH / 2;
            int popupY = height / 2 - POPUP_HEIGHT / 2;
            
            // Check if click is within popup bounds
            if (mouseX >= popupX && mouseX <= popupX + POPUP_WIDTH &&
                mouseY >= popupY && mouseY <= popupY + POPUP_HEIGHT) {
                return super.mouseClicked(mouseX, mouseY, button);
            }
            return true; // Block clicks outside popup
        }
        
        // Handle stock selection and action icons
        int marketY = 26; // Start after headers (10 + 16)
        int rowY = marketY;
        Map<String, Stock> stocks = stockMarketService.getAllStocks();
        Portfolio portfolio = stockMarketService.getPortfolio(minecraft.player);
        
        for (Stock stock : stocks.values()) {
            if (mouseY >= rowY && mouseY < rowY + ROW_HEIGHT) {
                int columnX = 10;
                
                if (mouseX >= columnX && mouseX < columnX + SCREEN_WIDTH) {
                    selectedStock = stock.getSymbol();
                    
                    // Check if clicking on action icons
                    int actionsX = columnX + SYMBOL_WIDTH + NAME_WIDTH + PRICE_WIDTH + CHANGE_WIDTH + VOLUME_WIDTH + (COLUMN_SPACING * 5);
                    
                    if (mouseX >= actionsX && mouseX < actionsX + ACTION_ICON_SIZE) {
                        // Buy icon clicked
                        showTransactionPopup("buy");
                    } else if (portfolio.getHolding(stock.getSymbol()) > 0 && 
                             mouseX >= actionsX + ACTION_ICON_SIZE + 2 && 
                             mouseX < actionsX + ACTION_ICON_SIZE * 2 + 2) {
                        // Sell icon clicked
                        showTransactionPopup("sell");
                    }
                    return true;
                }
            }
            rowY += ROW_HEIGHT;
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
} 