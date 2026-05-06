import { expect, test } from '@playwright/test';

test.describe('EV defect breakdown', () => {
  test('charts render with default chips for Tesla MODEL 3 / Polestar 2 / VW ID.4', async ({ page }) => {
    await page.goto('/defect-breakdown');
    const charts = page.getByRole('region', { name: /Highcharts interactive chart/i });
    await expect(charts).toHaveCount(2);
    await charts.first().waitFor();

    // Chips are vaadin-button elements rendered with role="button". Highcharts
    // a11y also emits "Show <series>" buttons — exact: true avoids the overlap.
    await expect(page.getByRole('button', { name: 'Tesla MODEL 3', exact: true })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Polestar 2', exact: true })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Volkswagen ID.4', exact: true })).toBeVisible();
  });

  test('removing a chip drops the model from the page', async ({ page }) => {
    await page.goto('/defect-breakdown');
    await page.getByRole('region', { name: /Highcharts interactive chart/i }).first().waitFor();

    const polestarChip = page.getByRole('button', { name: 'Polestar 2', exact: true });
    await expect(polestarChip).toBeVisible();
    await polestarChip.click();
    await expect(polestarChip).toHaveCount(0);
  });

  test('picking a model from the combobox adds a chip', async ({ page }) => {
    await page.goto('/defect-breakdown');
    await page.getByRole('region', { name: /Highcharts interactive chart/i }).first().waitFor();

    const combo = page.locator('vaadin-combo-box');
    await combo.locator('input').click();
    await combo.locator('input').fill('Tesla MODEL S');
    await page.getByRole('option', { name: /Tesla MODEL S/i }).click();

    await expect(page.getByRole('button', { name: 'Tesla MODEL S', exact: true })).toBeVisible();
  });
});
