import { test } from '@playwright/test';

test.describe('EV registrations', () => {
  test('chart renders', async ({ page }) => {
    await page.goto('/registrations');
    await page.getByRole('region', { name: /Highcharts interactive chart/i }).first().waitFor();
  });
});
