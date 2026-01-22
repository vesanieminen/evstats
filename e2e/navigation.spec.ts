import { expect, test } from '@playwright/test';

test.describe('Navigation and layout', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
  });

  test('side nav routes resolve', async ({ page }) => {
    await expect(page.getByRole('link', { name: 'Charging tool' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Adoption curve' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'New cars' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Tesla registrations' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'New Teslas / year' })).toBeVisible();

    await page.goto('/adoption');
    await expect(page).toHaveURL(/\/adoption$/);

    await page.goto('/registrations');
    await expect(page).toHaveURL(/\/registrations$/);

    await page.goto('/tesla-registrations');
    await expect(page).toHaveURL(/\/tesla-registrations$/);

    await page.goto('/tesla-registrations-bar');
    await expect(page).toHaveURL(/\/tesla-registrations-bar$/);
  });

  test('header title updates per view', async ({ page }) => {
    await page.getByRole('link', { name: 'Adoption curve' }).click();
    await expect(page.getByRole('heading', { name: 'New EV registration percentage' })).toBeVisible();

    await page.getByRole('link', { name: 'New cars' }).click();
    await expect(page.getByRole('heading', { name: 'New EV registrations per month' })).toBeVisible();
  });

  test('footer links visible', async ({ page }) => {
    await expect(page.getByRole('link', { name: 'aut.fi' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Built with Vaadin' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Code on Github' })).toBeVisible();
  });
});
