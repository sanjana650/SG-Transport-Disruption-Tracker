import { test, expect } from '@playwright/test'

// Full-stack smoke test: real React app -> real Spring Boot backend (running with mock LTA data).
// Only runs when E2E_LIVE=1, e.g. against `docker compose up`.
test.skip(process.env.E2E_LIVE !== '1', 'set E2E_LIVE=1 to run against the real backend')

test('backend serves status and the dashboard renders it', async ({ page, request }) => {
  const status = await request.get('/api/status')
  expect(status.ok()).toBeTruthy()
  const body = await status.json()
  expect(['NORMAL', 'DISRUPTED']).toContain(body.overall)

  await page.goto('/')
  await expect(page.getByTestId('status-banner')).toBeVisible()
  await expect(page.getByRole('heading', { name: 'SG Transport Disruption Tracker' })).toBeVisible()
})

test('refresh endpoint works end to end', async ({ page }) => {
  await page.goto('/')
  await page.getByTestId('refresh-button').click()
  await expect(page.getByTestId('last-updated')).toBeVisible()
  await expect(page.getByTestId('error-banner')).toHaveCount(0)
})
