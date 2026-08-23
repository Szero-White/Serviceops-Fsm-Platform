import { defineConfig, devices } from '@playwright/test'

const baseURL = process.env.E2E_BASE_URL?.trim()

if (!baseURL) {
  throw new Error(
    'E2E_BASE_URL is required. Run the mutating Playwright suite only against an isolated test stack such as CI/Docker on port 8088.',
  )
}

let parsedBaseURL: URL
try {
  parsedBaseURL = new URL(baseURL)
} catch {
  throw new Error(`E2E_BASE_URL must be a valid absolute URL. Received: ${baseURL}`)
}

const localDevelopmentHosts = new Set(['localhost', '127.0.0.1', '::1'])
const localDevelopmentPorts = new Set(['3000', '5173'])

if (localDevelopmentHosts.has(parsedBaseURL.hostname) && localDevelopmentPorts.has(parsedBaseURL.port)) {
  throw new Error(
    `Refusing to run mutating Playwright E2E against the local development frontend (${baseURL}). Use the isolated Docker/CI stack instead.`,
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
