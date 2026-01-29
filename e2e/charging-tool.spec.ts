import { expect, test } from '@playwright/test';

test.describe('Charging tool', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
    // Wait for the main view to be ready by checking for a visible element
    await page.getByRole('heading', { name: 'Charging tool' }).waitFor();
    await page.getByText('Charging Summary').waitFor({ state: 'visible' });
  });

  test('default values load', async ({ page }) => {
    // Verify visible default values on main view
    await expect(page.getByText('29%').first()).toBeVisible();
    await expect(page.getByText('80%').first()).toBeVisible();
    await expect(page.getByText('16 A')).toBeVisible();
    await expect(page.getByText('11.0 kW')).toBeVisible();

    // Expand Advanced section to see phases/voltage/charging loss
    await page.getByRole('button', { name: 'Advanced' }).click();
    await expect(page.getByLabel('Phases')).toHaveValue('3');
    await expect(page.getByLabel('Voltage')).toHaveValue('230');
    await expect(page.getByLabel('Charging Loss')).toHaveValue('10');

    // Verify vehicle select is visible when expanded
    await page.getByRole('button', { name: 'Change Vehicle' }).click();
    await expect(page.getByRole('button', { name: /Tesla Model 3 LR/ })).toBeVisible();
  });

  test('calculations update on input change', async ({ page }) => {
    const durationValue = page.locator('.summary-row').filter({ hasText: 'Duration' }).locator('.value');
    const initialDuration = await durationValue.textContent();

    // Expand advanced section and change voltage to affect calculations
    await page.getByRole('button', { name: 'Advanced' }).click();
    const voltageField = page.getByLabel('Voltage');
    await voltageField.click();
    await voltageField.fill('400');
    await voltageField.press('Tab');

    await expect(durationValue).not.toHaveText(initialDuration || '');
  });

  test('charging power inputs affect calculations', async ({ page }) => {
    const powerValue = page.locator('.power-value');
    const initialPower = await powerValue.textContent();

    // Expand advanced section
    await page.getByRole('button', { name: 'Advanced' }).click();

    // Change phases to 1
    const phasesField = page.getByLabel('Phases');
    await phasesField.click();
    await phasesField.fill('1');
    await phasesField.press('Tab');

    // Power should change from 11.0 kW (3 phase) to ~3.7 kW (1 phase)
    await expect(powerValue).not.toHaveText(initialPower || '');
  });

  test('charging loss affects calculations', async ({ page }) => {
    const lostToHeatValue = page.locator('.summary-row').filter({ hasText: 'Lost to heat' }).locator('.value');
    const initialValue = await lostToHeatValue.textContent();

    // Expand advanced section
    await page.getByRole('button', { name: 'Advanced' }).click();

    // Change charging loss
    const lossField = page.getByLabel('Charging Loss');
    await lossField.click();
    await lossField.fill('20');
    await lossField.press('Tab');

    await expect(lostToHeatValue).not.toHaveText(initialValue || '');
  });

  test('toggle calculation target updates read-only states', async ({ page }) => {
    // By default "Calculate End" is active, so end date/time should be read-only
    const startDatePicker = page.getByLabel('Start Date');
    const endDatePicker = page.getByLabel('End Date');

    // Start date should be editable, end date should be read-only
    await expect(startDatePicker).not.toHaveAttribute('readonly');

    // Click "Calculate Start" button
    await page.getByRole('button', { name: 'Calculate Start' }).click();

    // Now start date should be read-only and end date should be editable
    await expect(endDatePicker).not.toHaveAttribute('readonly');

    // Click "Calculate End" to switch back
    await page.getByRole('button', { name: 'Calculate End' }).click();
    await expect(startDatePicker).not.toHaveAttribute('readonly');
  });

  test('date picker bounds are set', async ({ page }) => {
    const startDatePicker = page.locator('vaadin-date-picker').first();
    const bounds = await startDatePicker.evaluate((el: any) => ({ min: el.min, max: el.max }));
    expect(bounds.min).toBeTruthy();
    expect(bounds.max).toBeTruthy();
  });

  test('cost fields render when API is available', async ({ page }) => {
    const spotValue = page.locator('.summary-row').filter({ hasText: 'Spot price' }).locator('.value');
    const totalValue = page.locator('.summary-row').filter({ hasText: 'Total Cost' }).locator('.value');

    await expect.soft(spotValue).toHaveText(/\d/);
    await expect.soft(totalValue).toHaveText(/\d/);
  });

  test('field persistence across refresh', async ({ page }) => {
    // Expand advanced section and change voltage
    await page.getByRole('button', { name: 'Advanced' }).click();
    const voltageField = page.getByLabel('Voltage');
    await voltageField.click();
    await voltageField.fill('400');
    await voltageField.press('Tab');

    // Wait for value to be saved
    await page.waitForTimeout(500);

    await page.reload();
    await page.getByRole('heading', { name: 'Charging tool' }).waitFor();

    // Expand advanced section again
    await page.getByRole('button', { name: 'Advanced' }).click();
    await expect(page.getByLabel('Voltage')).toHaveValue('400');
  });
});
