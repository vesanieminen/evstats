import { test, expect } from '@playwright/test';

const chartViews: Array<{ path: string; slug: string }> = [
  { path: '/registrations', slug: 'ev-registrations' },
  { path: '/adoption', slug: 'ev-adoption' },
  { path: '/tesla-registrations', slug: 'tesla-registrations' },
  { path: '/tesla-registrations-bar', slug: 'tesla-registrations-by-year' },
];

async function waitForChartReady(page: import('@playwright/test').Page) {
  await page.getByRole('region', { name: /Highcharts interactive chart/i }).first().waitFor();
  // ChartExport.java tags the chart element with data-export-ready when its
  // onclick-equipped menu items have been wired up.
  await page.locator('vaadin-chart[data-export-ready]').first().waitFor();
}

test.describe('Chart export menu', () => {
  for (const { path, slug } of chartViews) {
    test(`${path} renders export context button`, async ({ page }) => {
      await page.goto(path);
      await waitForChartReady(page);
      await expect(page.getByRole('button', { name: 'View chart menu' }).first()).toBeVisible();
    });

    test(`${path} export menu opens with PNG and CSV items`, async ({ page }) => {
      await page.goto(path);
      await waitForChartReady(page);
      await page.getByRole('button', { name: 'View chart menu' }).first().click();
      const menu = page.locator('.highcharts-contextmenu').first();
      await expect(menu).toBeVisible();
      await expect(menu.getByText('Download PNG', { exact: true })).toBeVisible();
      await expect(menu.getByText('Download CSV', { exact: true })).toBeVisible();
    });

    test(`${path} CSV download fires with view-name filename`, async ({ page }) => {
      await page.goto(path);
      await waitForChartReady(page);
      await page.getByRole('button', { name: 'View chart menu' }).first().click();
      const downloadPromise = page.waitForEvent('download', { timeout: 10_000 });
      await page.locator('.highcharts-contextmenu').first().getByText('Download CSV', { exact: true }).click();
      const download = await downloadPromise;
      expect(download.suggestedFilename()).toMatch(new RegExp(`^${slug}.*\\.csv$`));
    });
  }
});
