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
  // The brand-* class is applied to <html> via a server roundtrip; wait for the
  // new class specifically (not just any brand-*) so successive picks don't race.
  await page.locator(`html.${expectedClass}`).waitFor();
}

/** Read the computed --lumo-primary-color of <html> as an `rgb(a, b, c)` triple. */
async function primaryColor(page: Page): Promise<string> {
  await page.locator('main').first().waitFor();
  return await page.evaluate(() => {
    return getComputedStyle(document.documentElement).getPropertyValue('--lumo-primary-color').trim();
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

  test('Polestar preset scales <html> font-size to 22px (rem-based --cv-* tokens scale with it)', async ({ page }) => {
    // Read the default html font-size before switching brands.
    const defaultSize = await page.evaluate(() => getComputedStyle(document.documentElement).fontSize);

    await pickVehicle(page, /Polestar 2 LR/i, 'brand-polestar');
    const polestarSize = await page.evaluate(() => getComputedStyle(document.documentElement).fontSize);
    expect(polestarSize).toBe('22px');

    // Sanity: --cv-base now resolves to a larger pixel value than under the default brand
    // (the token is in rem; html font-size is what scales it).
    const cvBasePx = await page.evaluate(() => {
      const container = document.querySelector('.charging-view-container') as HTMLElement;
      return container ? getComputedStyle(container).fontSize : null;
    });
    expect(cvBasePx).not.toBe(defaultSize);
  });

  test('Custom resolves to bare Lumo tokens (brand-default is a no-op)', async ({ page }) => {
    // Capture Lumo's real default by stripping any brand class first — the
    // cold-load default is now brand-tesla (set by the inline boot script),
    // so we can't read "default primary" by just visiting another view.
    await page.evaluate(() => {
      Array.from(document.documentElement.classList).forEach(c => {
        if (c.startsWith('brand-')) document.documentElement.classList.remove(c);
      });
    });
    const lumoDefault = normalise(await primaryColor(page));

    await pickVehicle(page, 'Custom', 'brand-default');
    expect(normalise(await primaryColor(page))).toBe(lumoDefault);
  });

  test('charging view tags <html> with the brand class', async ({ page }) => {
    await pickVehicle(page, /Tesla Model 3 LR/i, 'brand-tesla');
    await expect(page.locator('html.brand-tesla')).toHaveCount(1);
    await pickVehicle(page, /BMW iX xDrive50/i, 'brand-bmw');
    await expect(page.locator('html.brand-bmw')).toHaveCount(1);
    await expect(page.locator('html.brand-tesla')).toHaveCount(0);
  });

  test('app-wide takeover: registrations view inherits the brand theme', async ({ page }) => {
    await pickVehicle(page, /Tesla Model 3 LR/i, 'brand-tesla');
    expect(normalise(await primaryColor(page))).toBe(normalise('#3E6AE1'));

    await page.goto('/registrations');
    expect(normalise(await primaryColor(page))).toBe(normalise('#3E6AE1'));
    await expect(page.locator('html.brand-tesla')).toHaveCount(1);
  });

  test('nav-back persists the user vehicle (no default-vehicle flash)', async ({ page }) => {
    await pickVehicle(page, /Polestar 2 LR/i, 'brand-polestar');
    await page.goto('/registrations');
    await page.goto('/');
    // The session-scoped PreservedState seeds selectedModel before the view
    // renders, so the vehicle name span shouldn't flash through "Tesla Model 3 LR".
    await expect(page.locator('.vehicle-name')).toHaveText('Polestar 2 LR');
    await expect(page.locator('html.brand-polestar')).toHaveCount(1);
  });
});

test.describe('Inline boot script (no theme/brand flash on reload)', () => {
  test('preseeded localStorage paints dark + Polestar before any module loads', async ({ page }) => {
    // addInitScript runs in the page context before any inline or external
    // script. By the time the boot script in <head> executes, theme.brand and
    // theme.preference are already set, so <html> ends up dark + brand-polestar
    // synchronously — no flash window.
    await page.addInitScript(() => {
      localStorage.setItem('theme.brand', 'brand-polestar');
      localStorage.setItem('theme.preference', 'dark');
    });
    await page.goto('/');

    // Both attributes should be present on <html> from the very first DOM
    // observation. We don't assert "before paint" directly (Playwright observes
    // post-paint), but a missing class here would mean the boot script didn't
    // run synchronously.
    await expect(page.locator('html.brand-polestar')).toHaveCount(1);
    expect(await page.evaluate(() => document.documentElement.getAttribute('theme'))).toBe('dark');
  });

  test('empty localStorage falls back to brand-tesla default', async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.removeItem('theme.brand');
      localStorage.removeItem('theme.preference');
    });
    await page.goto('/');
    await expect(page.locator('html.brand-tesla')).toHaveCount(1);
  });
});

/**
 * Polestar dark-mode tokens (issue #24). Verifies that the surface hierarchy
 * from juuso-vaadin's reference palette actually paints — not just the accent.
 */
