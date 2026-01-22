import { test } from '@playwright/test';

test.describe('Tesla registrations', () => {
  test('chart renders', async ({ page }) => {
    await page.goto('/tesla-registrations');
    await page.getByRole('region', { name: /Highcharts interactive chart/i }).first().waitFor();
  });
});
