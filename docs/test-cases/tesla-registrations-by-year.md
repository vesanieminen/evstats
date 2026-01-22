# Tesla Registrations by Year Test Cases

**TC-tesla-year-01: Chart renders with license**
- Steps: Open `/tesla-registrations-bar` with a valid Vaadin Charts license.
- Expected: Stacked bar chart renders with years as categories and months as series.

**TC-tesla-year-02: License notice without license**
- Steps: Open `/tesla-registrations-bar` without a license.
- Expected: License notice is shown instead of the chart.
