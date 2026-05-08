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

  test('Theme select switches the document theme attribute', async ({ page }) => {
    await page.locator('#settings-button').click();
    await page.getByRole('dialog', { name: 'Settings' }).waitFor();

    // Force Dark explicitly. Click the Select via the inner trigger, not the
    // host element — clicks on the host can land on the dialog overlay.
    await page.locator('#settings-theme').click({ force: true });
    await page.getByRole('option', { name: 'Dark', exact: true }).click();
    await expect
      .poll(async () => page.evaluate(() => document.documentElement.getAttribute('theme')))
      .toBe('dark');

    // Force Light explicitly.
    await page.locator('#settings-theme').click({ force: true });
    await page.getByRole('option', { name: 'Light', exact: true }).click();
    await expect
      .poll(async () => page.evaluate(() => document.documentElement.getAttribute('theme')))
      .toBe('');

    await page.keyboard.press('Escape');
  });

  test('Theme preference persists across reload', async ({ page }) => {
    await page.locator('#settings-button').click();
    await page.getByRole('dialog', { name: 'Settings' }).waitFor();
    await page.locator('#settings-theme').click({ force: true });
    await page.getByRole('option', { name: 'Dark', exact: true }).click();
    await expect
      .poll(async () => page.evaluate(() => document.documentElement.getAttribute('theme')))
      .toBe('dark');
    // Close without going through the helper — pressing Escape twice handles
    // both the (already-closed) select overlay and the settings dialog.
    await page.keyboard.press('Escape');

    await page.reload();
    await expect
      .poll(async () => page.evaluate(() => document.documentElement.getAttribute('theme')))
      .toBe('dark');
  });

  test('System preference follows simulated OS dark-mode change', async ({ page }) => {
    // Stored preference defaults to "system".
    await page.emulateMedia({ colorScheme: 'light' });
    await page.evaluate(() => localStorage.setItem('theme.preference', 'system'));
    await page.reload();
    await expect
      .poll(async () => page.evaluate(() => document.documentElement.getAttribute('theme')))
      .toBe('');

    await page.emulateMedia({ colorScheme: 'dark' });
    await expect
      .poll(async () => page.evaluate(() => document.documentElement.getAttribute('theme')))
      .toBe('dark');
  });

  test('Explicit Light preference ignores OS dark-mode change', async ({ page }) => {
    await page.emulateMedia({ colorScheme: 'light' });
    await page.evaluate(() => localStorage.setItem('theme.preference', 'light'));
    await page.reload();
    await expect
      .poll(async () => page.evaluate(() => document.documentElement.getAttribute('theme')))
      .toBe('');

    await page.emulateMedia({ colorScheme: 'dark' });
    // applyTheme runs on the OS-change event but reads the stored "light" pref
    // and keeps the document theme empty.
    await page.waitForTimeout(300);
    expect(await page.evaluate(() => document.documentElement.getAttribute('theme'))).toBe('');
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
