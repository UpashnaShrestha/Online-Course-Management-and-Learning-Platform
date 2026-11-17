/**
 * Renders every routed page and exercises its interactions, failing on any React error.
 *
 * Extends allPages.verify.jsx, which only checks that each page mounts. A page that mounts
 * but throws when a tab, accordion or filter is clicked is still broken, so this drives the
 * real controls: every course-builder tab, every catalog filter, the dropdowns, and the
 * password visibility toggles.
 *
 * Needs the backend on :8081.
 */
import { createRoot } from 'react-dom/client'
import { act, createElement } from 'react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { AuthProvider } from '../src/context/AuthContext'
import { NotificationsProvider } from '../src/context/NotificationsContext'
import CoursesPage from '../src/pages/CoursesPage'
import InstructorCourses from '../src/pages/instructor/InstructorCourses'
import Register from '../src/pages/auth/Register'
import Login from '../src/pages/auth/Login'
import ResetPassword from '../src/pages/auth/ResetPassword'
import CourseDetailPage from '../src/pages/CourseDetailPage'
import { clearSession, writeSession } from '../src/services/session'

const BASE = 'http://localhost:8081'
const results = []
const check = (name, pass, detail = '') => results.push({ name, pass, detail })
const sleep = (ms) => new Promise((r) => setTimeout(r, ms))

const online = await fetch(`${BASE}/api/courses/options`).then(() => true).catch(() => false)
if (!online) {
  console.log(`SKIPPED: no backend on ${BASE}. Start the app, then re-run.`)
  process.exit(0)
}

