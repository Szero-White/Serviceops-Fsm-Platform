import { readdirSync, readFileSync, statSync } from 'node:fs'
import { join, relative } from 'node:path'
import { fileURLToPath } from 'node:url'

const frontendRoot = join(fileURLToPath(new URL('..', import.meta.url)))
const appStylesRoot = join(frontendRoot, 'src', 'styles', 'app')
const landingStylesRoot = join(frontendRoot, 'src', 'pages', 'landing', 'styles')
const violations = []

const productTokenExpectations = new Map([
  ['--app-type-caption', '11px'],
  ['--app-type-meta', '11px'],
  ['--app-type-label', '12px'],
  ['--app-type-body', '12px'],
  ['--app-type-body-lg', '13px'],
  ['--app-type-section-title', '13px'],
  ['--app-type-panel-title', '15px'],
  ['--app-type-auth-title', '20px'],
  ['--app-type-metric', '20px'],
  ['--app-type-page-title', '22px'],
])

const baseCssPath = join(appStylesRoot, 'base.css')
const baseCss = readFileSync(baseCssPath, 'utf8')
for (const [token, expected] of productTokenExpectations) {
  const match = baseCss.match(new RegExp(`${token}\\s*:\\s*([^;]+);`))
  if (!match || match[1].trim() !== expected) {
    violations.push(`src/styles/app/base.css product token ${token} must be ${expected}`)
  }
}

function walk(directory) {
  return readdirSync(directory).flatMap((name) => {
    const path = join(directory, name)
    return statSync(path).isDirectory() ? walk(path) : [path]
  })
}

for (const file of walk(appStylesRoot).filter((path) => path.endsWith('.css'))) {
  const source = readFileSync(file, 'utf8')
  const display = relative(frontendRoot, file).replaceAll('\\\\', '/')
  const lines = source.split(/\r?\n/)

  lines.forEach((line, index) => {
    if (/font-size\s*:\s*\d+\.\d+px/i.test(line)) {
      violations.push(`${display}:${index + 1} fractional font-size -> ${line.trim()}`)
    }
    if (/font-weight\s*:\s*(?:[7-9]00|bold)\b/i.test(line)) {
      violations.push(`${display}:${index + 1} heavy font-weight -> ${line.trim()}`)
    }
    if (/text-transform\s*:\s*uppercase\b/i.test(line)) {
      violations.push(`${display}:${index + 1} forced uppercase -> ${line.trim()}`)
    }
    const rawFontSize = line.match(/font-size\s*:\s*([^;]+);/i)
    if (rawFontSize) {
      const value = rawFontSize[1].trim()
      const allowed = value.startsWith('var(--app-type-') || value.startsWith('var(--app-icon-')
      if (!allowed) violations.push(`${display}:${index + 1} raw app font-size -> ${line.trim()}`)
    }
  })
}

for (const file of walk(landingStylesRoot).filter((path) => path.endsWith('.css'))) {
  const source = readFileSync(file, 'utf8')
  const display = relative(frontendRoot, file).replaceAll('\\', '/')
  const lines = source.split(/\r?\n/)

  lines.forEach((line, index) => {
    const rawFontSize = line.match(/font-size\s*:\s*([^;]+);/i)
    if (!rawFontSize) return
    const value = rawFontSize[1].trim().replace(/\s*!important$/, '')
    const allowed = value.startsWith('var(--lp-type-') || value.startsWith('var(--lp-icon-') || value.startsWith('var(--lp-mockup-')
    if (!allowed) violations.push(`${display}:${index + 1} raw landing font-size -> ${line.trim()}`)
  })
}

if (violations.length > 0) {
  console.error('UI typography policy failed:\n' + violations.map((item) => `- ${item}`).join('\n'))
  process.exit(1)
}

console.log('UI typography policy passed')
