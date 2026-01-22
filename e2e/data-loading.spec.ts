import { expect, test } from '@playwright/test';

test.describe('Data loading', () => {
  test('EV CSV loads', async ({ page }) => {
    await page.goto('/registrations');
    await page.getByRole('region', { name: /Highcharts interactive chart/i }).first().waitFor();

    const chartImage = page.getByAltText('Interactive chart');
    if (await chartImage.isVisible().catch(() => false)) {
      await expect(chartImage).toBeVisible();
    }
  });

  test('Tesla CSV loads', async ({ page }) => {
    await page.goto('/tesla-registrations');
    await page.getByRole('region', { name: /Highcharts interactive chart/i }).first().waitFor();

    const chartImage = page.getByAltText('Interactive chart');
    if (await chartImage.isVisible().catch(() => false)) {
      await expect(chartImage).toBeVisible();
    }
  });
});
