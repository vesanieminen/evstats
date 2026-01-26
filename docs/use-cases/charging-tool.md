# Auto Liukuri - Charging Tool View Specification

## Overview

The Charging Tool is a responsive view that allows users to calculate EV charging costs, duration, and energy consumption. Users input their vehicle details, desired charge levels, charging speed, and schedule to receive a comprehensive charging summary.

---

## Technology Stack

- **Framework**: Vaadin 24
- **Language**: Java 21
- **Theming**: Vaadin Lumo theme (light/dark) using CSS custom properties
- **Layout**: Vaadin's responsive layout components (VerticalLayout, HorizontalLayout, FormLayout)

---

## View Structure

The view consists of six main sections, each contained in a styled card component:

1. Vehicle & Status Card
2. Charge Level Card
3. Charging Speed Card
4. Schedule Card
5. Charging Summary Card

---

## 1. Vehicle & Status Card

### Purpose
Displays the selected vehicle with a visual representation and key charging metrics. Allows vehicle selection via expandable detail.

### Components

#### 1.1 Vehicle Visualization
- **Component**: Custom HTML/SVG or embedded image
- **Content**: Simplified car silhouette graphic
- **Behavior**: Static visual, centered horizontally

#### 1.2 Vehicle Name
- **Component**: `Span` with muted text styling
- **Content**: Displays selected vehicle model name (e.g., "Tesla Model 3 LR")
- **Position**: Centered below car graphic

#### 1.3 Status Metrics Row
Three-column layout displaying:

| Column | Label | Value | Sub-value |
|--------|-------|-------|-----------|
| Left | "Current" | `{currentSoc}%` | `{currentRange} km` |
| Center | "Range +" | `{addedRange} km` | "added" |
| Right | "Target" | `{targetSoc}%` | `{targetRange} km` |

- **Styling**: Center column value uses accent color
- **Calculation**: `range = (capacity × soc / 100) / efficiency × 100`

#### 1.4 Vehicle Selection Toggle
- **Component**: `Button` with icon
- **Label**: "Change Vehicle"
- **Icons**: `VaadinIcon.CAR`, `VaadinIcon.CHEVRON_DOWN` / `VaadinIcon.CHEVRON_UP`
- **Behavior**: Toggles visibility of vehicle selection panel

#### 1.5 Vehicle Selection Panel (Expandable)
- **Component**: `Details` or custom collapsible panel
- **Default state**: Collapsed

##### 1.5.1 Vehicle Dropdown
- **Component**: `ComboBox<EVModel>`
- **Data**: List of predefined EV models with capacity and efficiency
- **Display format**: `"{modelName} ({capacity} kWh)"`
- **Behavior**: On selection, collapse panel and update all calculations

##### 1.5.2 Custom Vehicle Fields (Conditional)
Visible only when "Custom" is selected:

| Field | Component | Label | Type | Unit |
|-------|-----------|-------|------|------|
| Battery Capacity | `NumberField` | "Battery Capacity" | Integer | kWh |
| Consumption | `NumberField` | "Consumption" | Decimal (step 0.1) | kWh/100km |

### Data Model: EVModel

```java
public record EVModel(
    String name,
    int capacity,        // kWh
    double efficiency    // kWh/100km
) {}
```

### Predefined Vehicles

| Model | Capacity (kWh) | Efficiency (kWh/100km) |
|-------|----------------|------------------------|
| Tesla Model 3 LR | 75 | 14.5 |
| Tesla Model Y LR | 75 | 15.2 |
| Tesla Model S | 100 | 16.1 |
| Tesla Model X | 100 | 18.5 |
| BMW iX xDrive50 | 105 | 19.8 |
| Mercedes EQS 450+ | 108 | 17.5 |
| Audi e-tron GT | 93 | 19.2 |
| Porsche Taycan | 93 | 18.8 |
| Volkswagen ID.4 | 77 | 16.5 |
| Hyundai Ioniq 6 | 77 | 14.0 |
| Polestar 2 LR | 78 | 16.8 |
| Ford Mustang Mach-E | 91 | 17.2 |
| Custom | 75 (default) | 16.0 (default) |

---

## 2. Charge Level Card

### Purpose
Allows user to set current and target state of charge (SOC) using a dual-handle range slider.

### Components

#### 2.1 Section Header
- **Icon**: Battery icon (`VaadinIcon.BATTERY` or Lumo icon)
- **Label**: "Charge Level"
- **Styling**: Icon uses accent color

