import { test } from '@playwright/test';

test.describe('Tesla registrations by year', () => {
  test('chart renders', async ({ page }) => {
    await page.goto('/tesla-registrations-bar');
    await page.getByRole('region', { name: /Highcharts interactive chart/i }).first().waitFor();
  });
});
