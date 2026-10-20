import { test, expect } from '@playwright/test'

const EWL_MAJOR = {
  id: 1,
  line: 'EWL',
  direction: 'Both',
  stations: 'EW27,EW26,EW25,EW24',
  message: 'EWL: No train service between Boon Lay and Jurong East due to a signalling fault.',
  severity: 'MAJOR',
  freeBusAvailable: true,
  active: true,
  firstSeen: '2026-10-20T00:12:00Z',
  lastSeen: '2026-10-20T00:20:00Z',
  resolvedAt: null,
}

const NSL_MINOR = {
  id: 2,
  line: 'NSL',
  direction: 'Northbound',
  stations: 'NS13,NS14',
  message: 'NSL: Delay of 5 minutes towards Marina South Pier.',
  severity: 'MINOR',
  freeBusAvailable: false,
  active: true,
  firstSeen: '2026-10-20T00:30:00Z',
  lastSeen: '2026-10-20T00:31:00Z',
  resolvedAt: null,
}

const NSL_RESOLVED = {
  ...NSL_MINOR,
  id: 3,
  active: false,
  resolvedAt: '2026-10-20T01:00:00Z',
}

/**
 * Pretend to be the backend. `state` lets each test control what the API returns
 * and records the requests the page made so we can assert on them.
 */
async function mockApi(page, state) {
  state.requests = []
  await page.route('**/api/**', async (route) => {
    const request = route.request()
    const url = new URL(request.url())
    state.requests.push(`${request.method()} ${url.pathname}${url.search}`)

    if (state.fail) {
      return route.fulfill({ status: 500, body: 'boom' })
    }
    if (url.pathname === '/api/status') {
      const active = state.disruptions.filter((d) => d.active)
      const byLine = {}
      active.forEach((d) => (byLine[d.line] = (byLine[d.line] || 0) + 1))
      return route.fulfill({
        json: {
          overall: active.length ? 'DISRUPTED' : 'NORMAL',
          activeCount: active.length,
          activeByLine: byLine,
        },
      })
    }
    if (url.pathname === '/api/refresh') {
      return route.fulfill({ json: { overall: 'NORMAL', activeCount: 0, activeByLine: {} } })
    }
    if (url.pathname === '/api/disruptions') {
      const line = url.searchParams.get('line')
      const activeOnly = url.searchParams.get('activeOnly') !== 'false'
      let list = state.disruptions
      if (line) list = list.filter((d) => d.line === line)
      if (activeOnly) list = list.filter((d) => d.active)
      return route.fulfill({ json: list })
    }
    return route.continue()
  })
}

test.describe('dashboard (mocked API)', () => {
  test('shows active disruptions and a disrupted banner', async ({ page }) => {
    await mockApi(page, { disruptions: [EWL_MAJOR, NSL_MINOR] })
    await page.goto('/')

    await expect(page.getByTestId('status-banner')).toHaveText('2 active disruptions')
    const cards = page.getByTestId('disruption-card')
    await expect(cards).toHaveCount(2)
    await expect(cards.first()).toContainText('EWL')
    await expect(cards.first()).toContainText('MAJOR')
    await expect(cards.first()).toContainText('Free bus service available')
    await expect(page.getByTestId('line-summary')).toContainText('EWL: 1')
    await expect(page.getByTestId('line-summary')).toContainText('NSL: 1')
  })

  test('shows normal banner and empty state when nothing is disrupted', async ({ page }) => {
    await mockApi(page, { disruptions: [] })
    await page.goto('/')

    await expect(page.getByTestId('status-banner')).toHaveText('All lines running normally')
    await expect(page.getByTestId('empty-state')).toContainText('No active disruptions')
    await expect(page.getByTestId('disruption-card')).toHaveCount(0)
  })

  test('filtering by line only shows that line and calls the API with the line', async ({ page }) => {
    const state = { disruptions: [EWL_MAJOR, NSL_MINOR] }
    await mockApi(page, state)
    await page.goto('/')
    await expect(page.getByTestId('disruption-card')).toHaveCount(2)

    await page.getByTestId('line-filter').selectOption('NSL')

    await expect(page.getByTestId('disruption-card')).toHaveCount(1)
    await expect(page.getByTestId('disruption-card')).toContainText('NSL')
    expect(state.requests.some((r) => r.includes('line=NSL'))).toBe(true)
  })

  test('empty state mentions the selected line', async ({ page }) => {
    await mockApi(page, { disruptions: [EWL_MAJOR] })
    await page.goto('/')

    await page.getByTestId('line-filter').selectOption('CCL')

    await expect(page.getByTestId('empty-state')).toContainText('on CCL')
  })

  test('show resolved includes resolved history with a Resolved tag', async ({ page }) => {
    const state = { disruptions: [EWL_MAJOR, NSL_RESOLVED] }
    await mockApi(page, state)
    await page.goto('/')
    await expect(page.getByTestId('disruption-card')).toHaveCount(1)

    await page.getByTestId('show-resolved').check()

    await expect(page.getByTestId('disruption-card')).toHaveCount(2)
    await expect(page.getByText('Resolved', { exact: true }).first()).toBeVisible()
    expect(state.requests.some((r) => r.includes('activeOnly=false'))).toBe(true)
  })

  test('refresh button triggers a backend refresh', async ({ page }) => {
    const state = { disruptions: [EWL_MAJOR] }
    await mockApi(page, state)
    await page.goto('/')
    await expect(page.getByTestId('disruption-card')).toHaveCount(1)

    await page.getByTestId('refresh-button').click()

    await expect.poll(() => state.requests.includes('POST /api/refresh')).toBe(true)
    await expect(page.getByTestId('last-updated')).toBeVisible()
  })

  test('shows an error banner when the API fails', async ({ page }) => {
    await mockApi(page, { disruptions: [], fail: true })
    await page.goto('/')

    await expect(page.getByTestId('error-banner')).toContainText('Could not load data')
  })
})
