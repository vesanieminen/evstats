import { expect, test } from '@playwright/test';
import { setFieldByLabel, selectVaadinSelectValue, waitForChargingToolReady } from './utils';

test.describe('Charging tool', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
    await waitForChargingToolReady(page);
  });

  test('default values load', async ({ page }) => {
    await expect(page.getByLabel('Battery capacity')).toHaveValue('75');
    await expect(page.getByLabel('Current SOC')).toHaveValue('20');
    await expect(page.getByLabel('Target SOC')).toHaveValue('50');
    await expect(page.getByLabel('Charging speed')).toHaveValue('16');
    await expect(page.getByLabel('Phases')).toHaveValue('3');
    await expect(page.getByLabel('Voltage')).toHaveValue('230');
    await expect(page.getByLabel('Charging loss')).toHaveValue('10');
    await expect(page.locator('vaadin-select')).toBeVisible();
  });

  test('calculations update on input change', async ({ page }) => {
    const chargingLengthLabel = page.getByText('Charging length:');
    const chargingLengthValue = chargingLengthLabel.locator('xpath=following-sibling::*[1]');
    const initialLength = await chargingLengthValue.textContent();

    await setFieldByLabel(page, 'Battery capacity', '60');
    await expect(chargingLengthValue).not.toHaveText(initialLength || '');
  });

  test('target and current SOC affect calculations', async ({ page }) => {
    const chargingLengthValue = page.getByText('Charging length:').locator('xpath=following-sibling::*[1]');
    const initialLength = await chargingLengthValue.textContent();

    await setFieldByLabel(page, 'Target SOC', '80');
    await expect.poll(async () => chargingLengthValue.textContent()).not.toBe(initialLength);
    const increasedLength = await chargingLengthValue.textContent();

    await setFieldByLabel(page, 'Current SOC', '40');
    await expect.poll(async () => chargingLengthValue.textContent()).not.toBe(increasedLength);
  });

  test('charging power inputs affect calculations', async ({ page }) => {
    const speedValue = page.getByText('Charging speed:').locator('xpath=following-sibling::*[1]');
    const initialSpeed = await speedValue.textContent();

    await setFieldByLabel(page, 'Charging speed', '10');
    await setFieldByLabel(page, 'Phases', '1');
    await setFieldByLabel(page, 'Voltage', '230');

    await expect(speedValue).not.toHaveText(initialSpeed || '');
  });

  test('charging loss affects calculations', async ({ page }) => {
    const minusLossValue = page
      .getByText('Charging speed minus loss:')
      .locator('xpath=following-sibling::*[1]');
    const initialValue = await minusLossValue.textContent();

    await setFieldByLabel(page, 'Charging loss', '20');
    await expect(minusLossValue).not.toHaveText(initialValue || '');
  });

  test('toggle calculation target updates labels', async ({ page }) => {
    await selectVaadinSelectValue(page, 'Start time');
    await expect(page.getByText('Select charging end')).toBeVisible();
    await expect(page.getByText('Calculated charging start time')).toBeVisible();

    await selectVaadinSelectValue(page, 'End time');
    await expect(page.getByText('Select charging start')).toBeVisible();
    await expect(page.getByText('Calculated charging end time')).toBeVisible();
  });

  test('date time picker bounds are set', async ({ page }) => {
    const picker = page.locator('vaadin-date-time-picker').first();
    const bounds = await picker.evaluate((el: any) => ({ min: el.min, max: el.max }));
    expect(bounds.min).toBeTruthy();
    expect(bounds.max).toBeTruthy();
  });

  test('cost fields render when API is available', async ({ page }) => {
    const spotValue = page.getByText('Spot average (inc. margin):').locator('xpath=following-sibling::*[1]');
    const totalValue = page.getByText('Total cost:').locator('xpath=following-sibling::*[1]');

    await expect.soft(spotValue).toHaveText(/\d/);
    await expect.soft(totalValue).toHaveText(/\d/);
  });

  test('field persistence across refresh', async ({ page }) => {
    await setFieldByLabel(page, 'Battery capacity', '66');
    await page.reload();
    await expect(page.getByLabel('Battery capacity')).toHaveValue('66');
  });
});
