import { defineConfig, devices } from '@playwright/test'

const baseURL = process.env.E2E_BASE_URL?.trim()

if (!baseURL) {
  throw new Error(
    'E2E_BASE_URL is required. Use an isolated test stack and set E2E_ALLOW_MUTATIONS=true explicitly.',
  )
}

let parsedBaseURL: URL
try {
  parsedBaseURL = new URL(baseURL)
} catch {
  throw new Error(`E2E_BASE_URL must be a valid absolute URL. Received: ${baseURL}`)
}

const allowMutations = process.env.E2E_ALLOW_MUTATIONS?.trim().toLowerCase() === 'true'

if (!allowMutations) {
  throw new Error(
    `Refusing to run mutating Playwright E2E against ${baseURL} without explicit opt-in. `
      + 'Use disposable isolated test data and set E2E_ALLOW_MUTATIONS=true.',
  )
}

export default defineConfig({
  testDir: './e2e',
  timeout: 60_000,
  expect: { timeout: 10_000 },
  fullyParallel: false,
  retries: process.env.CI ? 1 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: process.env.CI
    ? [['line'], ['html', { outputFolder: 'playwright-report', open: 'never' }]]
    : [['list'], ['html', { outputFolder: 'playwright-report', open: 'never' }]],
  outputDir: 'test-results',
  use: {
    baseURL,
    actionTimeout: 15_000,
    navigationTimeout: 15_000,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
    ...devices['Desktop Chrome'],
  },
})
