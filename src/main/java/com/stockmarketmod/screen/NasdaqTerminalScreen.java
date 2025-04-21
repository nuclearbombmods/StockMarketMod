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
import net.minecraft.ChatFormatting;

import java.text.DecimalFormat;
import java.util.Map;

public class NasdaqTerminalScreen extends Screen {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final DecimalFormat PRICE_FORMAT = new DecimalFormat("#,##0.00");
    private static final DecimalFormat CHANGE_FORMAT = new DecimalFormat("+#,##0.00;-#,##0.00");
    private static final int SECTION_SPACING = 20;
    private static final int ROW_HEIGHT = 15;
    private static final int HEADER_HEIGHT = 16;
    private static final int TITLE_HEIGHT = 10;
    private static final int COLUMN_SPACING = 10;
    private static final int ACTION_ICON_SIZE = 10;
    private static final int POPUP_WIDTH = 200;
    private static final int POPUP_HEIGHT = 100;
    private static final int DEPOSIT_BUTTON_WIDTH = 80; // Reduced from 100
    private static final int DEPOSIT_BUTTON_HEIGHT = 15;
    
    // Column widths
    private static final int SYMBOL_WIDTH = 60;
    private static final int NAME_WIDTH = 120;
    private static final int PRICE_WIDTH = 70;
    private static final int CHANGE_WIDTH = 70;
    private static final int VOLUME_WIDTH = 80;
    private static final int ACTIONS_WIDTH = 30;
    private static final int SCREEN_WIDTH = 
        SYMBOL_WIDTH + NAME_WIDTH + PRICE_WIDTH + CHANGE_WIDTH + VOLUME_WIDTH + ACTIONS_WIDTH + (COLUMN_SPACING * 8);
    
    private static final int VISIBLE_ROWS = 6; // Fixed number of visible rows for each section
    private int firstVisibleStockIndex = 0; // Index of first visible stock
    private int firstVisiblePortfolioIndex = 0; // Index of first visible portfolio item
    
    private final StockMarketService stockMarketService;
    private String selectedStock = null;
    private int scrollOffset = 0; // Add this field to track scroll position

    // Transaction popup state
    private boolean showTransactionPopup = false;
    private boolean showDepositPopup = false;
    private String transactionType = ""; // "buy" or "sell"
    private EditBox quantityInput;
    private EditBox depositInput;
    private Button confirmButton;
    private Button cancelButton;
    private Button depositButton;
    private String depositErrorMessage = null; // Add this field at the class level

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
        quantityInput.setTextColor(0x00FF00);
        quantityInput.setBordered(true);
        quantityInput.setFilter(text -> text.matches("\\d*")); // Only allow digits
        quantityInput.setFocused(true);

        // Initialize deposit popup components
        depositInput = new EditBox(this.font,
            width / 2 - POPUP_WIDTH / 2 + 10,
            height / 2 - POPUP_HEIGHT / 2 + 30,
            POPUP_WIDTH - 20, 20,
            Component.translatable("container.stockmarketmod.deposit_amount"));
        depositInput.setValue("1");
        depositInput.setTextColor(0x00FF00);
        depositInput.setBordered(true);
        depositInput.setFilter(text -> text.matches("\\d*")); // Only allow digits
        depositInput.setFocused(true);
        
        // Create buttons with explicit onPress handlers
        confirmButton = Button.builder(Component.literal("Confirm"), button -> {
            LOGGER.info("Confirm button clicked");
            if (showDepositPopup) {
                handleDeposit();
            } else {
                handleTransaction();
            }
        })
        .pos(width / 2 - POPUP_WIDTH / 2 + 10, 
            height / 2 - POPUP_HEIGHT / 2 + 60)
        .size(80, 20)
        .build();
            
        cancelButton = Button.builder(Component.literal("Cancel"), button -> {
            LOGGER.info("Cancel button clicked");
            hideTransactionPopup();
            hideDepositPopup();
        })
        .pos(width / 2 + POPUP_WIDTH / 2 - 90, 
            height / 2 - POPUP_HEIGHT / 2 + 60)
        .size(80, 20)
        .build();

        // Add deposit button with adjusted size and position
        depositButton = Button.builder(Component.literal("Deposit"), button -> {
            showDepositPopup();
        })
        .pos(width - DEPOSIT_BUTTON_WIDTH - 10, 5) // Moved up to 5px from top
        .size(DEPOSIT_BUTTON_WIDTH, DEPOSIT_BUTTON_HEIGHT)
        .build();
        
