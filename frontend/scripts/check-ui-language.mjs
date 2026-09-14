import { readdirSync, readFileSync, statSync } from 'node:fs'
import { join, relative } from 'node:path'
import { fileURLToPath } from 'node:url'

const frontendRoot = join(fileURLToPath(new URL('..', import.meta.url)))
const sourceRoot = join(frontendRoot, 'src')
const violations = []

// User-facing React surfaces must use business language. Internal API names,
// enums and domain identifiers remain stable and are translated by the
// presentation layer before they reach the UI.
const forbiddenUserFacingPatterns = [
  { pattern: /\bKTV\b/, label: 'abbreviation KTV' },
  { pattern: /\bCSKH\b/, label: 'abbreviation CSKH' },
  { pattern: /\bSKU\b/, label: 'inventory code jargon SKU' },
  { pattern: /\bJWT\b/, label: 'security implementation detail JWT' },
  { pattern: /\bRBAC\b/, label: 'security implementation detail RBAC' },
  { pattern: /Tenant isolation/i, label: 'implementation phrase Tenant isolation' },
  { pattern: /audit trail/i, label: 'technical phrase audit trail' },
  { pattern: /\bWork Orders?\b/, label: 'English Work Order label' },
  { pattern: /\bService Request\b/, label: 'English Service Request label' },
  { pattern: /\bBackend\b/, label: 'implementation term Backend' },
  { pattern: /Public deployment/i, label: 'deployment implementation phrase' },
  { pattern: /\bFSM\b/, label: 'technical acronym FSM' },
  { pattern: /\bserial\b/i, label: 'technical asset term serial' },
  { pattern: /\bdatabase\b/i, label: 'implementation term database' },
  { pattern: /API URL/i, label: 'implementation term API URL' },
  { pattern: /\bREST API\b/i, label: 'implementation term REST API' },
  { pattern: /HTTP\/JSON/i, label: 'transport implementation detail HTTP/JSON' },
  { pattern: /\bPrometheus\b/i, label: 'monitoring implementation detail Prometheus' },
  { pattern: /\bCRUD\b/, label: 'engineering term CRUD' },
  { pattern: /domain flow/i, label: 'engineering phrase domain flow' },
  { pattern: /Testcontainers/i, label: 'test infrastructure term Testcontainers' },
  { pattern: /\(legacy\)/i, label: 'legacy marker' },
  { pattern: /Nhập CSV/i, label: 'file-format-oriented label Nhập CSV' },
  { pattern: /Xuất CSV/i, label: 'file-format-oriented label Xuất CSV' },
  { pattern: /Tải mẫu import/i, label: 'mixed-language import label' },
  { pattern: /Tìm[^\n'"`]*\busername\b/i, label: 'user-facing username search label' },
  { pattern: /\bUsername[,:]/, label: 'user-facing Username label' },
  { pattern: /\bpublic demo\b/i, label: 'mixed-language public demo phrase' },
  { pattern: /\bDemo cố định\b/i, label: 'unclear protected-demo label' },
  { pattern: /\bOWNER\s+(?:tạo|quản|có|được|xem|dùng)/i, label: 'raw OWNER role in explanatory copy' },
  { pattern: /\bCustomer Service\b/, label: 'English Customer Service role label' },
  { pattern: /\bWarehouse Staff\b/, label: 'English Warehouse Staff role label' },
  { pattern: /\bDispatcher\b/, label: 'English Dispatcher role label' },
]

function walk(directory) {
  return readdirSync(directory).flatMap((name) => {
    const path = join(directory, name)
    return statSync(path).isDirectory() ? walk(path) : [path]
  })
}

for (const file of walk(sourceRoot).filter((path) => path.endsWith('.tsx'))) {
  const source = readFileSync(file, 'utf8')
  const display = relative(frontendRoot, file).replaceAll('\\', '/')
  const lines = source.split(/\r?\n/)

  lines.forEach((line, index) => {
    for (const rule of forbiddenUserFacingPatterns) {
      if (rule.pattern.test(line)) {
        violations.push(`${display}:${index + 1} ${rule.label} -> ${line.trim()}`)
      }
    }
  })
}

if (violations.length > 0) {
  console.error('UI business-language policy failed:\n' + violations.map((item) => `- ${item}`).join('\n'))
  process.exit(1)
}

console.log('UI business-language policy passed')
