import { Page } from '@playwright/test';

export async function closeSettingsDialogIfOpen(page: Page) {
  const dialog = page.getByRole('dialog', { name: 'Settings' });
  if (!(await dialog.isVisible().catch(() => false))) {
    return;
  }
  // Escape reliably closes the topmost Vaadin overlay. May need two presses
  // if a vaadin-select dropdown inside the dialog is also open.
  await page.keyboard.press('Escape');
  if (await dialog.isVisible().catch(() => false)) {
    await page.keyboard.press('Escape');
  }
  await dialog.waitFor({ state: 'hidden' }).catch(() => {});
}

export async function waitForChargingToolReady(page: Page) {
  await page.getByRole('heading', { name: 'Charging tool' }).waitFor();
  await page.getByText('Charging Summary').waitFor({ state: 'visible' });
}

export async function setFieldByLabel(page: Page, label: string, value: string) {
  const field = page.getByLabel(label);
  await field.click();
  await field.fill(value);
  await field.press('Enter');
  await field.blur();
}
