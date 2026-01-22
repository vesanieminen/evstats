# Settings Dialog Test Cases

**TC-settings-01: Open settings dialog**
- Steps: Click the header settings button (cog icon).
- Expected: Dialog opens with General + Electricity costs sections.

**TC-settings-02: Theme toggle**
- Steps: Click theme toggle; observe icon and label.
- Expected: Theme switches; button label changes accordingly.

**TC-settings-03: Margin input persists**
- Steps: Set margin to `0.45`, refresh page.
- Expected: Margin value persists.

**TC-settings-04: VAT checkbox persists**
- Steps: Toggle VAT on, refresh page.
- Expected: VAT remains checked.

**TC-settings-05: Margin/VAT affect cost**
- Steps: Change margin and VAT; observe total cost.
- Expected: Cost recalculates based on updated settings.
