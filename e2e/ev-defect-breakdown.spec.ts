import { expect, test } from '@playwright/test';

test.describe('EV defect breakdown', () => {
  test('charts render with both All cars and Selected bars', async ({ page }) => {
    await page.goto('/defect-breakdown');
    const charts = page.getByRole('region', { name: /Highcharts interactive chart/i });
    await expect(charts).toHaveCount(2);
    await charts.first().waitFor();
  });

  test('default selection is the curated BEV allow-list', async ({ page }) => {
    await page.goto('/defect-breakdown');
    await page.getByRole('region', { name: /Highcharts interactive chart/i }).first().waitFor();

    // Chart legend reflects the selection count via the "Selected (N)" label.
    // Default is the curated BEV allow-list (currently 20 models).
    await expect(page.getByText(/Selected \(\d+\)/).first()).toBeVisible();
    await expect(page.getByText('Selected (20)').first()).toBeVisible();
  });

  test('clear button empties the selection', async ({ page }) => {
    await page.goto('/defect-breakdown');
    await page.getByRole('region', { name: /Highcharts interactive chart/i }).first().waitFor();
    await expect(page.getByText('Selected (20)').first()).toBeVisible();

    // Hover over the picker to reveal the clear button (Vaadin shows it on
    // hover/focus when clearButtonVisible is true), then click it.
    const picker = page.locator('vaadin-multi-select-combo-box');
    await picker.hover();
    await picker.locator('[part="clear-button"]').click({ force: true });

    await expect(page.getByText('Selected (0)').first()).toBeVisible();
  });
});
