import { expect, test } from '@playwright/test';

test.describe('Tesla reliability deep-dive', () => {
  test('chart and default panel render', async ({ page }) => {
    await page.goto('/tesla-reliability');
    await page.getByRole('region', { name: /Highcharts interactive chart/i }).first().waitFor();

    // Default selection per spec: Tesla Model 3 / 2021.
    await expect(page.getByRole('heading', { name: /Tesla MODEL 3 — 2021/i })).toBeVisible();

    // Stats labels visible.
    await expect(page.getByText(/Inspections/i).first()).toBeVisible();
    await expect(page.getByText(/Avg km/i).first()).toBeVisible();
  });

  test('top defect for Model 3 / 2021 is the rear-axle / suspension issue', async ({ page }) => {
    await page.goto('/tesla-reliability');
    await page.getByRole('region', { name: /Highcharts interactive chart/i }).first().waitFor();

    // Defect list shows EN translation followed by FI in muted text.
    // Top-3 defects for Model 3 / 2021: Taka-akselisto, Etuakselisto, Muut ikkunat.
    await expect(page.getByText(/Rear axle \/ suspension/i)).toBeVisible();
    await expect(page.getByText(/— Taka-akselisto/)).toBeVisible();
  });

  test('clicking a Model S column updates the bottom panel', async ({ page }) => {
    await page.goto('/tesla-reliability');
    await page.getByRole('region', { name: /Highcharts interactive chart/i }).first().waitFor();
    await expect(page.getByRole('heading', { name: /Tesla MODEL 3 — 2021/i })).toBeVisible();

    // Series 0 is "Tesla MODEL S". Clicking its 2014 column should swap the panel.
    await page.locator('g.highcharts-series-0 rect.highcharts-point').first().click({ force: true });
    await expect(page.getByRole('heading', { name: /Tesla MODEL S — 2014/i })).toBeVisible();
  });
});
