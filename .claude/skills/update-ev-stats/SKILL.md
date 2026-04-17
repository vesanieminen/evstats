---
name: update-ev-stats
description: Use when the user wants to add new monthly Finnish EV/Tesla registration statistics (e.g. "update stats for February 2026", "add March 2026 data"). Updates the two CSV datasets and, when a new calendar year first appears, the TeslaRegistrationsBarView year range.
---

# Update monthly EV/Tesla registration stats

Adds one or more new month rows to the registration datasets. The Tesla bar view Java file only needs editing when crossing into a year that is not yet represented.

## Files touched

1. `src/main/resources/data/Tesla registrations.csv` — Tesla-specific monthly registrations.
2. `src/main/resources/data/ensirekisteroinnit_kayttovoimat_jakauma.csv` — All-powertrain monthly registrations (Traficom).
3. `src/main/java/com/vesanieminen/views/statistics/TeslaRegistrationsBarView.java` — **only** when the new data introduces a year not already in `configuration.getxAxis().setCategories(...)`. Extend the categories list and bump the `for (int year = 0; year < N; ...)` loop to match.

Do **not** touch `src/main/resources/application-dev.properties`. The Jan 2026 commit changed the port by accident — don't repeat that.

## Data sources (aut.fi — Autoalan tiedotuskeskus)

All data comes from aut.fi. Two separate publications feed two separate files.

### 1. Powertrain split (feeds `ensirekisteroinnit_kayttovoimat_jakauma.csv`)

- **Landing page:** https://aut.fi/markkinatilastot/uudet-autot/
- **Direct CSV (URL may change — the `/YYYY/MM/` path can rotate as aut.fi reuploads):** https://aut.fi/wp-content/uploads/2025/05/ensirekisteroinnit_kayttovoimat_lkm_kk.csv
- **On the landing page, locate under the heading:** *"Henkilöautojen ensirekisteröinnit käyttövoimittain, lkm kuukausittain"* and copy the `.csv` link.
- Used by the EV-adoption chart and all per-powertrain views.

### 2. Monthly Tesla-specific xlsx (feeds `Tesla registrations.csv`)

- **Landing page:** https://aut.fi/markkinatilastot/kuukausitilastot/
- **Per-month xlsx URL pattern:** `https://aut.fi/wp-content/uploads/<PUBLISH_YEAR>/<PUBLISH_MONTH>/<finnish_month_name>_<DATA_YEAR>.xlsx`
  - `PUBLISH_YEAR`/`PUBLISH_MONTH` = year and two-digit month when aut.fi published the report (typically the month **after** the data month).
  - `finnish_month_name` = lowercase Finnish month of the data (tammikuu, helmikuu, maaliskuu, huhtikuu, toukokuu, kesäkuu, heinäkuu, elokuu, syyskuu, lokakuu, marraskuu, joulukuu).
  - `DATA_YEAR` = calendar year the data covers.
- **Known-good examples:**
  - Feb 2026 data: https://aut.fi/wp-content/uploads/2026/03/helmikuu_2026.xlsx
  - Mar 2026 data: https://aut.fi/wp-content/uploads/2026/04/maaliskuu_2026.xlsx
- **Predicted upcoming URLs (verify on the landing page before relying on them):**
  - Apr 2026 → `https://aut.fi/wp-content/uploads/2026/05/huhtikuu_2026.xlsx`
  - May 2026 → `https://aut.fi/wp-content/uploads/2026/06/toukokuu_2026.xlsx`
  - Jun 2026 → `https://aut.fi/wp-content/uploads/2026/07/kesakuu_2026.xlsx` (note: aut.fi drops the `ä` diacritic in slugs — use `kesakuu`, `heinakuu`, not `kesäkuu`/`heinäkuu`)
- If the pattern misses, scrape the landing page for the current month's `.xlsx` link.

### Which file each source feeds

- The xlsx monthly reports → extract Tesla figures + BEV/cumulative totals → append to `Tesla registrations.csv`.
- The powertrain-split csv → append the new month row to `ensirekisteroinnit_kayttovoimat_jakauma.csv` (and this csv also backs the EV adoption chart).

## CSV formats

### Tesla registrations.csv

- Comma-separated, month key is `MM/YYYY`.
- File ends with a trailing `\n` after the last row. Preserve it — append new rows so the last row still ends with `\n`.
- Numeric decimals use comma (Finnish format). Quote any value containing a comma.
- Header columns: `Kuukausi, Autoja, Markkina-osuus (%), 1-0X/202X, Markkina-osuus (%), 1-0X/202X, Markkina-osuus (%), Kumulatiivinen muutos (%)`.
- Columns = month, Tesla count, Tesla market share %, cumulative Tesla YTD, cumulative Tesla share %, cumulative BEV YTD, cumulative BEV share %, cumulative change % vs prior year.
- The single-month cumulative row (e.g. `01/2026`) may legitimately have fewer fields if some totals aren't available yet — match whatever the source provides.

Example row (from Dec 2025): `12/2025,482,"8,76",2 618,"3,71",3 717,"5,12","-29,57"`

### ensirekisteroinnit_kayttovoimat_jakauma.csv

- **Semicolon-separated**, month key is `YYYYMMM` like `2026M01` (two-digit month).
- Header already contains U+FFFD replacement characters where the original ä/ö/Ä/Ö were — don't try to "fix" them, preserve the existing bytes exactly. Do not re-encode the file.
- **No** trailing newline at EOF. Insert new rows with a leading `\n` and do **not** add a final newline. Git will complain `\ No newline at end of file` in the diff — that's expected.
- Column order: `Kuukausi; Bensiini; Diesel; Sähkö (BEV); CNG; PHEV bensiini; PHEV diesel; HEV bensiini; HEV diesel; Vety; Etanoli; YHTEENSÄ`.
- Some recent rows have only BEV + total filled in (other columns empty between `;;`). That's normal — if the source only publishes BEV + total for the latest month, mirror the existing sparse pattern.
- Thousands separators in numbers appear as a **non-breaking space** in some rows (e.g. `2 505`) and plain digits in others. Copy the exact format the source uses; don't normalize.

Example dense row: `2025M9;537;203;2591;0;1090;14;1555;6;0;0;5996`
Example sparse row: `2025M12;;;2955;;;;;;;;5545`

## Java view change (only when adding a new year)

In `TeslaRegistrationsBarView.java`:

```java
for (int year = 0; year < N; ++year) {     // increment N
configuration.getxAxis().setCategories("2019", ..., "YYYY"); // append new year
```

`N` equals the count of years in the categories list. Skip this edit entirely when the new rows stay within an already-listed year.

## Verification

1. `./mvnw -q -DskipTests package` (or the Gradle/Maven command used by the repo) to confirm nothing broke — builds are quick but optional for pure data additions.
2. Open the charts locally when practical; otherwise visual verification is the user's call.
3. `git diff` the two CSVs before committing to confirm only additions, no line-ending or encoding churn.

## Commit

Follow the prior style: `Update statistics with <Month> <Year> data.` If multiple months are added in one change, list them: `Update statistics with February and March 2026 data.` Commit the CSVs (and the Java file if touched) in a single commit. Do **not** commit unrelated changes.

## Reference

Canonical example: commit `f182317` — "Update statistics with January 2026 data."
