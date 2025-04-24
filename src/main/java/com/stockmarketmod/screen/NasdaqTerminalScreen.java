package com.stockmarketmod.screen;

import com.mojang.logging.LogUtils;
import com.stockmarketmod.model.Market;
import com.stockmarketmod.model.Market.MarketDepth;
import com.stockmarketmod.model.Market.PriceLevel;
import com.stockmarketmod.model.Order;
import com.stockmarketmod.model.Portfolio;
import com.stockmarketmod.model.PortfolioItem;
import com.stockmarketmod.model.Stock;
import com.stockmarketmod.service.StockMarketService;
import com.stockmarketmod.Config;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;
import net.minecraft.ChatFormatting;
import com.stockmarketmod.model.MarketHistory;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
    private static final int DEPOSIT_BUTTON_WIDTH = 80;
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
    
    private static final int VISIBLE_ROWS = 6;
    private int firstVisibleStockIndex = 0;
    private int firstVisiblePortfolioIndex = 0;
    
    private final StockMarketService stockMarketService;
    private Stock selectedStock = null;
    private int scrollOffset = 0;
    private boolean isScrolling = false;
    private int lastMouseY = 0;

    private int leftPos;
    private int topPos;
    private int imageWidth;
    private int imageHeight;
    private Portfolio portfolio;

    // Transaction popup state
    private boolean showTransactionPopup = false;
    private boolean showDepositPopup = false;
    private String transactionType = "";
    private EditBox quantityInput;
    private EditBox depositInput;
    private Button confirmButton;
    private Button cancelButton;
    private Button depositButton;
    private String depositErrorMessage = null;

    private static final int MARKET_SECTION_HEIGHT = 100;
    private static final int PORTFOLIO_HEADER_HEIGHT = 20;
    private static final int PORTFOLIO_SECTION_HEIGHT = 200;
    private static final int SCROLL_BAR_WIDTH = 6;
    private static final int SCROLL_BAR_PADDING = 2;

    private Market market;

    public NasdaqTerminalScreen() {
        super(Component.literal("NASDAQ Terminal"));
        this.stockMarketService = StockMarketService.getInstance();
        this.market = stockMarketService.getMarket();
        this.portfolio = null;
        
        this.imageWidth = SCREEN_WIDTH;
        this.imageHeight = MARKET_SECTION_HEIGHT + PORTFOLIO_SECTION_HEIGHT + SECTION_SPACING * 3;
    }

    @Override
    public void init() {
        super.init();
        if (minecraft != null && minecraft.player != null) {
            this.portfolio = stockMarketService.getPortfolio(minecraft.player);
        }
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
        
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
        .pos(width - DEPOSIT_BUTTON_WIDTH - 10, 5) // Reverted to original position
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
        addRenderableWidget(depositButton);
    }

    private void handleTransaction() {
        if (minecraft.player == null) return;
        
        try {
            int quantity = Integer.parseInt(quantityInput.getValue());
            if (quantity <= 0) {
                LOGGER.warn("Invalid quantity: {}", quantity);
                return;
            }
            
            if (transactionType.equals("buy")) {
                stockMarketService.buyStock(minecraft.player, selectedStock.getSymbol(), quantity);
            } else if (transactionType.equals("sell")) {
                stockMarketService.sellStock(minecraft.player, selectedStock.getSymbol(), quantity);
            }
            
            // Place order in market
            Market market = stockMarketService.getMarket();
            if (market != null) {
                Order.OrderType orderType = transactionType.equals("buy") ? Order.OrderType.BUY : Order.OrderType.SELL;
                Order order = new Order(UUID.randomUUID(), minecraft.player.getUUID(), selectedStock.getSymbol(), orderType, selectedStock.getCurrentPrice(), quantity);
                market.placeOrder(order);
            }
            
            showTransactionPopup = false;
        } catch (NumberFormatException e) {
            LOGGER.warn("Invalid quantity format: {}", quantityInput.getValue());
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
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(graphics);
        
        // Draw dark background
        graphics.fill(0, 0, this.width, this.height, 0xAA000000);
        
        // Draw main container
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        graphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFF333333);
        
        // Draw title
        Component title = Component.literal("NASDAQ Terminal").withStyle(ChatFormatting.GOLD);
        graphics.drawString(this.font, title, 
            (this.width - this.font.width(title)) / 2, 
            y + 10, 
            0xFFFFFF);
        
        // Draw market section
        drawMarketSection(graphics, mouseX, mouseY);
        
        // Draw portfolio section
        drawPortfolioSection(graphics, mouseX, mouseY);
        
        // Draw popups if active
        if (showTransactionPopup) {
            drawTransactionPopup(graphics, mouseX, mouseY);
        }
        if (showDepositPopup) {
            drawDepositPopup(graphics, mouseX, mouseY);
        }
        
        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    private void drawMarketSection(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = DEPOSIT_BUTTON_HEIGHT + 10; // Position just below the deposit button with some padding
        
        // Draw section background
        graphics.fill(x, y, x + imageWidth, y + MARKET_SECTION_HEIGHT, 0xFF222222);
        
        // Draw section title
        Component sectionTitle = Component.literal("Market Overview").withStyle(ChatFormatting.YELLOW);
        graphics.drawString(font, sectionTitle, 
            x + 10, 
            y + 5, 
            0xFFFFFF);
            
        // Draw column headers
        int headerY = y + HEADER_HEIGHT;
        int currentX = x + 10;
        
        graphics.drawString(font, "Symbol", currentX, headerY, 0xFFFFFF);
        currentX += SYMBOL_WIDTH + COLUMN_SPACING;
        
        graphics.drawString(font, "Name", currentX, headerY, 0xFFFFFF);
        currentX += NAME_WIDTH + COLUMN_SPACING;
        
        graphics.drawString(font, "Price", currentX, headerY, 0xFFFFFF);
        currentX += PRICE_WIDTH + COLUMN_SPACING;
        
        graphics.drawString(font, "Change", currentX, headerY, 0xFFFFFF);
        currentX += CHANGE_WIDTH + COLUMN_SPACING;
        
        graphics.drawString(font, "Volume", currentX, headerY, 0xFFFFFF);
        
        // Draw stock rows
        int rowY = headerY + ROW_HEIGHT;
        Map<String, Stock> stocks = stockMarketService.getAllStocks();
        
        int index = 0;
        for (Stock stock : stocks.values()) {
            if (index >= firstVisibleStockIndex && index < firstVisibleStockIndex + VISIBLE_ROWS) {
                currentX = x + 10;
                
                // Draw row background (alternate colors)
                int rowColor = (index % 2 == 0) ? 0xFF333333 : 0xFF383838;
                if (stock == selectedStock) {
                    rowColor = 0xFF444499; // Highlight selected stock
                }
                graphics.fill(x + 1, rowY, x + imageWidth - 1, rowY + ROW_HEIGHT, rowColor);
                
                // Symbol
                graphics.drawString(font, stock.getSymbol(), currentX, rowY + 2, 0xFFFFFF);
                currentX += SYMBOL_WIDTH + COLUMN_SPACING;
                
                // Name
                graphics.drawString(font, stock.getName(), currentX, rowY + 2, 0xFFFFFF);
                currentX += NAME_WIDTH + COLUMN_SPACING;
                
                // Price
                String price = PRICE_FORMAT.format(stock.getCurrentPrice());
                graphics.drawString(font, price, currentX, rowY + 2, 0xFFFFFF);
                currentX += PRICE_WIDTH + COLUMN_SPACING;
                
                // Change
                double change = stock.getPriceChange();
                int changeColor = change >= 0 ? 0x00FF00 : 0xFF0000;
                String changeStr = CHANGE_FORMAT.format(change);
                graphics.drawString(font, changeStr, currentX, rowY + 2, changeColor);
                currentX += CHANGE_WIDTH + COLUMN_SPACING;
                
                // Volume
                String volume = String.format("%,d", stock.getVolume());
                graphics.drawString(font, volume, currentX, rowY + 2, 0xFFFFFF);
                
                // Action buttons
                currentX += VOLUME_WIDTH + COLUMN_SPACING;
                
                // Buy button
                if (mouseX >= currentX && mouseX < currentX + ACTION_ICON_SIZE &&
                    mouseY >= rowY && mouseY < rowY + ACTION_ICON_SIZE) {
                    graphics.fill(currentX, rowY + 2, currentX + ACTION_ICON_SIZE, rowY + 2 + ACTION_ICON_SIZE, 0xFF00FF00);
                } else {
                    graphics.fill(currentX, rowY + 2, currentX + ACTION_ICON_SIZE, rowY + 2 + ACTION_ICON_SIZE, 0xFF009900);
                }
                graphics.drawString(font, "+", currentX + 2, rowY + 3, 0xFFFFFF);
                
                // Sell button
                currentX += ACTION_ICON_SIZE + 5;
                if (mouseX >= currentX && mouseX < currentX + ACTION_ICON_SIZE &&
                    mouseY >= rowY && mouseY < rowY + ACTION_ICON_SIZE) {
                    graphics.fill(currentX, rowY + 2, currentX + ACTION_ICON_SIZE, rowY + 2 + ACTION_ICON_SIZE, 0xFFFF0000);
                } else {
                    graphics.fill(currentX, rowY + 2, currentX + ACTION_ICON_SIZE, rowY + 2 + ACTION_ICON_SIZE, 0xFF990000);
                }
                graphics.drawString(font, "-", currentX + 2, rowY + 3, 0xFFFFFF);
                
                rowY += ROW_HEIGHT;
            }
            index++;
        }
    }

    private void drawPortfolioSection(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = DEPOSIT_BUTTON_HEIGHT + 10 + MARKET_SECTION_HEIGHT + SECTION_SPACING;
        
        // Draw section background
        graphics.fill(x, y, x + imageWidth, y + PORTFOLIO_SECTION_HEIGHT, 0xFF222222);
        
        // Draw section title and balance
        Component sectionTitle = Component.literal("Portfolio").withStyle(ChatFormatting.YELLOW);
        graphics.drawString(font, sectionTitle, 
            x + 10, 
            y + 5, 
            0xFFFFFF);
            
        if (portfolio != null) {
            String balance = String.format("Balance: %s%s", Config.currencySymbol, 
                PRICE_FORMAT.format(portfolio.getBalance()));
            graphics.drawString(font, balance, 
                x + imageWidth - font.width(balance) - 10, 
                y + 5, 
                0x00FF00);
        }
        
        // Draw column headers
        int headerY = y + PORTFOLIO_HEADER_HEIGHT;
        int currentX = x + 10;
        
        graphics.drawString(font, "Symbol", currentX, headerY, 0xFFFFFF);
        currentX += SYMBOL_WIDTH + COLUMN_SPACING;
        
        graphics.drawString(font, "Quantity", currentX, headerY, 0xFFFFFF);
        currentX += PRICE_WIDTH + COLUMN_SPACING;
        
        graphics.drawString(font, "Avg Price", currentX, headerY, 0xFFFFFF);
        currentX += PRICE_WIDTH + COLUMN_SPACING;
        
        graphics.drawString(font, "Current", currentX, headerY, 0xFFFFFF);
        currentX += PRICE_WIDTH + COLUMN_SPACING;
        
        graphics.drawString(font, "P/L", currentX, headerY, 0xFFFFFF);
        
        // Draw portfolio items
        if (portfolio != null) {
            int rowY = headerY + ROW_HEIGHT;
            Map<String, Integer> holdings = portfolio.getAllHoldings();
            
            int index = 0;
            for (Map.Entry<String, Integer> entry : holdings.entrySet()) {
                if (index >= firstVisiblePortfolioIndex && 
                    index < firstVisiblePortfolioIndex + VISIBLE_ROWS && 
                    entry.getValue() > 0) {
                    
                    int quantity = entry.getValue();
                    Stock stock = stockMarketService.getStock(entry.getKey());
                    if (stock != null) {
                        currentX = x + 10;
                        
                        // Draw row background
                        int rowColor = (index % 2 == 0) ? 0xFF333333 : 0xFF383838;
                        graphics.fill(x + 1, rowY, x + imageWidth - 1, rowY + ROW_HEIGHT, rowColor);
                        
                        // Symbol
                        graphics.drawString(font, entry.getKey(), currentX, rowY + 2, 0xFFFFFF);
                        currentX += SYMBOL_WIDTH + COLUMN_SPACING;
                        
                        // Quantity
                        String quantityStr = String.format("%d", quantity);
                        graphics.drawString(font, quantityStr, currentX, rowY + 2, 0xFFFFFF);
                        currentX += PRICE_WIDTH + COLUMN_SPACING;
                        
                        // Average Price (using current price as placeholder since we don't track avg price)
                        String avgPrice = PRICE_FORMAT.format(stock.getCurrentPrice());
                        graphics.drawString(font, avgPrice, currentX, rowY + 2, 0xFFFFFF);
                        currentX += PRICE_WIDTH + COLUMN_SPACING;
                        
                        // Current Price
                        String currentPrice = PRICE_FORMAT.format(stock.getCurrentPrice());
                        graphics.drawString(font, currentPrice, currentX, rowY + 2, 0xFFFFFF);
                        currentX += PRICE_WIDTH + COLUMN_SPACING;
                        
                        // Profit/Loss (simplified since we don't track avg price)
                        double pl = 0.0; // We can't calculate P/L without avg price
                        int plColor = pl >= 0 ? 0x00FF00 : 0xFF0000;
                        String plStr = PRICE_FORMAT.format(pl);
                        graphics.drawString(font, plStr, currentX, rowY + 2, plColor);
                        
                        rowY += ROW_HEIGHT;
                    }
                }
                index++;
            }
        }
    }

    private void drawScrollBar(GuiGraphics graphics, int mouseX, int mouseY, int totalHeight, int visibleHeight) {
        int scrollBarX = leftPos + imageWidth - SCROLL_BAR_WIDTH - SCROLL_BAR_PADDING;
        int scrollBarY = topPos + 20;
        int scrollBarHeight = height - 40;
        
        // Draw scroll bar track
        graphics.fill(scrollBarX, scrollBarY, scrollBarX + SCROLL_BAR_WIDTH, scrollBarY + scrollBarHeight, 0x80000000);
        
        // Calculate thumb size and position
        int thumbHeight = (int)((float)visibleHeight / totalHeight * scrollBarHeight);
        int thumbY = scrollBarY + (int)((float)scrollOffset / (totalHeight - visibleHeight) * (scrollBarHeight - thumbHeight));
        
        // Draw scroll bar thumb
        graphics.fill(scrollBarX, thumbY, scrollBarX + SCROLL_BAR_WIDTH, thumbY + thumbHeight, 0xFFFFFFFF);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (showTransactionPopup && quantityInput.isFocused()) {
            return quantityInput.keyPressed(keyCode, scanCode, modifiers);
        }
        if (showDepositPopup && depositInput.isFocused()) {
            return depositInput.keyPressed(keyCode, scanCode, modifiers);
        }
        if (keyCode == 256) { // ESC key
            this.minecraft.setScreen(null);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (showTransactionPopup && quantityInput.isFocused()) {
            return quantityInput.charTyped(codePoint, modifiers);
        }
        if (showDepositPopup && depositInput.isFocused()) {
            return depositInput.charTyped(codePoint, modifiers);
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        // Get screen coordinates
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2 + TITLE_HEIGHT + SECTION_SPACING;
        
        // Check if click is in market section
        if (mouseY >= y + HEADER_HEIGHT && mouseY < y + MARKET_SECTION_HEIGHT) {
            int rowY = y + HEADER_HEIGHT + ROW_HEIGHT;
            Map<String, Stock> stocks = stockMarketService.getAllStocks();
            
            int index = 0;
            for (Stock stock : stocks.values()) {
                if (index >= firstVisibleStockIndex && index < firstVisibleStockIndex + VISIBLE_ROWS) {
                    if (mouseY >= rowY && mouseY < rowY + ROW_HEIGHT) {
                        // Calculate button positions
                        int buttonX = x + imageWidth - ACTIONS_WIDTH - COLUMN_SPACING;
                        
                        // Buy button
                        if (mouseX >= buttonX && mouseX < buttonX + ACTION_ICON_SIZE &&
                            mouseY >= rowY + 2 && mouseY < rowY + 2 + ACTION_ICON_SIZE) {
                            selectedStock = stock;
                            showTransactionPopup("buy");
                            return true;
                        }
                        
                        // Sell button
                        buttonX += ACTION_ICON_SIZE + 5;
                        if (mouseX >= buttonX && mouseX < buttonX + ACTION_ICON_SIZE &&
                            mouseY >= rowY + 2 && mouseY < rowY + 2 + ACTION_ICON_SIZE) {
                            selectedStock = stock;
                            showTransactionPopup("sell");
                            return true;
                        }
                        
                        // Select stock for viewing details
                        selectedStock = stock;
                        return true;
                    }
                    rowY += ROW_HEIGHT;
                }
                index++;
            }
        }
        
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            isScrolling = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isScrolling) {
            // Calculate total content height
            int totalContentHeight = MARKET_SECTION_HEIGHT;
            totalContentHeight += PORTFOLIO_SECTION_HEIGHT;
            
            // Calculate visible area
            int visibleHeight = height - 40;
            
            // Calculate scroll amount based on mouse movement
            int deltaY = (int)mouseY - lastMouseY;
            int scrollAmount = (int)((float)deltaY / (height - 40) * (totalContentHeight - visibleHeight));
            
            // Update scroll offset
            scrollOffset = Math.max(0, Math.min(scrollOffset + scrollAmount, Math.max(0, totalContentHeight - visibleHeight)));
            
            lastMouseY = (int)mouseY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        // Calculate total content height
        int totalContentHeight = MARKET_SECTION_HEIGHT;
        totalContentHeight += PORTFOLIO_SECTION_HEIGHT;
        
        // Calculate visible area
        int visibleHeight = height - 40;
        
        // Update scroll offset (20 pixels per scroll step)
        int scrollStep = 20;
        int scrollAmount = (int)(delta * scrollStep);
        scrollOffset = Math.max(0, Math.min(scrollOffset + scrollAmount, Math.max(0, totalContentHeight - visibleHeight)));
        return true;
    }

    private void drawHeader(GuiGraphics graphics) {
        graphics.drawString(font, "NASDAQ Terminal", leftPos + 10, topPos + 10, 0xFFFFFF);
    }

    private void drawFooter(GuiGraphics graphics) {
        String balance = String.format("Balance: %.2f", portfolio.getBalance());
        graphics.drawString(font, balance, leftPos + 10, topPos + height - 20, 0xFFFFFF);
    }

    private void drawTransactionPopup(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = (width - POPUP_WIDTH) / 2;
        int y = (height - POPUP_HEIGHT) / 2;
        
        // Draw popup background
        graphics.fill(x, y, x + POPUP_WIDTH, y + POPUP_HEIGHT, 0xFF222222);
        graphics.fill(x + 1, y + 1, x + POPUP_WIDTH - 1, y + POPUP_HEIGHT - 1, 0xFF444444);
        
        // Draw title
        String title = transactionType.equals("buy") ? "Buy Stock" : "Sell Stock";
        graphics.drawString(font, title, 
            x + (POPUP_WIDTH - font.width(title)) / 2, 
            y + 10, 
            0xFFFFFF);
            
        // Draw quantity label
        graphics.drawString(font, "Quantity:", 
            x + 10, 
            y + 30, 
            0xFFFFFF);
    }

    private void drawDepositPopup(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = (width - POPUP_WIDTH) / 2;
        int y = (height - POPUP_HEIGHT) / 2;
        
        // Draw popup background
        graphics.fill(x, y, x + POPUP_WIDTH, y + POPUP_HEIGHT, 0xFF222222);
        graphics.fill(x + 1, y + 1, x + POPUP_WIDTH - 1, y + POPUP_HEIGHT - 1, 0xFF444444);
        
        // Draw title
        String title = "Deposit Emeralds";
        graphics.drawString(font, title, 
            x + (POPUP_WIDTH - font.width(title)) / 2, 
            y + 10, 
            0xFFFFFF);
            
        // Draw amount label
        graphics.drawString(font, "Amount:", 
            x + 10, 
            y + 30, 
            0xFFFFFF);
            
        // Draw error message if any
        if (depositErrorMessage != null) {
            graphics.drawString(font, depositErrorMessage, 
                x + 10, 
                y + POPUP_HEIGHT - 30, 
                0xFF0000);
        }
    }
} 