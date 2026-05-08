import { expect, Page, test } from '@playwright/test';

/**
 * Helpers
 */
async function ensurePickerOpen(page: Page) {
  await page.getByRole('heading', { name: 'Charging tool' }).waitFor();
  // "Change Vehicle" is a toggle. Only click if the picker is currently hidden.
  const select = page.locator('#vehicleSelect');
  if (!(await select.isVisible().catch(() => false))) {
    await page.getByRole('button', { name: 'Change Vehicle' }).click();
    await select.waitFor({ state: 'visible' });
  }
}

async function pickVehicle(page: Page, label: string | RegExp, expectedClass: string) {
  await ensurePickerOpen(page);
  await page.locator('#vehicleSelect').click();
  // Items in the rendered field are also <vaadin-select-item>; restrict to the
  // open overlay's options via role="option".
  await page.locator('vaadin-select-item[role="option"]').filter({ hasText: label }).first().click();
  // The brand-* class is applied via a server roundtrip; wait for the new
  // class specifically (not just any brand-*) so successive picks don't race.
  await page.locator(`main.${expectedClass}`).waitFor();
}

/** Read the computed --lumo-primary-color of <main> as an `rgb(a, b, c)` triple. */
async function primaryColor(page: Page): Promise<string> {
  await page.locator('main').first().waitFor();
  return await page.evaluate(() => {
    const main = document.querySelector('main') as HTMLElement;
    return getComputedStyle(main).getPropertyValue('--lumo-primary-color').trim();
  });
}

/** Convert "#3E6AE1" or "rgb(62, 106, 225)" to a normalised "rgb(R, G, B)" string. */
function normalise(value: string): string {
  if (value.startsWith('#')) {
    const hex = value.slice(1);
    const r = parseInt(hex.slice(0, 2), 16);
    const g = parseInt(hex.slice(2, 4), 16);
    const b = parseInt(hex.slice(4, 6), 16);
    return `rgb(${r}, ${g}, ${b})`;
  }
  return value.replace(/\s+/g, ' ').trim();
}

test.describe('Charging-page brand theme', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
  });

  test('Tesla preset paints the primary token Electric Blue', async ({ page }) => {
    await pickVehicle(page, /Tesla Model 3 LR/i, 'brand-tesla');
    expect(normalise(await primaryColor(page))).toBe(normalise('#3E6AE1'));
  });

  test('BMW preset paints the primary token BMW Blue', async ({ page }) => {
    await pickVehicle(page, /BMW iX xDrive50/i, 'brand-bmw');
    expect(normalise(await primaryColor(page))).toBe(normalise('#1C69D4'));
  });

  test('Polestar preset paints the primary token Safety Orange', async ({ page }) => {
    await pickVehicle(page, /Polestar 2 LR/i, 'brand-polestar');
    expect(normalise(await primaryColor(page))).toBe(normalise('#E07020'));
  });

  test('Custom resets to the project-default primary token', async ({ page }) => {
    // Read the default by visiting another view first.
    await page.goto('/registrations');
    const defaultPrimary = normalise(await primaryColor(page));

    await page.goto('/');
    await pickVehicle(page, 'Custom', 'brand-default');
    expect(normalise(await primaryColor(page))).toBe(defaultPrimary);
  });

  test('charging view tags <main> with the brand class', async ({ page }) => {
    await pickVehicle(page, /Tesla Model 3 LR/i, 'brand-tesla');
    await expect(page.locator('main.brand-tesla')).toBeVisible();
    await pickVehicle(page, /BMW iX xDrive50/i, 'brand-bmw');
    await expect(page.locator('main.brand-bmw')).toBeVisible();
    await expect(page.locator('main.brand-tesla')).toHaveCount(0);
  });

  test('cross-view isolation: registrations view stays on default theme', async ({ page }) => {
    await pickVehicle(page, /Tesla Model 3 LR/i, 'brand-tesla');
    expect(normalise(await primaryColor(page))).toBe(normalise('#3E6AE1'));

    await page.goto('/registrations');
    const otherPrimary = normalise(await primaryColor(page));
    expect(otherPrimary).not.toBe(normalise('#3E6AE1'));
  });
});