        addRenderableWidget(depositButton);
    }

    private void showTransactionPopup(String type) {
        LOGGER.info("Showing transaction popup for type: {}", type);
        showTransactionPopup = true;
        transactionType = type;
        quantityInput.setValue("1");
        quantityInput.setFocused(true);
        
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

    private void handleDeposit() {
        LOGGER.info("Confirm button clicked for deposit");
        try {
            int quantity = Integer.parseInt(depositInput.getValue());
            LOGGER.info("Attempting to deposit {} emeralds", quantity);
            
            // Log current inventory state
            int mainInventoryCount = 0;
            int hotbarCount = 0;
            int offhandCount = 0;
            
            // Count emeralds in main inventory
            for (int i = 0; i < minecraft.player.getInventory().getContainerSize(); i++) {
                ItemStack stack = minecraft.player.getInventory().getItem(i);
                if (stack.getItem() == Items.EMERALD) {
                    if (i < 9) { // Hotbar
                        hotbarCount += stack.getCount();
                    } else {
                        mainInventoryCount += stack.getCount();
                    }
                }
            }
            
            // Count emeralds in offhand
            ItemStack offhandStack = minecraft.player.getOffhandItem();
            if (offhandStack.getItem() == Items.EMERALD) {
                offhandCount = offhandStack.getCount();
            }
            
            LOGGER.info("Current emerald counts - Main Inventory: {}, Hotbar: {}, Offhand: {}, Total: {}", 
                mainInventoryCount, hotbarCount, offhandCount, mainInventoryCount + hotbarCount + offhandCount);
            
            if (quantity > 0) {
                boolean success = stockMarketService.depositEmeralds(minecraft.player, quantity);
                LOGGER.info("Deposit result: {}", success);
                
                if (success) {
                    // Log final inventory state
                    int finalMainInventoryCount = 0;
                    int finalHotbarCount = 0;
                    int finalOffhandCount = 0;
                    
                    for (int i = 0; i < minecraft.player.getInventory().getContainerSize(); i++) {
                        ItemStack stack = minecraft.player.getInventory().getItem(i);
                        if (stack.getItem() == Items.EMERALD) {
                            if (i < 9) {
                                finalHotbarCount += stack.getCount();
                            } else {
                                finalMainInventoryCount += stack.getCount();
                            }
                        }
                    }
                    
                    ItemStack finalOffhandStack = minecraft.player.getOffhandItem();
                    if (finalOffhandStack.getItem() == Items.EMERALD) {
                        finalOffhandCount = finalOffhandStack.getCount();
                    }
                    
                    LOGGER.info("Final emerald counts - Main Inventory: {}, Hotbar: {}, Offhand: {}, Total: {}", 
                        finalMainInventoryCount, finalHotbarCount, finalOffhandCount, 
                        finalMainInventoryCount + finalHotbarCount + finalOffhandCount);
                    
                    LOGGER.info("Emeralds removed: {}", 
                        (mainInventoryCount + hotbarCount + offhandCount) - 
                        (finalMainInventoryCount + finalHotbarCount + finalOffhandCount));
                    
                    hideDepositPopup();
                    minecraft.player.displayClientMessage(Component.literal("Successfully deposited " + quantity + " emeralds.").withStyle(ChatFormatting.GREEN), false);
                } else {
                    depositErrorMessage = "You need " + quantity + " emeralds in your inventory to deposit.";
                }
            } else {
                depositErrorMessage = "Please enter a positive number of emeralds.";
            }
        } catch (NumberFormatException e) {
            LOGGER.error("Invalid deposit quantity input: {}", depositInput.getValue());
            depositErrorMessage = "Please enter a valid number.";
        }
    }

    private void showDepositPopup() {
        LOGGER.info("Showing deposit popup");
        showDepositPopup = true;
        depositInput.setValue("1");
        depositInput.setFocused(true);
        
        // Clear existing widgets
        clearWidgets();
        
        // Add popup widgets
        addRenderableWidget(depositInput);
        addRenderableWidget(confirmButton);
        addRenderableWidget(cancelButton);
    }

    private void hideDepositPopup() {
        showDepositPopup = false;
        depositErrorMessage = null; // Clear error message when hiding popup
        clearWidgets();
        addRenderableWidget(depositButton);
    }

    private void handleRefresh() {
        stockMarketService.updateMarket(minecraft.level);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Draw the background terminal first
        renderBackground(guiGraphics);
        
        if (showTransactionPopup || showDepositPopup) {
            // Draw solid black background for the entire screen
            guiGraphics.fill(0, 0, this.width, this.height, 0xFF000000);
            
            // Draw the popup with a solid white background
            int popupX = width / 2 - POPUP_WIDTH / 2;
            int popupY = height / 2 - POPUP_HEIGHT / 2;
            
            // Draw solid white background for popup
            guiGraphics.fill(popupX, popupY, popupX + POPUP_WIDTH, popupY + POPUP_HEIGHT, 0xFFFFFFFF);
            
            if (showTransactionPopup) {
                // Draw transaction popup content with stock name
                Stock stock = stockMarketService.getStock(selectedStock);
                String title = transactionType.equals("buy") ? 
                    String.format("BUY %s", stock != null ? stock.getSymbol() : "") : 
                    String.format("SELL %s", stock != null ? stock.getSymbol() : "");
                guiGraphics.drawString(font, title, 
                    width / 2 - font.width(title) / 2,
                    popupY + 10,
                    0x000000, false);
                
                // Draw stock info in black
                if (stock != null) {
                    String stockInfo = String.format("%s - %s", stock.getSymbol(), stock.getName());
                    guiGraphics.drawString(font, stockInfo,
                        width / 2 - font.width(stockInfo) / 2,
                        popupY + 30,
                        0x000000, false);
                }
            } else {
                // Draw deposit popup content
                String title = "Deposit Emeralds";
                guiGraphics.drawString(font, title,
                    width / 2 - font.width(title) / 2,
                    popupY + 10,
                    0x000000, false);

                // Draw input box background
                int inputX = width / 2 - POPUP_WIDTH / 2 + 10;
                int inputY = height / 2 - POPUP_HEIGHT / 2 + 30;
                guiGraphics.fill(inputX - 1, inputY - 1, 
                    inputX + POPUP_WIDTH - 20 + 1, inputY + 20 + 1, 
                    0xFF808080); // Gray border
                guiGraphics.fill(inputX, inputY,
                    inputX + POPUP_WIDTH - 20, inputY + 20,
                    0xFFFFFFFF); // White background

                // Calculate and show value
                try {
                    int quantity = Integer.parseInt(depositInput.getValue());
                    double value = quantity * 100.0; // Each emerald is worth 100
                    String valueText = String.format("Value: %,.2f", value);
                    guiGraphics.drawString(font, valueText,
                        width / 2 - font.width(valueText) / 2,
                        popupY + 30,
                        0x000000, false);
                } catch (NumberFormatException e) {
                    // Ignore invalid input, will be handled in handleDeposit
                }

                // Draw error message if exists
                if (depositErrorMessage != null) {
                    guiGraphics.drawString(font, depositErrorMessage,
                        width / 2 - font.width(depositErrorMessage) / 2,
                        popupY + 60,
                        0xFF0000, false);
                }
            }
        } else {
            // Draw market section
            int marketY = TITLE_HEIGHT;
            renderMarketSection(guiGraphics, 0, marketY, width, (VISIBLE_ROWS + 1) * ROW_HEIGHT);
            
            // Draw portfolio section with extra spacing
            int portfolioY = marketY + (VISIBLE_ROWS + 1) * ROW_HEIGHT + SECTION_SPACING + 10; // Added extra 10 pixels
            renderPortfolioSection(guiGraphics, 0, portfolioY, width, (VISIBLE_ROWS + 1) * ROW_HEIGHT);
        }
        
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderMarketSection(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        // Section title
        guiGraphics.drawString(font, "MARKET OVERVIEW", x + 10, y, 0xFFFFFF);
        
        // Draw headers
        int headerY = y + HEADER_HEIGHT;
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
        
        // Draw stock rows within viewport
        int rowY = headerY + ROW_HEIGHT;
        Map<String, Stock> stocks = stockMarketService.getAllStocks();
        Portfolio portfolio = stockMarketService.getPortfolio(minecraft.player);
        
        // Get stocks in visible range
        int endIndex = Math.min(firstVisibleStockIndex + VISIBLE_ROWS, stocks.size());
        int index = 0;
        for (Stock stock : stocks.values()) {
            if (index >= firstVisibleStockIndex && index < endIndex) {
                // Highlight selected stock
                if (selectedStock != null && stock.getSymbol().equals(selectedStock)) {
                    guiGraphics.fillGradient(x, rowY - 3, x + SCREEN_WIDTH, rowY + ROW_HEIGHT - 4, 0x40FFFFFF, 0x40FFFFFF);
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
            index++;
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
        int headerY = y + HEADER_HEIGHT;
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

        // Draw holdings within viewport
        int rowY = headerY + ROW_HEIGHT;
        int endIndex = Math.min(firstVisiblePortfolioIndex + VISIBLE_ROWS, holdings.size());
        
        // Get holdings in visible range
        int index = 0;
        for (Map.Entry<String, Integer> entry : holdings.entrySet()) {
            if (index >= firstVisiblePortfolioIndex && index < endIndex) {
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
            index++;
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
        if (showTransactionPopup || showDepositPopup) {
            // Only handle popup interactions when it's visible
            int popupX = width / 2 - POPUP_WIDTH / 2;
            int popupY = height / 2 - POPUP_HEIGHT / 2;
            
            // Check if click is within popup bounds
            if (mouseX >= popupX && mouseX <= popupX + POPUP_WIDTH &&
                mouseY >= popupY && mouseY <= popupY + POPUP_HEIGHT) {
                return super.mouseClicked(mouseX, mouseY, button);
            }
            return true; // Block clicks outside popup
        } else {
            // Check if click is in market section
            int marketY = TITLE_HEIGHT;
            int headerY = marketY + HEADER_HEIGHT;
            int contentY = headerY + ROW_HEIGHT;
            
            // Only check clicks in the content area (below header)
            if ((int)mouseY >= contentY && (int)mouseY < contentY + (VISIBLE_ROWS * ROW_HEIGHT)) {
                int relativeY = (int)mouseY - contentY;
                int clickedRow = relativeY / ROW_HEIGHT;
                
                if (clickedRow >= 0 && clickedRow < VISIBLE_ROWS) {
                    int stockIndex = firstVisibleStockIndex + clickedRow;
                    
                    // Get the stock at this index
                    Map<String, Stock> stocks = stockMarketService.getAllStocks();
                    int index = 0;
                    Stock clickedStock = null;
                    
                    // Find the stock at the calculated index
                    for (Stock stock : stocks.values()) {
                        if (index == stockIndex) {
                            clickedStock = stock;
                            break;
                        }
                        index++;
                    }
                    
                    if (clickedStock != null) {
                        selectedStock = clickedStock.getSymbol();
                        
                        // Check if clicking on action icons
                        int actionsX = 10 + SYMBOL_WIDTH + NAME_WIDTH + PRICE_WIDTH + CHANGE_WIDTH + VOLUME_WIDTH + (COLUMN_SPACING * 5);
                        
                        if ((int)mouseX >= actionsX && (int)mouseX < actionsX + ACTION_ICON_SIZE) {
                            // Buy icon clicked
                            showTransactionPopup("buy");
                        } else if (stockMarketService.getPortfolio(minecraft.player).getHolding(clickedStock.getSymbol()) > 0 && 
                                 (int)mouseX >= actionsX + ACTION_ICON_SIZE + 2 && 
                                 (int)mouseX < actionsX + ACTION_ICON_SIZE * 2 + 2) {
                            // Sell icon clicked
                            showTransactionPopup("sell");
                        }
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        // Allow scrolling only when not in a popup
        if (!showTransactionPopup && !showDepositPopup) {
            int marketY = TITLE_HEIGHT;
            int portfolioY = marketY + (VISIBLE_ROWS + 1) * ROW_HEIGHT + SECTION_SPACING;
            
            // Check if mouse is in market section
            if ((int)mouseY >= marketY && (int)mouseY < marketY + (VISIBLE_ROWS + 1) * ROW_HEIGHT) {
                // Update first visible stock index
                int totalStocks = stockMarketService.getAllStocks().size();
                firstVisibleStockIndex = Math.max(0, Math.min(
                    firstVisibleStockIndex - (int)delta,
                    totalStocks - VISIBLE_ROWS
                ));
                return true;
            }
            // Check if mouse is in portfolio section
            else if ((int)mouseY >= portfolioY && (int)mouseY < portfolioY + (VISIBLE_ROWS + 1) * ROW_HEIGHT) {
                // Update first visible portfolio index
                int totalHoldings = stockMarketService.getPortfolio(minecraft.player).getAllHoldings().size();
                firstVisiblePortfolioIndex = Math.max(0, Math.min(
                    firstVisiblePortfolioIndex - (int)delta,
                    totalHoldings - VISIBLE_ROWS
                ));
                return true;
            }
        }
        return false;
    }
} 