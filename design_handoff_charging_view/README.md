# Handoff: Liukuri charging-view redesign

## Overview
Redesign of the Liukuri (auto.liukuri.fi) EV-charging calculator view. Goals
that drove the redesign:

- Eliminate the duplicated current/target read-outs that appeared both in the
  header stat row and again above the slider.
- Stop wasting a full row of horizontal space on the car photo. The vehicle
  identity is now a compact chip on the top-left of the hero card.
- Roll "Change vehicle" into the vehicle chip itself — tapping it opens a
  bottom-sheet picker. The previous expand-in-place layout (model dropdown +
  vehicle-image upload + URL field + reset link) is gone.
- Make the UI legible in the Polestar 2 in-car browser (1152×1536, 3:4
  portrait) by using a larger type/control scale at tablet+ widths. Mobile
  keeps the original web sizing.
- Preserve every existing piece of functionality: SoC range slider, charging
  current slider with **Advanced** disclosure (Phases / Voltage / Charging
  Loss %), schedule with calculate-end / calculate-start modes, and the
  charging-cost summary.

## About the Design Files
The files in this bundle are **design references created in HTML** —
prototypes showing intended look and behaviour, not production code to copy
directly. The task is to **recreate this design inside the existing
auto.liukuri.fi codebase** using its established patterns and libraries
(Vaadin/Lit/etc.). Treat the HTML as authoritative for layout, sizing,
typography, colour usage, and interaction; reimplement against the real
backend / state model.

## Fidelity
**High-fidelity (hifi).** Colours, typography, spacing, and slider behaviour
are intended to be matched closely. Per-vehicle accent colour is functional
(it actually drives every accent surface on the page).

## Files in this bundle

| File | Purpose |
|------|---------|
| `Charging View Redesign.html` | Host page. Renders the design inside the design canvas at four viewport sizes × light/dark. |
| `charging-core.jsx` | All shared logic: vehicle catalogue, accent ramp, theme tokens, density presets, charging math, sliders, `useChargingState` hook, `AdvancedSettings` disclosure. **Read this first.** |
| `charging-v1.jsx` | The chosen design ("A · Refined"). The `ChargingV1` component composes the hero, slider, charging-speed, schedule, and cost cards. Also defines the bottom-sheet `VehicleSheet`. |
| `design-canvas.jsx`, `tweaks-panel.jsx` | Presentation/scaffolding only — not part of the design. Skip these. |

## Screens / Views

There is one screen — the charging calculator. Sections, top to bottom:

### 1. Vehicle hero card
- **Layout:** Card. Top row is a flex row with three children: car silhouette
  (left), name + battery (centre, takes remaining space), `Change ›` chip
  (right). The whole row is a single button that opens the vehicle sheet.
- **Below the row:** three-column stats grid `current % / +km adds / target %`.
  Left and right columns are `D.h2` numerals with a `D.h3` `%` suffix. Centre
  is the same scale but coloured with the per-vehicle accent and shows the
  range delta. Each cell has a small label above (`D.label`, muted) and a
  small km / kWh sub-line below.
- **Below the stats:** the dual-thumb SoC slider (`DualSlider`, `low =
  currentPct`, `high = targetPct`, `min 0`, `max 100`).
- All numeric cells use `font-variant-numeric: tabular-nums` and
  `white-space: nowrap` so values like `+227 km` never wrap.

### 2. Charging speed card
- Header row: `Charging speed` label (left, muted, uppercase, 1px tracking)
  and the live kW value (right).
- Big amperage read-out (`{amps} A · {phases}-phase`) above a single-thumb
  slider, **min 0, max 32** (so the default 16 A sits exactly mid-track).
- Below the slider: `Advanced` disclosure. When open, three steppers in a
  flex-wrap row:
  - Phases (1..3)
  - Voltage (100..450, step 5, V)
  - Charging Loss (0..30, %)
- All three feed `calculate()` so kW, duration, and cost reflect changes
  live.

### 3. Schedule card
- Header: `Schedule` label and a small inline button on the right that flips
  `calcMode` between `'end'` (compute end from start) and `'start'` (compute
  start from end).
- Body: two `TimeField`s separated by an `→`. The active one is editable
  (steppers visible); the derived one is dimmed.
- Footer row: `Duration` label + `formatDuration(out.durationH)`.

### 4. Charging cost card
- `Charging cost` header.
- Rows: `Energy consumed`, `Added to battery`, `Lost to heat`, `Spot price`
  (in green, with a small dot indicator).
- Divider, then `Total` left + `{totalCost} €` right in success-green.

### 5. Vehicle bottom sheet (modal)
- Triggered by tapping the hero card.
- Slides up from the bottom; backdrop dismiss.
- One row per vehicle: silhouette, name, trim · battery · consumption,
  active checkmark. The active row uses that vehicle's accent ramp.

## Design tokens

### Density presets (`charging-core.jsx → DENSITY`)
Tablet, desktop, and in-car all share the **in-car** preset. Mobile is its
own thing.

