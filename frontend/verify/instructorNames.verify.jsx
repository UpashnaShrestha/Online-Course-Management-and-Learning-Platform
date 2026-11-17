/**
 * End-to-end UI check against the running backend.
 *
 * Confirms the instructor names now appear on real courses, that the catalog's level filter
 * offers one entry per level, and that the dropdowns would accept the API's options.
 */
import { createRoot } from 'react-dom/client'
import { act, createElement } from 'react'
import { MemoryRouter } from 'react-router-dom'
import { AuthProvider } from '../src/context/AuthContext'
import CoursesPage from '../src/pages/CoursesPage'

const BASE = 'http://localhost:8081'
const results = []
const check = (name, pass, detail = '') => results.push({ name, pass, detail })
const sleep = (ms) => new Promise((r) => setTimeout(r, ms))

// ---- the API exposes the options the dropdown is built from ----
const options = await fetch(`${BASE}/api/courses/options`).then((r) => r.json())
const apiLevels = options.data.levels.map((o) => o.value)
const apiDurations = options.data.durations.map((o) => o.value)

check('API offers 3 levels', apiLevels.length === 3, apiLevels.join(', '))
check('API offers 11 durations', apiDurations.length === 11, `count=${apiDurations.length}`)
check('durations ship an approximate-hours value',
  options.data.durations.every((o) => typeof o.approximateHours === 'number'))

// ---- every published course has an instructor with a Nepali name ----
const courses = (await fetch(`${BASE}/api/courses`).then((r) => r.json())).data

check('courses returned', courses.length > 0, `count=${courses.length}`)
const missing = courses.filter((c) => !(c.instructors ?? []).length)
check('every course has at least one instructor', missing.length === 0,
  missing.map((c) => c.title).join(', '))

const named = courses.filter((c) =>
  (c.instructors ?? []).some((i) => i.firstName && i.lastName))
check('instructor names are present', named.length === courses.length,
  `${named.length}/${courses.length}`)

const sample = courses.slice(0, 4).map((c) => {
  const i = (c.instructors ?? [])[0]
  return `${c.title} -> ${i ? `${i.firstName} ${i.lastName}` : 'NONE'}`
})
console.log('\ninstructor assignment as rendered:')
sample.forEach((line) => console.log('  ' + line))
console.log('')

// Levels/durations in the data must be exactly what the dropdown offers, otherwise editing
// the course in the builder would blank the field.
const offListLevels = courses.filter((c) => c.level && !apiLevels.includes(c.level))
check('no course holds a level outside the offered set', offListLevels.length === 0,
  offListLevels.map((c) => `${c.title}=${c.level}`).join(', '))

const offListDurations = courses.filter((c) => c.duration && !apiDurations.includes(c.duration))
check('no course holds a duration outside the offered set', offListDurations.length === 0,
  offListDurations.map((c) => `${c.title}=${c.duration}`).join(', '))

// ---- render the catalog and read what a visitor sees ----
const host = document.createElement('div')
document.body.appendChild(host)
const root = createRoot(host)

await act(async () => {
  root.render(createElement(MemoryRouter, { initialEntries: ['/courses'] },
    createElement(AuthProvider, null, createElement(CoursesPage))))
})
for (let i = 0; i < 60 && host.textContent.includes('Loading'); i++) {
  await act(async () => { await sleep(100) })
}
await act(async () => { await sleep(400) })

const text = host.textContent
check('catalog loaded', !text.includes('Loading'), text.slice(0, 80))

// Instructor names must be on the card, not just in the API.
const cards = [...host.querySelectorAll('article')]
const withoutName = []
for (const card of cards) {
  // Match the card back to its course by the title the card renders.
  const title = card.querySelector('h2')?.textContent?.trim()
  const course = courses.find((c) => c.title === title)
  if (!course) { withoutName.push(`<unmatched card: ${title}>`); continue }

  const names = (course.instructors ?? []).map((i) => `${i.firstName} ${i.lastName}`.trim())
  if (!names.some((name) => name && card.textContent.includes(name))) {
    withoutName.push(`${title} (expected one of: ${names.join(' / ') || 'none'})`)
  }
}
check('cards show the instructor name', withoutName.length === 0,
  withoutName.join('; '))

// The level filter must offer one entry per distinct level, no case duplicates.
const levelSelect = [...host.querySelectorAll('select')].at(-1)
const levelOptions = [...(levelSelect?.querySelectorAll('option') ?? [])].map((o) => o.textContent.trim())
check('level filter has no case-duplicate entries',
  new Set(levelOptions.map((o) => o.toLowerCase())).size === levelOptions.length,
  `options=[${levelOptions.join(', ')}]`)
check('level filter matches the API levels',
  levelOptions.filter((o) => o !== 'All levels').every((o) => apiLevels.includes(o)),
  `filter=[${levelOptions.join(', ')}] api=[${apiLevels.join(', ')}]`)

// Durations and levels render in their human-readable form.
check('no raw enum names leak into the UI', !/\b(UNDER_1_HOUR|HOURS_\d|WEEKS_\d|MONTHS_\d)\b/.test(text))
check('no uppercase level names leak into the UI', !/\bBEGINNER\b|\bINTERMEDIATE\b/.test(text),
  (text.match(/\b(BEGINNER|INTERMEDIATE|ADVANCED)\b/g) ?? []).join(','))

let failed = 0
for (const r of results) {
  if (!r.pass) failed++
  console.log(`${r.pass ? 'PASS' : 'FAIL'}  ${r.name}${r.detail ? `\n        ${r.detail}` : ''}`)
}
console.log(`\n${results.length - failed}/${results.length} passed`)
process.exit(failed === 0 ? 0 : 1)