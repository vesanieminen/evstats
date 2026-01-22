# EVStats Test Plan (Based on View Use Cases)

## Scope
Validate the user flows documented in `docs/use-cases/` for the charging tool, settings, and statistics views.
Detailed test cases live in `docs/test-cases/`.

## Environments
- Local dev mode: `./mvnw spring-boot:run`
- Browser: latest Chrome or Firefox

## Test Data
- EV registrations CSV in `src/main/resources/data/ensirekisteroinnit_kayttovoimat_jakauma.csv`
- Tesla registrations CSV in `src/main/resources/data/Tesla registrations.csv`
- Liukuri API availability for cost calculation

## Functional Tests

### Charging Tool
- Verify default values are shown on first load (capacity, SOCs, amps, phases, voltage, loss, calculation target, time).
- Update each input field and confirm calculated outputs update (length, speeds, energy values).
- Switch calculation target (Start time vs End time) and verify labels and computed time swap accordingly.
- Verify start/end date limits match Liukuri valid calculation range.
- Confirm spot average and total cost populate when Liukuri API is reachable.
- Confirm fields persist across refresh (WebStorage).

### Settings
- Open settings dialog; verify it renders General + Electricity costs sections.
- Toggle theme button; verify theme changes and button label updates.
- Enter margin and toggle VAT; verify values persist across refresh.
- With margin/VAT set, verify charging cost output changes (spot average and total cost).

### EV Adoption Curve
- Open Adoption curve view; chart renders with two series (BEV vs Other).
- Verify timeline range and zoom controls exist.
- Confirm data spans the expected date range from CSV.

### EV Registrations
- Open New cars view; column chart renders.
- Verify timeline controls exist and data spans the expected date range from CSV.

### Tesla Registrations
- Open Tesla registrations view; column chart renders.
- Verify timeline controls exist and data spans the expected date range from CSV.

### Tesla Registrations by Year
- Open New Teslas / year view.
- If Vaadin Charts license is configured, stacked bar chart renders with years on X-axis and months as series.
- If license is missing, verify the license notice is shown instead of the chart.

## Non-Functional Tests
- Performance: charts render within a reasonable time (<3s) on local dev data.
- Accessibility: verify chart screen reader summary exists and navigation is keyboard-accessible.
- Error handling: Liukuri API outage should not crash the UI; cost fields should handle nulls gracefully.

## Regression Checklist
- Navigation between all views works via side nav.
- Footer links and branding remain visible in the drawer.
- Settings dialog opens from the header in all views.

## Out of Scope
- Backend services beyond Liukuri API and CSV loading.
- Production build validation.
