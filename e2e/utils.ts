import { Page } from '@playwright/test';

export async function closeSettingsDialogIfOpen(page: Page) {
  const dialog = page.getByRole('dialog', { name: 'Settings' });
  if (await dialog.isVisible().catch(() => false)) {
    const close = dialog.getByRole('button').first();
    await close.click();
  }
  const overlay = page.locator('vaadin-dialog-overlay[opened]');
  if (await overlay.isVisible().catch(() => false)) {
    await page.keyboard.press('Escape');
  }
  await overlay.waitFor({ state: 'hidden' }).catch(() => {});
}
