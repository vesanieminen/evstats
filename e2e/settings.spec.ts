import { expect, test } from '@playwright/test';
import { closeSettingsDialogIfOpen, setFieldByLabel } from './utils';

test.describe('Settings dialog', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
    await closeSettingsDialogIfOpen(page);
  });

  test('open settings dialog', async ({ page }) => {
    await page.locator('#settings-button').click();
    await expect(page.getByRole('heading', { name: 'Settings' })).toBeVisible();
    await expect(page.getByRole('heading', { name: 'General' })).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Electricity costs' })).toBeVisible();
  });

  test('theme toggle updates theme attribute', async ({ page }) => {
    await page.locator('#settings-button').click();
    const toggle = page.getByRole('button', { name: /Switch to/ });

    const before = await page.evaluate(() => document.documentElement.getAttribute('theme'));
    await toggle.click();
    await expect
      .poll(async () => page.evaluate(() => document.documentElement.getAttribute('theme')))
      .not.toBe(before);
    await closeSettingsDialogIfOpen(page);
  });

  test('margin and VAT persist across refresh', async ({ page }) => {
    await page.locator('#settings-button').click();
    await setFieldByLabel(page, 'Margin', '0.45');
    await page.getByLabel('VAT').check();
    const storedMargin = await page.getByLabel('Margin').inputValue();
    await closeSettingsDialogIfOpen(page);

    await page.reload();
    await closeSettingsDialogIfOpen(page);
    await page.locator('#settings-button').click();

    await expect(page.getByLabel('Margin')).toHaveValue(storedMargin);
    await expect(page.getByLabel('VAT')).toBeChecked();
  });
});