#### 2.2 Dual Range Slider
- **Component**: Custom component or two synchronized `Slider` components
- **Range**: 0–100%
- **Handles**: 
  - Low handle (Current SOC): Default 20%
  - High handle (Target SOC): Default 80%
- **Constraint**: Minimum 5% gap between handles
- **Visual**: Filled track between handles using accent gradient
- **Labels**: Percentage values displayed below each handle

#### 2.3 Summary Row
- **Left**: "Adding: **{energyToAdd} kWh**"
- **Right**: `{capacity} kWh battery` (muted text)
- **Calculation**: `energyToAdd = capacity × (targetSoc - currentSoc) / 100`

---

## 3. Charging Speed Card

### Purpose
Configure charging amperage with optional advanced settings for phases, voltage, and charging loss.

### Components

#### 3.1 Section Header Row
- **Left**: Icon (`VaadinIcon.BOLT`) + Label "Charging Speed"
- **Right**: Power display `{chargingPower} kW` (prominent styling)
- **Calculation**: `chargingPower = (amps × voltage × phases) / 1000`

#### 3.2 Amperage Slider
- **Component**: `Slider` or custom range input
- **Range**: 1–32 A
- **Default**: 16 A
- **Step**: 1

#### 3.3 Amperage Value Display
- **Position**: Centered below slider
- **Format**: `{amps} A`
- **Styling**: Muted text

#### 3.4 Advanced Toggle
- **Component**: `Button` or `Details` summary
- **Label**: "Advanced"
- **Icon**: `VaadinIcon.CHEVRON_DOWN` / `VaadinIcon.CHEVRON_UP`
- **Position**: Centered below amperage display

#### 3.5 Advanced Options Panel (Expandable)
- **Default state**: Collapsed
- **Layout**: 2-column grid

##### 3.5.1 Phases
- **Component**: `Select<Integer>`
- **Options**: 1, 3
- **Default**: 3
- **Label**: "Phases"

##### 3.5.2 Voltage
- **Component**: `Select<Integer>`
- **Options**: 220V, 230V, 240V, 400V
- **Default**: 230V
- **Label**: "Voltage"

##### 3.5.3 Charging Loss
- **Layout**: Horizontal - toggle button + input field + unit label
- **Components**:
  - `Button` "Auto" - toggles auto/manual mode
  - `NumberField` - value input (0–30%)
  - `Span` "%" - unit label
- **Default**: Auto mode enabled
- **Auto calculation**:
  - Power > 11 kW: 8%
  - Power > 7 kW: 10%
  - Otherwise: 12%
- **Behavior**: 
  - In Auto mode: field shows calculated value, appears dimmed
  - Typing in field switches to manual mode
  - Clicking "Auto" returns to auto mode

---

## 4. Schedule Card

### Purpose
Set charging start/end times with calculation mode selection.

### Components

#### 4.1 Section Header
- **Icon**: Calendar icon (`VaadinIcon.CALENDAR`)
- **Label**: "Schedule"

#### 4.2 Date/Time Grid
- **Layout**: 2×2 grid

| Position | Label | Component | Default |
|----------|-------|-----------|---------|
| Top-left | "Start Date" | `DatePicker` | Today |
| Top-right | "End Date" | `DatePicker` | Today |
| Bottom-left | "Start Time" | `TimePicker` | 11:00 |
| Bottom-right | "End Time" | `TimePicker` | 13:15 |

#### 4.3 Read-only Behavior
Based on calculation mode:
- **"Calculate End" mode**: End Date and End Time fields are read-only and visually dimmed (50% opacity)
- **"Calculate Start" mode**: Start Date and Start Time fields are read-only and visually dimmed

#### 4.4 Calculation Mode Buttons
- **Layout**: 2-column grid
- **Components**: Two `Button` components

| Button | Label | Behavior |
|--------|-------|----------|
| Left | "Calculate End" | Sets mode to calculate end time from start time + duration |
| Right | "Calculate Start" | Sets mode to calculate start time from end time - duration |

- **Styling**: 
  - Active: Accent gradient background, white text
  - Inactive: Muted background, muted text
- **Default**: "Calculate End" is active

---

## 5. Charging Summary Card

### Purpose
Display calculated results including duration, energy, losses, and total cost.

### Components

#### 5.1 Section Header
- **Icon**: Euro icon (`VaadinIcon.EURO` or custom)
- **Label**: "Charging Summary"

