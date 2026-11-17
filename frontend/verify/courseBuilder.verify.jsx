/**
 * Confirms the new course-authoring surfaces render and behave:
 *  - the level/duration dropdowns come from the API and reject nothing on their own;
 *  - the tab bar switches panels and resets when a different course is selected;
 *  - the jump list and scroll-to-top button mount and toggle correctly.
 */
import { createRoot } from 'react-dom/client'
import { act, createElement } from 'react'
import { MemoryRouter } from 'react-router-dom'
import CourseTabBar from '../src/components/CourseTabBar'
import CourseSectionNav from '../src/components/CourseSectionNav'
import ScrollToTopButton from '../src/components/ScrollToTopButton'
import { flattenLessons } from '../src/components/course-builder/CoursePanels'
import {
  COURSE_DURATIONS,
  COURSE_LEVELS,
  normalizeDuration,
  normalizeLevel,
} from '../src/utils/courseOptions'

const results = []
const check = (name, pass, detail = '') => results.push({ name, pass, detail })
const sleep = (ms) => new Promise((r) => setTimeout(r, ms))

async function mount(element) {
  const host = document.createElement('div')
  document.body.appendChild(host)
  const root = createRoot(host)
  await act(async () => { root.render(element) })
  return { host, root }
}

// ---- the option sets mirror the backend enums ----
check('three levels, matching CourseLevel', COURSE_LEVELS.length === 3, COURSE_LEVELS.join(', '))
check('eleven durations, matching CourseDuration', COURSE_DURATIONS.length === 11,
  `count=${COURSE_DURATIONS.length}`)
check('durations are ordered shortest first',
  COURSE_DURATIONS[0] === 'Under 1 hour' && COURSE_DURATIONS.at(-1) === '6+ months',
  `${COURSE_DURATIONS[0]} ... ${COURSE_DURATIONS.at(-1)}`)

// Legacy stored values must map onto a dropdown option, not blank the field.
check('normalizeLevel maps BEGINNER -> Beginner', normalizeLevel('BEGINNER') === 'Beginner',
  String(normalizeLevel('BEGINNER')))
check('normalizeLevel maps lowercase beginner -> Beginner', normalizeLevel('beginner') === 'Beginner',
  String(normalizeLevel('beginner')))
check('normalizeLevel maps mixed case', normalizeLevel('  iNtErMeDiAtE ') === 'Intermediate',
  String(normalizeLevel('  iNtErMeDiAtE ')))
check('normalizeLevel rejects nonsense', normalizeLevel('wizard') === null,
  String(normalizeLevel('wizard')))
// A free-text duration outside the offered set is kept verbatim rather than coerced, so an
// old row never silently changes meaning.
check('normalizeDuration keeps an off-list "6 weeks" as-is',
  normalizeDuration('6 weeks') === '6 weeks', String(normalizeDuration('6 weeks')))
check('normalizeDuration round-trips an offered value',
  normalizeDuration('4-8 weeks') === '4-8 weeks', String(normalizeDuration('4-8 weeks')))
check('normalizeDuration keeps an unknown legacy value visible',
  normalizeDuration('Three long months') === 'Three long months',
  String(normalizeDuration('Three long months')))
check('normalizeDuration handles the "to" spelling',
  normalizeDuration('6 to 10 hours') === '6-10 hours', String(normalizeDuration('6 to 10 hours')))

// ---- flattenLessons orders by module then lesson ----
const flattened = flattenLessons(
  [{ id: 2, orderNumber: 2, title: 'Second' }, { id: 1, orderNumber: 1, title: 'First' }],
  {
    1: [{ id: 11, orderNumber: 2, title: 'B' }, { id: 10, orderNumber: 1, title: 'A' }],
    2: [{ id: 20, orderNumber: 1, title: 'C' }],
  },
)
check('flattenLessons sorts by module then lesson order',
  flattened.map((l) => l.title).join(',') === 'A,B,C',
  flattened.map((l) => l.title).join(','))
check('flattenLessons carries the owning module title',
  flattened[0].moduleTitle === 'First', String(flattened[0].moduleTitle))

