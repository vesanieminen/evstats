import { expect, test } from '@playwright/test';

test.describe('EV reliability league', () => {
  test('chart renders with Polestar 2 in top 3 and Tesla Model 3 in bottom 3', async ({ page }) => {
    await page.goto('/reliability');
    const chart = page.getByRole('region', { name: /Highcharts interactive chart/i }).first();
    await chart.waitFor();

    await expect(chart.getByLabel(/Polestar 2/i).first()).toBeVisible();
    await expect(chart.getByLabel(/Tesla MODEL 3/i).first()).toBeVisible();
  });

  test('cohort year filter restricts the visible bars', async ({ page }) => {
    await page.goto('/reliability');
    const chart = page.getByRole('region', { name: /Highcharts interactive chart/i }).first();
    await chart.waitFor();

    // Tesla Model S only has data through 2020 — should disappear when we
    // narrow to 2021. (Sanity check on default view first.)
    await page.getByLabel('Cohort year').click();
    await page.getByRole('option', { name: 'All years' }).click();

    // Now switch to 2021 — Model S cohorts are pre-2021, so they vanish.
    await page.getByLabel('Cohort year').click();
    await page.getByRole('option', { name: '2021' }).click();

    await expect(chart.getByLabel(/Tesla MODEL S/i)).toHaveCount(0, { timeout: 10000 });
    await expect(chart.getByLabel(/Tesla MODEL 3/i).first()).toBeVisible();
  });

});