#### 5.2 Summary Rows
Each row: Label (left, muted) — Value (right)

| Label | Value | Notes |
|-------|-------|-------|
| Duration | `{hours}h {minutes}min` | Clock icon before label |
| Energy consumed | `{energyConsumed} kWh` | 2 decimal places |
| Added to battery | `{energyNeeded} kWh` | 2 decimal places |
| Lost to heat | `{lostEnergy} kWh` | Orange/warning color |
| Spot price | `{spotPrice} c/kWh` | |

#### 5.3 Total Cost Row
- **Separator**: Top border line
- **Label**: "Total Cost" (bold)
- **Value**: `{totalCost} €` (large, bold, accent gradient text)

### Calculations

```
energyNeeded = capacity × (targetSoc - currentSoc) / 100
energyConsumed = energyNeeded / (1 - chargingLoss / 100)
lostEnergy = energyConsumed - energyNeeded
effectivePower = chargingPower × (1 - chargingLoss / 100)
chargingTimeHours = energyNeeded / effectivePower
totalCost = energyConsumed × spotPrice / 100
```

---

## Theming & Styling

### CSS Custom Properties (Lumo)

Use Vaadin's Lumo CSS variables for automatic light/dark theme support:

```css
/* Backgrounds */
--lumo-base-color
--lumo-contrast-5pct   /* Card backgrounds */
--lumo-contrast-10pct  /* Input backgrounds */

/* Text */
--lumo-body-text-color
--lumo-secondary-text-color  /* Muted text */
--lumo-primary-text-color    /* Accent text */

/* Borders */
--lumo-contrast-20pct

/* Primary/Accent */
--lumo-primary-color
--lumo-primary-color-50pct

/* Spacing */
--lumo-space-s, --lumo-space-m, --lumo-space-l

/* Border radius */
--lumo-border-radius-l  /* Cards: 1rem/16px */
```

### Card Styling

```css
.charging-card {
    background: var(--lumo-contrast-5pct);
    border: 1px solid var(--lumo-contrast-10pct);
    border-radius: var(--lumo-border-radius-l);
    padding: var(--lumo-space-m);
}
```

### Accent Gradient (Custom)

```css
.accent-gradient {
    background: linear-gradient(135deg, 
        var(--lumo-primary-color), 
        var(--lumo-primary-color-50pct));
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
}
```

---

## Responsive Behavior

### Breakpoints

| Width | Behavior |
|-------|----------|
| < 480px | Single column, full-width cards, compact spacing |
| 480–768px | Single column, comfortable spacing |
| > 768px | Max-width container (448px), centered |

### Implementation

```java
@Route("charging-tool")
@CssImport("./styles/charging-tool.css")
public class ChargingToolView extends VerticalLayout {
    // Set max-width and center
    setMaxWidth("28rem"); // 448px
    setMargin(true);
    setPadding(true);
    setSpacing(true);
    getStyle().set("margin", "0 auto");
}
```

---

## State Management

### View State Fields

```java
// Vehicle
private EVModel selectedModel;
private int customCapacity = 75;
private double customEfficiency = 16.0;

// Charge levels
private int currentSoc = 20;
private int targetSoc = 80;

// Charging speed
private int chargingAmps = 16;
private int phases = 3;
private int voltage = 230;
private boolean chargingLossAuto = true;
private int chargingLossManual = 10;

// Schedule
private LocalDate startDate = LocalDate.now();
private LocalTime startTime = LocalTime.of(11, 0);
private LocalDate endDate = LocalDate.now();
private LocalTime endTime = LocalTime.of(13, 15);
private CalculationMode calcMode = CalculationMode.CALCULATE_END;

// Pricing
private double spotPrice = 19.08;
```

### Calculation Service

Consider extracting calculations to a separate service class:

```java
@Service
public class ChargingCalculationService {
    
    public ChargingResult calculate(ChargingParameters params) {
        // All calculations in one place
        // Returns immutable result record
    }
}
```

---

## Accessibility

- All form fields must have associated labels
- Use ARIA attributes for custom components (dual slider)
- Ensure sufficient color contrast in both themes
- Support keyboard navigation for all interactive elements
- Read-only fields should use `aria-readonly="true"`

---

## Future Considerations

- Spot price API integration for real-time electricity prices
- Price graph showing hourly rates for optimal charging time
- User preferences persistence (last used vehicle, default settings)
- Multiple vehicle profiles
- Charging history logging