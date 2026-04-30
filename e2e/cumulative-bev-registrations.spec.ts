import { test } from '@playwright/test';

test.describe('Cumulative BEV registrations per year', () => {
  test('chart renders', async ({ page }) => {
    await page.goto('/bev-registrations-by-year');
    await page.getByRole('region', { name: /Highcharts interactive chart/i }).first().waitFor();
  });
});