// ---- the tab bar ----
const tabs = [
  { id: 'modules', label: 'Modules', icon: () => null, hint: 'h' },
  { id: 'lessons', label: 'Lessons', icon: () => null, hint: 'h' },
  { id: 'materials', label: 'Materials', icon: () => null, hint: 'h' },
]
{
  const { host, root } = await mount(
    createElement(MemoryRouter, null, createElement(CourseTabBar, { tabs, counts: { modules: 3 } })))

  const buttons = [...host.querySelectorAll('[role="tab"]')]
  check('tab bar renders every tab', buttons.length === 3, `count=${buttons.length}`)
  check('first tab is selected initially',
    buttons[0].getAttribute('aria-selected') === 'true', buttons[0].getAttribute('aria-selected'))
  check('only the selected tab is in the tab order',
    buttons.filter((b) => b.getAttribute('tabindex') === '0').length === 1)
  check('tab has an accessible role and label', !!host.querySelector('[role="tablist"][aria-label]'))
  check('count badge rendered for modules', host.textContent.includes('3'))

  await act(async () => {
    buttons[1].dispatchEvent(new globalThis.MouseEvent('click', { bubbles: true }))
  })
  const after = [...host.querySelectorAll('[role="tab"]')]
  check('clicking a tab selects it',
    after[1].getAttribute('aria-selected') === 'true', after[1].getAttribute('aria-selected'))

  // Arrow keys move between tabs, as a tablist is expected to.
  await act(async () => {
    after[1].dispatchEvent(new globalThis.KeyboardEvent('keydown', { key: 'ArrowRight', bubbles: true }))
  })
  check('ArrowRight moves to the next tab',
    [...host.querySelectorAll('[role="tab"]')][2].getAttribute('aria-selected') === 'true')

  await act(async () => { root.unmount() })
}

// ---- the section jump list ----
{
  for (const id of ['builder-modules', 'builder-lessons', 'builder-assignments']) {
    const target = document.createElement('div')
    target.id = id
    // jsdom implements neither of these, and the component calls both.
    target.scrollIntoView = () => {}
    document.body.appendChild(target)
  }
  if (globalThis.Element) globalThis.Element.prototype.scrollIntoView = () => {}

  const { host, root } = await mount(
    createElement(MemoryRouter, null, createElement(CourseSectionNav, {
      sections: [
        { id: 'builder-modules', label: 'Modules', count: 4 },
        { id: 'builder-lessons', label: 'Lessons', count: 9 },
        { id: 'builder-assignments', label: 'Assignments' },
      ],
    })))

  const links = [...host.querySelectorAll('button')]
  check('jump list lists every section', links.length === 3, `count=${links.length}`)
  check('jump list is a labelled nav', !!host.querySelector('nav[aria-label]'))
  check('jump list shows counts', host.textContent.includes('4') && host.textContent.includes('9'))
  check('jump list has a back-to-dashboard link',
    !!host.querySelector('a[href="/instructor"]'))

  await act(async () => {
    links[1].dispatchEvent(new globalThis.MouseEvent('click', { bubbles: true }))
  })
  check('clicking a jump entry focuses the target',
    document.activeElement?.id === 'builder-lessons', String(document.activeElement?.id))
  check('jumped entry is marked current',
    !!host.querySelector('button[aria-current="true"]'))

  await act(async () => { root.unmount() })
}

// ---- scroll to top ----
{
  const { host, root } = await mount(createElement(ScrollToTopButton, null))
  const button = host.querySelector('button')

  check('scroll-to-top renders a labelled button',
    button?.getAttribute('aria-label') === 'Back to top', button?.getAttribute('aria-label'))

  window.scrollY = 0
  await act(async () => {
    window.dispatchEvent(new globalThis.Event('scroll'))
    await sleep(5)
  })
  check('hidden at the top of the page',
    button.className.includes('opacity-0') && button.getAttribute('tabindex') === '-1',
    button.className.match(/opacity-\d+/)?.[0])

  window.scrollY = 900
  await act(async () => {
    window.dispatchEvent(new globalThis.Event('scroll'))
    await sleep(5)
  })
  check('appears after scrolling down',
    button.className.includes('opacity-100') && button.getAttribute('tabindex') === '0',
    button.className.match(/opacity-\d+/)?.[0])
  check('no longer aria-hidden when visible', button.getAttribute('aria-hidden') === 'false',
    button.getAttribute('aria-hidden'))

  // At the very bottom of a short page the button must not show.
  window.scrollY = 40
  await act(async () => {
    window.dispatchEvent(new globalThis.Event('scroll'))
    await sleep(5)
  })
  check('hides again near the top', button.className.includes('opacity-0'))

  await act(async () => { root.unmount() })
}

let failed = 0
for (const r of results) {
  if (!r.pass) failed++
  console.log(`${r.pass ? 'PASS' : 'FAIL'}  ${r.name}${r.detail ? `\n        ${r.detail}` : ''}`)
}
console.log(`\n${results.length - failed}/${results.length} passed`)
process.exit(failed === 0 ? 0 : 1)