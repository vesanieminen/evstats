# Use Case: Settings and Preferences

## Goal
Adjust UI theme and electricity cost settings used by the charging calculator.

## Actors
- User

## Preconditions
- Application is running.

## Steps
1. Open the settings dialog from the header button (cog icon).
2. Toggle the theme between dark and light mode.
3. Enter a spot price margin (cents per kWh).
4. Enable or disable VAT inclusion.

## Result
- Theme preference is applied immediately.
- Margin and VAT settings are persisted and used in charging cost calculations.

## Data Sources and Dependencies
- Local storage (WebStorage) for persistence.
- Charging tool uses these settings when requesting cost calculation.
