/**
 * End-to-end UI check: renders the real CoursesPage against the running backend and asserts
 * on what a user would actually see. Proves the price fix and the CSS-comment leak fix
 * together, using live data rather than fixtures.
 */
import { createRoot } from 'react-dom/client'
import { act, createElement } from 'react'
import { MemoryRouter } from 'react-router-dom'
import CoursesPage from '../src/pages/CoursesPage'
import { AuthProvider } from '../src/context/AuthContext'

const host = document.createElement('div')
document.body.appendChild(host)
const root = createRoot(host)

const sleep = (ms) => new Promise((r) => setTimeout(r, ms))

await act(async () => {
  // CoursesPage renders SiteNavbar, which reads the auth context.
  root.render(
    createElement(MemoryRouter, { initialEntries: ['/courses'] },
      createElement(AuthProvider, null, createElement(CoursesPage))))
})

// Let the real axios GET /api/courses and /api/categories settle.
for (let i = 0; i < 60 && host.textContent.includes('Loading'); i++) {
  await act(async () => { await sleep(100) })
}
await act(async () => { await sleep(500) })

const text = host.textContent
const results = []
const check = (name, pass, detail = '') => results.push({ name, pass, detail })

check('catalog finished loading', !text.includes('Loading'),
  text.includes('Loading') ? text.slice(0, 160) : 'loaded')
check('catalog rendered course titles', /Digital Marketing Basics/.test(text) && /Spring Boot/.test(text),
  `length=${text.length}`)

// The original report: a CSS comment rendered as page content.
for (const leak of ['Underscores become spaces', 'calc(100%-11rem)', 'silently dropped', '/* ', '*/']) {
  check(`no source text leaked: "${leak.trim()}"`, !text.includes(leak))
}

// Prices: whole rupees, no .99 anywhere on the page.
check('no .99-style price on the page', !/Rs\.\d+\.\d+/.test(text),
  (text.match(/Rs\.[\d,.]+/g) ?? []).join(' '))

const expected = [
  ['Digital Marketing Basics', 'Rs.199'],
  ['Java Programming Fundamentals', 'Rs.250'],
  ['React for Beginners', 'Rs.440'],
  ['Spring Boot Backend Development', 'Rs.650'],
  ['React Native Mobile Development', 'Rs.750'],
  ['PostgreSQL Database Masterclass', 'Rs.249'],
  ['Python for Data Analysis', 'Rs.550'],
]
for (const [title, price] of expected) {
  const card = [...host.querySelectorAll('article')].find((a) => a.textContent.includes(title))
  const shown = card?.textContent.match(/Rs\.[\d,]+/)?.[0]
  check(`${title} shows ${price}`, shown === price, `got ${shown}`)
}

// The level filter is a <select>, so read its <option>s -- asserting on buttons here
// would pass vacuously.
const levelOptions = [...host.querySelectorAll('select')].pop()
const allOptions = [...(levelOptions?.querySelectorAll('option') ?? [])].map((o) => o.textContent.trim())
const options = allOptions.filter((o) => o !== 'All levels')

check('level filter rendered options', options.length > 0, `options=[${allOptions.join(', ')}]`)
check('exactly one entry per level, no case-duplicates',
  new Set(options.map((o) => o.toLowerCase())).size === options.length,
  `options=[${allOptions.join(', ')}]`)
check('levels are uppercase', !options.includes('beginner'), `options=[${allOptions.join(', ')}]`)
check('no duplicate-case level entries', !(options.includes('beginner') && options.includes('BEGINNER')),
  `options=[${allOptions.join(', ')}]`)

let failed = 0
for (const r of results) {
  if (!r.pass) failed++
  console.log(`${r.pass ? 'PASS' : 'FAIL'}  ${r.name}${r.detail ? `\n        ${r.detail}` : ''}`)
}
console.log(`\n${results.length - failed}/${results.length} passed`)
process.exit(failed === 0 ? 0 : 1)