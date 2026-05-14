import { test, expect } from '@playwright/test';

test.describe('i18n', () => {
  test('Finnish: pre-seeded cookie flips the UI to Finnish', async ({ page, context }) => {
    // Seed the locale cookie so the app picks Finnish on first attach.
    await context.addCookies([
      { name: 'settings.locale', value: 'fi', url: 'http://localhost:8080/' }
    ]);
    await page.goto('/');
    // Navigation labels should be Finnish.
    await expect(page.getByRole('link', { name: 'Latauslaskuri' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Sähköistyminen' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Uudet autot' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Tesla-rekisteröinnit' })).toBeVisible();
    // Charging-view labels are Finnish too. "Lataustaso" is the Charge Level
    // card title, visible without expanding any sections.
    await expect(page.getByText('Lataustaso')).toBeVisible();
    // Footer too.
    await expect(page.getByRole('link', { name: 'Tehty Vaadinilla' })).toBeVisible();
  });

  test('Settings dialog has a language selector with English and Finnish', async ({ page }) => {
    await page.goto('/');
    await page.getByRole('button').filter({ has: page.locator('vaadin-icon') }).first();
    // Open the settings dialog via the cog button.
    await page.locator('#settings-button').click();
    const dialog = page.getByRole('dialog', { name: /Settings|Asetukset/ });
    await expect(dialog).toBeVisible();
    // Language select has both options.
    const langSelect = page.locator('#settings-language');
    await expect(langSelect).toBeVisible();
    await langSelect.click();
    await expect(page.getByRole('option', { name: 'English' })).toBeVisible();
    await expect(page.getByRole('option', { name: 'Suomi' })).toBeVisible();
  });

  test('Switching to Finnish in settings persists across reload', async ({ page, context }) => {
    await page.goto('/');
    await page.locator('#settings-button').click();
    const dialog = page.getByRole('dialog', { name: /Settings|Asetukset/ });
    await expect(dialog).toBeVisible();
    await page.locator('#settings-language').click();
    // Scope to the open overlay so we don't race against a stale, detaching
    // option from a previous interaction.
    const overlay = page.locator('vaadin-select-overlay[opened]');
    await overlay.waitFor({ state: 'visible' });
    await overlay.getByRole('option', { name: 'Suomi' }).click();
    // The server-side handler sets a `settings.locale` cookie and triggers
    // location.reload(). Wait until the cookie is present, then force a
    // reload from the test side to be deterministic about timing.
    await expect.poll(async () =>
      (await context.cookies()).some(c => c.name === 'settings.locale' && c.value === 'fi'),
      { timeout: 10_000 }
    ).toBe(true);
    await page.reload();
    await expect(page.getByRole('link', { name: 'Latauslaskuri' })).toBeVisible();
  });
});
