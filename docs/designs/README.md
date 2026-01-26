# EV Charging Tool Design

This folder contains design mockups for the "Auto Liukuri" EV charging calculator.

## Screenshots

- `charging-view light-theme.png` - Light theme (Frost) design
- `charging-view dark-theme.png` - Dark theme (Tesla Dark) design

## Features

The tool allows users to:

- **Vehicle Selection**: Choose from preset EV models (Tesla, BMW, Mercedes, etc.) or enter custom battery specs
- **Charge Level**: Set current and target state of charge (SOC) with a dual-range slider
- **Charging Speed**: Configure amperage (1-32A) with advanced settings for phases, voltage, and charging loss
- **Schedule**: Plan charging sessions with start/end date and time
- **Cost Calculation**: View charging duration, energy consumption, losses, and total cost based on spot price

## Themes

Four color themes are available:

| Theme | Description |
|-------|-------------|
| Tesla Dark | Blue/cyan gradient on dark gray |
| Midnight Purple | Purple/pink gradient on deep purple |
| Aurora Green | Emerald/teal gradient on dark green |
| Frost Light | Blue/indigo gradient on light background |

## Implementation Reference

See `ev-charging-tool.tsx` for the React component prototype using Tailwind CSS.