async function darken(page: Page) {
  await page.locator('#settings-button').click();
  await page.getByRole('dialog', { name: 'Settings' }).waitFor();
  await page.locator('#settings-theme').click({ force: true });
  await page.getByRole('option', { name: 'Dark', exact: true }).click();
  await page.keyboard.press('Escape');
}

async function bgOf(page: Page, selector: string): Promise<string> {
  await page.locator(selector).first().waitFor();
  return await page.evaluate((sel) => {
    const el = document.querySelector(sel) as HTMLElement;
    return getComputedStyle(el).backgroundColor;
  }, selector);
}

async function colorVar(page: Page, varName: string, selector = 'main'): Promise<string> {
  await page.locator(selector).first().waitFor();
  return await page.evaluate(
    ([sel, v]) => {
      const el = document.querySelector(sel) as HTMLElement;
      return getComputedStyle(el).getPropertyValue(v).trim();
    },
    [selector, varName]
  );
}

test.describe('Charging-page Polestar dark palette (#24)', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
  });

  test('html background paints Polestar #141414', async ({ page }) => {
    await darken(page);
    await pickVehicle(page, /Polestar 2 LR/i, 'brand-polestar');
    expect(normalise(await bgOf(page, 'html.brand-polestar'))).toBe(normalise('#141414'));
  });

  test('charging-card surface paints #1E1E1E', async ({ page }) => {
    await darken(page);
    await pickVehicle(page, /Polestar 2 LR/i, 'brand-polestar');
    expect(normalise(await bgOf(page, 'html.brand-polestar .charging-card'))).toBe(normalise('#1E1E1E'));
  });

  test('input-fill contrast token reads #282828', async ({ page }) => {
    await darken(page);
    await pickVehicle(page, /Polestar 2 LR/i, 'brand-polestar');
    expect(normalise(await colorVar(page, '--lumo-contrast-10pct', 'html.brand-polestar')))
      .toBe(normalise('#282828'));
  });

  test('secondary-text token reads #777777', async ({ page }) => {
    await darken(page);
    await pickVehicle(page, /Polestar 2 LR/i, 'brand-polestar');
    expect(normalise(await colorVar(page, '--lumo-secondary-text-color', 'html.brand-polestar')))
      .toBe(normalise('#777777'));
  });

  test('switching away from Polestar reverts surface to Lumo dark default', async ({ page }) => {
    await darken(page);
    // Capture Lumo's dark default for the card surface using the Custom preset
    // (which resolves to brand-default — pure Lumo tokens).
    await pickVehicle(page, 'Custom', 'brand-default');
    const lumoDarkCard = await bgOf(page, 'html.brand-default .charging-card');

    await pickVehicle(page, /Polestar 2 LR/i, 'brand-polestar');
    expect(normalise(await bgOf(page, 'html.brand-polestar .charging-card'))).toBe(normalise('#1E1E1E'));

    await pickVehicle(page, 'Custom', 'brand-default');
    expect(normalise(await bgOf(page, 'html.brand-default .charging-card'))).toBe(normalise(lumoDarkCard));
  });

  test('non-regression: BMW dark surface still reads #1A2129', async ({ page }) => {
    await darken(page);
    await pickVehicle(page, /BMW iX xDrive50/i, 'brand-bmw');
    // BMW Tier 1 sets --lumo-base-color in dark mode; the variable is what the
    // brand block exposes, regardless of whether <html> background is painted.
    expect(normalise(await colorVar(page, '--lumo-base-color', 'html.brand-bmw')))
      .toBe(normalise('#1A2129'));
  });

  test('full takeover: html, drawer and navbar all paint Polestar #141414', async ({ page }) => {
    await darken(page);
    await pickVehicle(page, /Polestar 2 LR/i, 'brand-polestar');
    expect(normalise(await bgOf(page, 'html'))).toBe(normalise('#141414'));
    const chrome = await page.evaluate(() => {
      const layout = document.querySelector('vaadin-app-layout') as any;
      const shadow = layout?.shadowRoot;
      const navbar = shadow?.querySelector('div[part="navbar"]:not([hidden])') as HTMLElement | null;
      const drawer = shadow?.querySelector('div[part="drawer"]') as HTMLElement | null;
      return {
        navbar: navbar ? getComputedStyle(navbar).backgroundColor : null,
        drawer: drawer ? getComputedStyle(drawer).backgroundColor : null,
      };
    });
    expect(normalise(chrome.navbar ?? '')).toBe(normalise('#141414'));
    expect(normalise(chrome.drawer ?? '')).toBe(normalise('#141414'));
  });

  test('takeover persists across navigation: html keeps Polestar #141414 off /charging', async ({ page }) => {
    await darken(page);
    await pickVehicle(page, /Polestar 2 LR/i, 'brand-polestar');
    expect(normalise(await bgOf(page, 'html'))).toBe(normalise('#141414'));

    await page.goto('/registrations');
    // The brand class lives on <html>, so it follows the user across views.
    expect(normalise(await bgOf(page, 'html'))).toBe(normalise('#141414'));
    await expect(page.locator('html.brand-polestar')).toHaveCount(1);
  });
});
