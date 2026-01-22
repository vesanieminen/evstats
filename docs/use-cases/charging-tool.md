# Use Case: Charging Session Time and Cost Estimation

## Goal
Estimate charging duration, energy use, and electricity cost for an EV charging session.

## Actors
- EV owner or planner

## Preconditions
- Application is running.
- Liukuri API is reachable for cost calculation and valid time range.

## Steps
1. Open the Charging tool view (root route).
2. Enter battery capacity (kWh).
3. Enter current and target SOC (%).
4. Enter charging parameters: amperes, phases, voltage, and charging loss (%).
5. Choose calculation target (Start time or End time).
6. Pick the start or end time within the allowed time range.
7. Review the calculated results.

## Result
- The UI shows charging length, charging speed, charging speed minus loss, consumed electricity, added energy, lost energy, and a calculated start or end time.
- The UI shows spot price average (with margin) and total cost, if the cost calculation succeeds.

## Data Sources and Dependencies
- Liukuri API: returns valid calculation range and spot price cost calculation.
- Local storage (WebStorage): persists inputs between sessions.
- Settings: margin and VAT influence the cost calculation.

## Notes
- Calculations use the Europe/Helsinki timezone and 1-second time steps.
- The view supports route aliases: /, /lataus, and /charging.