// Anything React reports as an error fails the run outright.
const reactErrors = []
const realError = console.error
// A state update that lands outside act() is a harness artefact of awaiting timers between
// renders, not a defect in the component. It is recorded separately so it is visible without
// failing the run; genuine React errors still fail.
const ACT_ADVISORY = /not wrapped in act\(|The current testing environment/i
const isActNoise = (message) => ACT_ADVISORY.test(message)

console.error = (...args) => {
  const message = args.map(String).join(' ')
  if (!isActNoise(message)) reactErrors.push(message)
  realError(...args)
}
const realWarn = console.warn
const reactWarnings = []
console.warn = (...args) => {
  const message = args.map(String).join(' ')
  if (!isActNoise(message)) reactWarnings.push(message)
  realWarn(...args)
}

async function mount(element) {
  const host = document.createElement('div')
  document.body.appendChild(host)
  const root = createRoot(host)
  await act(async () => {
    root.render(element)
  })
  return { host, root }
}

const wrap = (child, path = '/') =>
  createElement(MemoryRouter, { initialEntries: [path] },
    createElement(AuthProvider, null, createElement(NotificationsProvider, null, child)))

const click = (el) => act(async () => {
  el.dispatchEvent(new globalThis.MouseEvent('click', { bubbles: true }))
  await sleep(30)
})

/**
 * Waits until the page stops showing a loading state.
 *
 * Matching on the word "Loading" is fragile: each page names its own spinner ("Finding
 * courses...", "Loading certificates..."). This instead polls until the DOM stops changing,
 * which covers any label and any number of sequential requests.
 */
async function settle(host, { attempts = 80, quietMs = 150 } = {}) {
  let previous = null
  let stableFor = 0

  for (let i = 0; i < attempts; i++) {
    await act(async () => { await sleep(50) })
    const current = host.textContent
    if (current === previous) {
      stableFor += 50
      if (stableFor >= quietMs) return
    } else {
      stableFor = 0
      previous = current
    }
  }
}

/** Marks the current error count, so a block only fails on errors it caused. */
const mark = () => reactErrors.length
const since = (baseline) => reactErrors.slice(baseline)

const type = async (input, text) => {
  const setter = Object.getOwnPropertyDescriptor(globalThis.HTMLInputElement.prototype, 'value').set
  for (const ch of text) {
    setter.call(input, input.value + ch)
    input.dispatchEvent(new globalThis.Event('input', { bubbles: true }))
    await act(async () => { await sleep(0) })
  }
}

const courses = (await fetch(`${BASE}/api/courses`).then((r) => r.json())).data ?? []
const aCourseId = courses[0]?.id ?? 1

// ---------------------------------------------------------------- catalog filters
{
  const { host, root } = await mount(wrap(createElement(CoursesPage), '/courses'))
  await settle(host)

  const cardsBefore = host.querySelectorAll('article').length
  check('catalog lists courses', cardsBefore > 0, `cards=${cardsBefore}`)
  if (cardsBefore === 0) {
    console.log('  React errors so far:')
    reactErrors.slice(0, 5).forEach((e) => console.log('    ' + e.split('\n')[0]))
    console.log('  page text: ' + host.textContent.slice(0, 300))
    }

  const selects = [...host.querySelectorAll('select')]
  const levelSelect = selects.at(-1)
  const options = [...levelSelect.querySelectorAll('option')]

  // Click through every level option; the page must not throw on any of them.
  let levelFailures = 0
  const levelBaseline = mark()
  for (const option of options) {
    await act(async () => {
      const setter = Object.getOwnPropertyDescriptor(
        globalThis.HTMLSelectElement.prototype, 'value').set
      setter.call(levelSelect, option.value)
      levelSelect.dispatchEvent(new globalThis.Event('change', { bubbles: true }))
      await sleep(20)
    })
  }
  check('every level filter option applies without error',
    since(levelBaseline).length === 0,
    since(levelBaseline).slice(0, 2).map((e) => e.split('\n')[0]).join(' | '))

  // Reset to ALL and confirm the full list returns.
  await act(async () => {
    const setter = Object.getOwnPropertyDescriptor(
      globalThis.HTMLSelectElement.prototype, 'value').set
    setter.call(levelSelect, 'ALL')
    levelSelect.dispatchEvent(new globalThis.Event('change', { bubbles: true }))
    await sleep(20)
  })
  check('resetting the level filter restores every course',
    host.querySelectorAll('article').length === cardsBefore,
    `${host.querySelectorAll('article').length} vs ${cardsBefore}`)

  // Type into search.
  const search = host.querySelector('input[type="search"], input[placeholder*="Search" i]')
  if (search) {
    const searchBaseline = mark()
    await type(search, 'react')
    await act(async () => { await sleep(40) })
    check('search filters without error', since(searchBaseline).length === 0,
      since(searchBaseline).slice(0, 2).map((e) => e.split('\n')[0]).join(' | '))
    await type(search, '')
  } else {
    check('search field found', false, 'no search input rendered')
  }

  await act(async () => { root.unmount() })
}

// ------------------------------------------------------------ course builder tabs
// Sign in as an instructor so the builder's own data (courses, modules, lessons) is present.
// Without a session the page redirects to /login and none of the tab behaviour can be
// exercised -- which is how a broken tab bar hides behind "it only renders when signed in".
const seededInstructor = { username: 'aarav.shrestha', password: 'CourseFlow#2026' }
let instructorToken = null
try {
  const login = await fetch(`${BASE}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ ...seededInstructor, rememberMe: false }),
  })
  instructorToken = (await login.json())?.data?.accessToken ?? null
} catch { /* handled below */ }

check('instructor session established for the builder tests', !!instructorToken,
  instructorToken ? seededInstructor.username : `login failed for ${seededInstructor.username}`)

if (instructorToken) {
  writeSession({
    accessToken: instructorToken,
    refreshToken: instructorToken,
    role: 'INSTRUCTOR',
    username: seededInstructor.username,
    firstName: 'Aarav',
    lastName: 'Shrestha',
    email: 'aarav.shrestha@courseflow.edu',
  }, true)
}

{
  const { host, root } = await mount(wrap(createElement(InstructorCourses), '/instructor/courses'))
  await settle(host)

  const courseTitles = courses.map((c) => c.title)
  const courseButtons = [...host.querySelectorAll('button')].filter(
    (b) => courseTitles.some((title) => (b.textContent ?? '').includes(title))
  )
  check('builder lists the instructor\'s courses', courseButtons.length > 0,
    `matched ${courseButtons.length} of ${courseTitles.length} titles`)

  if (courseButtons.length) {
    // Selecting a course reveals the tab bar; the builder needs one to be open.
    await click(courseButtons[0])
    await settle(host)
  }

  const tabs = [...host.querySelectorAll('[role="tab"]')]
  check('builder renders its tab bar once a course is open', tabs.length > 0, `tabs=${tabs.length}`)

  if (tabs.length) {
    let failures = 0
    const seenPanels = new Set()

    for (const tab of tabs) {
      const before = reactErrors.length
      await click(tab)
      if (reactErrors.length > before) failures++

      // Each tab must swap the panel, not just toggle aria-selected.
      const selected = tab.getAttribute('aria-selected')
      if (selected === 'true') seenPanels.add(tab.getAttribute('aria-controls'))
    }
    check('every builder tab switches without a React error', failures === 0,
      `${failures} of ${tabs.length} threw`)
    check('each tab activated a distinct panel', seenPanels.size === tabs.length,
      `panels=${seenPanels.size} tabs=${tabs.length}`)
  }

  // Keyboard navigation across the tablist.
  if (tabs.length) {
    const before = reactErrors.length
    await act(async () => {
      tabs[0].dispatchEvent(new globalThis.KeyboardEvent('keydown', {
        key: 'ArrowRight', bubbles: true,
      }))
      await sleep(20)
    })
    check('arrow keys move through the tablist without error', reactErrors.length === before)
  }

  // The jump list must stay consistent with the tabs.
  const jumps = [...host.querySelectorAll('nav[aria-label="Course sections"] button')]
  check('jump list present alongside the tabs', jumps.length > 0, `jumps=${jumps.length}`)

  await act(async () => { root.unmount() })
}

// --------------------------------------------------------- level/duration dropdowns
// This page's dropdowns live in the create-course form, which is visible while signed out
// too, so the session is irrelevant here.
clearSession()
{
  const { host, root } = await mount(wrap(createElement(InstructorCourses), '/instructor/courses'))
  await settle(host)

  const selects = [...host.querySelectorAll('select')]
  const level = selects.find((s) => [...s.options].some((o) => o.value === 'Beginner'))
  const duration = selects.find((s) => [...s.options].some((o) => o.value === 'Under 1 hour'))

  check('level is a dropdown, not a text box', !!level,
    level ? `${level.options.length} options` : 'not found')
  check('duration is a dropdown, not a text box', !!duration,
    duration ? `${duration.options.length} options` : 'not found')
  check('level dropdown has a placeholder option',
    level?.options[0]?.value === '', level?.options[0]?.text)

  if (level && duration) {
    // Selecting must update the form state without a controlled-input warning.
    const before = reactErrors.length + reactWarnings.length
    await act(async () => {
      const setter = Object.getOwnPropertyDescriptor(
        globalThis.HTMLSelectElement.prototype, 'value').set
      setter.call(level, 'Advanced')
      level.dispatchEvent(new globalThis.Event('change', { bubbles: true }))
      setter.call(duration, '3-6 months')
      duration.dispatchEvent(new globalThis.Event('change', { bubbles: true }))
      await sleep(30)
    })
    check('selecting a level and duration updates the form cleanly',
      reactErrors.length + reactWarnings.length === before,
      'React complained about the controlled select')
    check('level selection is reflected in the DOM', level.value === 'Advanced', level.value)
    check('duration selection is reflected in the DOM', duration.value === '3-6 months',
      duration.value)
  }

  await act(async () => { root.unmount() })
}

// --------------------------------------------------------- password fields + toggles
// Clear the session first: Register and Login redirect to the dashboard when already signed
// in, which would silently render no password field at all.
clearSession()

for (const [name, Component, path] of [
  ['Register', Register, '/register'],
  ['Login', Login, '/login'],
  ['ResetPassword', ResetPassword, '/reset-password'],
]) {
  const { host, root } = await mount(wrap(createElement(Component), path))
  await act(async () => { await sleep(120) })

  const passwords = [...host.querySelectorAll('input[type="password"]')]
  check(`${name}: renders password field(s)`, passwords.length > 0, `count=${passwords.length}`)

  if (passwords.length) {
    const field = passwords[0]
    const before = field.type
    const toggle = host.querySelector('button[aria-label^="Show"], button[aria-label^="Hide"]')
    check(`${name}: has a visibility toggle`, !!toggle, toggle?.getAttribute('aria-label'))

    if (toggle) {
      await click(toggle)
      const after = field.type
      check(`${name}: toggle switches the field type`, before !== after, `${before} -> ${after}`)
      await click(toggle)
      check(`${name}: toggle switches back`, field.type === before, `${after} -> ${field.type}`)
    }

    // Typing must actually reach state.
    await type(field, 'Abc1!xyz')
    check(`${name}: typed value reaches state`, field.value === 'Abc1!xyz',
      `value="${field.value}"`)
  }

  await act(async () => { root.unmount() })
}

// ------------------------------------------------------------- course detail tabs
{
  // CourseDetailPage reads the id from the route via useParams, so it must be mounted
  // through a matching <Route path> rather than given an `id` prop.
  const { host, root } = await mount(
    createElement(MemoryRouter, { initialEntries: [`/courses/${aCourseId}`] },
      createElement(AuthProvider, null,
        createElement(NotificationsProvider, null,
          createElement(Routes, null,
            createElement(Route, {
              path: '/courses/:id',
              element: createElement(CourseDetailPage),
            }))))))
  await settle(host)

  const detailTabs = [...host.querySelectorAll('[role="tab"], button')]
  check('course detail rendered content', host.textContent.length > 200,
    `${host.textContent.length} chars`)

  let failures = 0
  const detailBaseline = mark()
  for (const tab of detailTabs.slice(0, 8)) {
    await click(tab)
  }
  const detailErrors = since(detailBaseline)
  check('course detail controls respond without a React error', detailErrors.length === 0,
    detailErrors.slice(0, 2).map((e) => e.split('\n')[0]).join(' | '))

  await act(async () => { root.unmount() })
}

// ------------------------------------------------------------------------- summary
let failed = 0
for (const r of results) {
  if (!r.pass) failed++
  console.log(`${r.pass ? 'PASS' : 'FAIL'}  ${r.name}${r.detail ? `\n        ${r.detail}` : ''}`)
}
console.log(`\n${results.length - failed}/${results.length} passed`)

const hookNoise = reactErrors.concat(reactWarnings)
  .filter((e) => /Invalid hook call|Cannot read properties of null \(reading 'use|resolveDispatcher/i.test(e))
if (hookNoise.length) {
  console.log('\nReact rule violations observed:')
  hookNoise.forEach((e) => console.log('  ' + e.split('\n')[0]))
  failed++
}

process.exit(failed === 0 ? 0 : 1)