import { expect, test } from '@playwright/test';
import { waitForChargingToolReady } from './utils';

test.describe('Reliability', () => {
  test('refresh keeps UI stable', async ({ page }) => {
    await page.goto('/');
    await page.reload();
    await page.goto('/');
    await waitForChargingToolReady(page);
    await expect(page.getByRole('link', { name: 'Charging tool' })).toBeVisible();
  });
});
