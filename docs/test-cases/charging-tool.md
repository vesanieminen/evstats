# Charging Tool Test Cases

**TC-charging-01: Default values load**
- Steps: Open `/`.
- Expected: Fields show defaults (capacity 75, current SOC 20, target SOC 50, amps 16, phases 3, voltage 230, loss 10). Calculation target is End time. Start time defaults to current hour in Europe/Helsinki.

**TC-charging-02: Calculations update on input change**
- Steps: Change Battery capacity to 60 kWh.
- Expected: Charging length, speeds, energy values update without page reload.

**TC-charging-03: Target SOC affects calculations**
- Steps: Change Target SOC to 80%.
- Expected: Charging length increases; consumed/added/lost energy update accordingly.

**TC-charging-04: Current SOC affects calculations**
- Steps: Change Current SOC to 40% (keep target at 50%).
- Expected: Charging length and energy values decrease.

**TC-charging-05: Charging power inputs affect calculations**
- Steps: Change Amperes to 10, Phases to 1, Voltage to 230.
- Expected: Charging speed decreases; length increases.

**TC-charging-06: Charging loss affects calculations**
- Steps: Change Charging loss to 20%.
- Expected: Charging speed minus loss decreases; total time increases; lost electricity increases.

**TC-charging-07: Toggle calculation target to Start time**
- Steps: Switch calculation target to Start time.
- Expected: Label for the picker becomes “Select charging end”; result field label becomes “Calculated charging start time”; calculated timestamp updates accordingly.

**TC-charging-08: Toggle calculation target to End time**
- Steps: Switch calculation target back to End time.
- Expected: Label for the picker becomes “Select charging start”; result field label becomes “Calculated charging end time”.

**TC-charging-09: Start time change updates results**
- Steps: Change start date/time.
- Expected: Calculated end time updates; cost recalculates if API is reachable.

**TC-charging-10: Date/time bounds enforced**
- Steps: Attempt to select a date outside the valid range.
- Expected: DateTimePicker disallows values outside Liukuri valid calculation range.

**TC-charging-11: Cost fields populate (API OK)**
- Steps: With valid inputs, wait for cost calculation.
- Expected: Spot average and total cost show numeric values (no errors).

**TC-charging-12: Cost fields handle API failure**
- Steps: Disable network or block `https://liukuri.fi/api/`.
- Expected: UI remains functional; cost values stay blank or unchanged without crashing.

**TC-charging-13: Field persistence (WebStorage)**
- Steps: Change several inputs, refresh the page.
- Expected: Inputs and calculation target restore from storage.
