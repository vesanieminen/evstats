import { test } from '@playwright/test';

test.describe('EV adoption curve', () => {
  test('chart renders', async ({ page }) => {
    await page.goto('/adoption');
    await page.getByRole('region', { name: /Highcharts interactive chart/i }).first().waitFor();
  });
});
