# Use Case: EV Adoption Curve

## Goal
Visualize BEV share versus all other powertrains over time.

## Actors
- Analyst or enthusiast

## Preconditions
- Application is running.
- EV registration dataset is available in app resources.

## Steps
1. Open the Adoption curve view from the Statistics navigation group.
2. Inspect the BEV and Other (incl. PHEV etc.) lines.
3. Use the timeline range selector and zoom controls to focus on a date range.

## Result
- A timeline line chart shows BEV share and non-BEV share as percentages over time.

## Data Sources and Dependencies
- CSV data loaded from application resources (EV registrations by month).
- Vaadin Charts for the interactive timeline.
