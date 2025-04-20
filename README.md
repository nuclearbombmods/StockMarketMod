# Stock Market Mod

A Minecraft mod that adds a fully functional stock market system to the game, allowing players to trade stocks using emeralds as currency.

## Features

- **Nasdaq Terminal Block**: A functional terminal that provides real-time stock market information
- **Live Trading**: Buy and sell stocks using emeralds
- **Market Overview**: View all available stocks with their current prices, changes, and trading volumes
- **Portfolio Management**: Track your stock holdings and overall wealth
- **Clean UI**: Modern interface with popup dialogs for transactions

## Requirements

- Minecraft 1.20.1
- Forge 47.4.0
- Java 17

## Installation

1. Install Minecraft Forge 47.4.0 for Minecraft 1.20.1
2. Download the latest release of the Stock Market Mod
3. Place the mod file in your Minecraft mods folder
4. Launch Minecraft with the Forge profile

## Usage

1. Craft the Nasdaq Terminal block (recipe to be added)
2. Place the terminal block in your world
3. Right-click the terminal to open the trading interface
4. View market information in the top section
5. View your portfolio in the bottom section
6. Click the emerald icon to buy stocks
7. Click the redstone icon to sell stocks (only visible for owned stocks)

## Configuration

The mod can be configured through the `config/stockmarketmod-common.toml` file:

- `enableStockMarket`: Enable/disable the stock market feature
- `initialBalance`: Set the initial balance for new players
- `currencySymbol`: Customize the currency symbol

## Development

### Building from Source

1. Clone the repository
```bash
git clone https://github.com/nuclearbombmods/StockMarketMod.git
```

2. Setup the development environment
```bash
./gradlew genEclipseRuns # For Eclipse
./gradlew genIntellijRuns # For IntelliJ IDEA
```

3. Build the mod
```bash
./gradlew build
```

The built jar file will be in `build/libs/`.

### Project Structure

- `src/main/java/com/stockmarketmod/`
  - `block/`: Block definitions
  - `item/`: Item definitions
  - `model/`: Data models for stocks and portfolio
  - `screen/`: GUI screens and menus
  - `service/`: Business logic for the stock market
  - `sound/`: Sound effects

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the GNU Lesser General Public License v2.1 (LGPL 2.1) - see the [LICENSE.txt](LICENSE.txt) file for details. 