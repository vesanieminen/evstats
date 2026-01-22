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

export async function waitForChargingToolReady(page: Page) {
  await page.getByRole('heading', { name: 'Charging tool' }).waitFor();
  await page.getByLabel('Battery capacity').waitFor({ state: 'visible' });
}

export async function setFieldByLabel(page: Page, label: string, value: string) {
  const field = page.getByLabel(label);
  await field.click();
  await field.fill(value);
  await field.press('Enter');
  await field.blur();
}

export async function selectVaadinSelectValue(page: Page, value: string) {
  const selectButton = page.locator('vaadin-select').getByRole('button');
  await selectButton.click();
  await page.getByRole('option', { name: value }).click();
}