| Token | mobile | incar / tablet / desktop |
|-------|-------:|--------------------------:|
| base  | 14 | 24 |
| h1    | 28 | 86 |
| h2    | 22 | 56 |
| h3    | 17 | 36 |
| label | 12 | 22 |
| pad   | 16 | 36 |
| gap   | 14 | 28 |
| radius| 12 | 18 |
| control | 40 | 88 |

### Theme tokens (`theme(dark)`)
Light: `bg #f6f6f4`, `surface #fff`, `text #171717`, `textMuted rgba(23,23,23,0.62)`,
`line rgba(15,15,15,0.08)`, `track rgba(15,15,15,0.08)`,
`successInk oklch(0.42 0.16 145)`.

Dark: `bg #0e0f12`, `surface #1a1c20`, `text #f4f4f5`,
`textMuted rgba(244,244,245,0.62)`, `line rgba(255,255,255,0.08)`,
`track rgba(255,255,255,0.10)`, `successInk oklch(0.85 0.18 145)`.

### Accent ramp (`accentRamp(accent, dark)`)
Each vehicle declares `{ h, c }` — hue and chroma in the oklch colour space.
The ramp builder produces:

- `base` — `oklch(0.55 c h)` light / `oklch(0.72 c h)` dark
- `bold` — slightly darker (light) / lighter (dark)
- `soft` — base at 14% / 18% alpha
- `ghost` — base at 6% / 8% alpha

Fall back to a single neutral ramp if you don't want per-vehicle accents.

### Vehicle catalogue (`VEHICLES`)
Each entry: `{ id, name, trim, battery (kWh), consumption (kWh/100km),
accent: { h, c }, sketch }`. The `sketch` field maps to one of five generic
silhouettes; replace with real product photos when available.

## Charging math (`calculate()`)
- `addedKwh = max(0, (targetPct - currentPct)/100) * battery`
- `power (kW) = (voltage * amps * phases) / 1000`
- `efficiency = 1 - lossPct/100`
- `energyKwh = addedKwh / efficiency`
- `lostKwh = energyKwh - addedKwh`
- `durationH = energyKwh / power`
- `rangeKm = (addedKwh / consumption) * 100`
- `totalCost = (energyKwh * spotPriceCkwh) / 100` (¢/kWh → €)

## State (`useChargingState`)
| Key | Default | Notes |
|-----|---------|-------|
| `vehicleId` | `'polestar2-lr'` | Selected vehicle id |
| `currentPct` | 29 | SoC low thumb, 0..100 |
| `targetPct` | 80 | SoC high thumb, 0..100 |
| `amps` | 16 | 0..32 |
| `phases` | 3 | 1..3 |
| `voltage` | 230 | V, advanced |
| `lossPct` | 10 | %, advanced |
| `startHour` | 9 | Hours since midnight, can exceed 24 (next-day) |
| `endHour` | 13 | Same scale |
| `calcMode` | `'end'` | `'end'` derives endHour, `'start'` derives startHour |
| `spotPrice` | 3.98 | c/kWh — wire this to your live price source |

`useEffect` re-derives the inactive end-of-schedule whenever
`out.durationH` or `calcMode` changes.

## Interactions
- **Hero card click** → open `VehicleSheet` (bottom sheet). Picking a vehicle
  closes the sheet and updates accent + battery + consumption.
- **DualSlider** — drag thumbs; clicking the track moves the closer thumb.
  `low` cannot exceed `high - 1` and vice versa.
- **SingleSlider** — drag thumb or click anywhere on the track.
- **Advanced disclosure** — chevron rotates 90° when open, content slides in
  with a 1px dashed top divider.
- **Schedule mode flip** — tapping `start time ↻` / `end time ↻` swaps which
  field is editable.
- All sliders use Pointer Events with `touch-action: none` so dragging works
  identically with mouse, finger, and the in-car capacitive screen.

## Responsive behaviour
- Pick the density preset based on viewport width:
  - `< 600px` → `mobile`
  - `≥ 600px` → `incar` (== tablet == desktop)
- The car-screen breakpoint isn't size-based on the live site (Polestar 2
  reports a desktop-ish UA), so consider exposing density as a user setting
  too.

## Assets
- No real product photos. Replace `CarSilhouette` with proper imagery when
  available; the silhouette function is purely a placeholder.
- No icons beyond a couple of glyphs (`›`, `↻`, `→`, `−`, `+`, `✕`, `✓`)
  used inline as text. Swap for your existing icon set.

## Out of scope / next steps
- Vehicle catalogue is a hand-rolled list. Wire to your real catalogue.
- Spot price is hardcoded at 3.98 c/kWh — connect to live data.
- Image upload / URL field for custom vehicle images was intentionally
  removed; reintroduce as a per-vehicle override panel inside `VehicleSheet`
  if it's still needed.
- Date pickers (start/end date from the original) are not present. Add them
  to `Schedule` if multi-day charging is in scope.
- Localisation — UI strings are English; the live app is in Finnish.
