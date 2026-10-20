import { defineConfig } from '@playwright/test'

// By default the tests start the React dev server and mock the backend API (fast, no Java needed).
// For a full-stack run against the real backend, start `docker compose up` and run:
//   E2E_LIVE=1 E2E_BASE_URL=http://localhost:3000 npx playwright test live
const live = process.env.E2E_LIVE === '1'
const baseURL = process.env.E2E_BASE_URL || 'http://localhost:5173'

export default defineConfig({
  testDir: './tests',
  timeout: 20_000,
  retries: process.env.CI ? 1 : 0,
  reporter: process.env.CI ? [['github'], ['html', { open: 'never' }]] : 'list',
  use: {
    baseURL,
    trace: 'retain-on-failure',
  },
  webServer: live
    ? undefined
    : {
        command: 'npm run dev --prefix ../frontend -- --host 127.0.0.1 --port 5173',
        url: 'http://127.0.0.1:5173',
        reuseExistingServer: !process.env.CI,
        timeout: 60_000,
      },
})
