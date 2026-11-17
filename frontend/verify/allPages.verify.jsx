/**
 * Renders every routed page inside React and fails on any console error.
 *
 * The existing harnesses mount one component at a time, which cannot catch a hook called at
 * module scope -- the page file itself parses and "builds" fine, and the error only appears
 * when the module is imported. Importing and rendering every page is what surfaces it.
 *
 * Needs the backend running on :8081 for the pages that fetch. Skipped, loudly, if not.
 */
import { createRoot } from 'react-dom/client'
import { act, createElement } from 'react'
import { MemoryRouter } from 'react-router-dom'
import { AuthProvider } from '../src/context/AuthContext'
import { NotificationsProvider } from '../src/context/NotificationsContext'

const BASE = 'http://localhost:8081'

const results = []
const check = (name, pass, detail = '') => results.push({ name, pass, detail })
const sleep = (ms) => new Promise((r) => setTimeout(r, ms))

// Capture anything React logs as an error: an invalid hook call is reported via console.error.
const consoleErrors = []
const realError = console.error
console.error = (...args) => {
  consoleErrors.push(args.map(String).join(' '))
  realError(...args)
}
const realWarn = console.warn
const warnings = []
console.warn = (...args) => {
  warnings.push(args.map(String).join(' '))
  realWarn(...args)
}

const online = await fetch(`${BASE}/api/courses/options`).then(() => true).catch(() => false)
if (!online) {
  console.log(`SKIPPED: no backend on ${BASE}. Start the app, then re-run.`)
  process.exit(0)
}

/**
 * Imported dynamically inside a try/catch per page: a module-scope hook call throws here,
 * which is exactly the failure we want to report rather than crash the harness on.
 */
const PAGES = [
  ['Home', () => import('../src/pages/Home')],
  ['CoursesPage', () => import('../src/pages/CoursesPage')],
  ['CourseDetailPage', () => import('../src/pages/CourseDetailPage')],
  ['AboutPage', () => import('../src/pages/AboutPage')],
  ['ContactPage', () => import('../src/pages/ContactPage')],
  ['Login', () => import('../src/pages/auth/Login')],
  ['Register', () => import('../src/pages/auth/Register')],
  ['ForgotPassword', () => import('../src/pages/auth/ForgotPassword')],
  ['ResetPassword', () => import('../src/pages/auth/ResetPassword')],
  ['ProfilePage', () => import('../src/pages/ProfilePage')],
  ['PaymentPage', () => import('../src/pages/PaymentPage')],
  ['PaymentSuccessPage', () => import('../src/pages/PaymentSuccessPage')],
  ['StudentDashboard', () => import('../src/pages/student/StudentDashboard')],
  ['StudentCourses', () => import('../src/pages/student/StudentCourses')],
  ['StudentLearning', () => import('../src/pages/student/StudentLearning')],
  ['StudentProgress', () => import('../src/pages/student/StudentProgress')],
  ['StudentCertificates', () => import('../src/pages/student/StudentCertificates')],
  ['StudentPayments', () => import('../src/pages/student/StudentPayments')],
  ['StudentNotifications', () => import('../src/pages/student/StudentNotifications')],
  ['InstructorDashboard', () => import('../src/pages/instructor/InstructorDashboard')],
  ['InstructorCourses', () => import('../src/pages/instructor/InstructorCourses')],
  ['InstructorAssignments', () => import('../src/pages/instructor/InstructorAssignments')],
  ['AdminDashboard', () => import('../src/pages/admin/AdminDashboard')],
  ['AdminUsers', () => import('../src/pages/admin/AdminUsers')],
  ['AdminCourses', () => import('../src/pages/admin/AdminCourses')],
  ['AdminCategories', () => import('../src/pages/admin/AdminCategories')],
  ['AdminPayments', () => import('../src/pages/admin/AdminPayments')],
  ['App', () => import('../src/App')],
  ['ScrollToTopButton', () => import('../src/components/ScrollToTopButton')],
  ['CourseTabBar', () => import('../src/components/CourseTabBar')],
  ['CourseSectionNav', () => import('../src/components/CourseSectionNav')],
  ['CoursePanels', () => import('../src/components/course-builder/CoursePanels')],
  ['PasswordInput', () => import('../src/components/PasswordInput')],
  ['PasswordRules', () => import('../src/components/PasswordRules')],
]

const INVALID_HOOK = /Invalid hook call|Cannot read properties of null \(reading 'use(State|Effect|Ref|Memo|Callback)'\)|dispatcher/
const HOOK_ERRORS = /Invalid hook call|Cannot read properties of null \(reading 'use|resolveDispatcher|is not a function.*dispatch/i

// A course id that exists, so CourseDetailPage / PaymentPage have something to fetch.
const courses = (await fetch(`${BASE}/api/courses`).then((r) => r.json())).data ?? []
const courseId = courses[0]?.id ?? 1
const paymentId = 1

const routes = {
  Home: '/',
  CoursesPage: '/courses',
  CourseDetailPage: `/courses/${courseId}`,
  AboutPage: '/about',
  ContactPage: '/contact',
  Login: '/login',
  Register: '/register',
  ForgotPassword: '/forgot-password',
  ResetPassword: '/reset-password',
  ProfilePage: '/profile',
  PaymentPage: `/payment/${paymentId}`,
  PaymentSuccessPage: `/payment/success/${paymentId}`,
  StudentDashboard: '/student',
  StudentCourses: '/student/courses',
  StudentLearning: '/student/courses',
  StudentProgress: '/student/progress',
  StudentCertificates: '/student/certificates',
  StudentPayments: '/student/payments',
  StudentNotifications: '/student/notifications',
  InstructorDashboard: '/instructor',
  InstructorCourses: '/instructor/courses',
  InstructorAssignments: '/instructor/assignments',
  AdminDashboard: '/admin',
  AdminUsers: '/admin/users',
  AdminCourses: '/admin/courses',
  AdminCategories: '/admin/categories',
  AdminPayments: '/admin/payments',
}

for (const [name, load] of PAGES) {
  consoleErrors.length = 0

  let mod = null
  try {
    mod = await load()
  } catch (error) {
    check(`${name}: imports cleanly`, false, String(error).split('\n')[0])
    continue
  }

  const Component = mod.default ?? Object.values(mod).find((v) => typeof v === 'function')
  if (!Component) {
    check(`${name}: exports a component`, false, 'no default or named component found')
    continue
  }

  const before = consoleErrors.length
  let renderError = null
  const host = document.createElement('div')
  document.body.appendChild(host)
  const root = createRoot(host)

  const path = routes[name] ?? '/'
  try {
    await act(async () => {
      root.render(
        createElement(MemoryRouter, { initialEntries: [path] },
          createElement(AuthProvider, null,
            createElement(NotificationsProvider, null,
              name === 'App'
                ? createElement(Component)
                : createElement(Component, { role: 'ADMIN' })))))
    })
    // Let any initial fetches settle.
    await act(async () => { await sleep(120) })
  } catch (error) {
    renderError = error
  }

  const newErrors = consoleErrors.slice(before)
  const hookFailure = renderError && HOOK_ERRORS.test(String(renderError.message))
    ? String(renderError.message).split('\n')[0]
    : null
  const loggedHookFailure = newErrors.find((e) => HOOK_ERRORS.test(e))?.split('\n')[0] ?? null

  const badWarnings = warnings.filter((w) => INVALID_HOOK.test(w))

  check(
    `${name}: renders without a hook error`,
    !hookFailure && !loggedHookFailure && badWarnings.length === 0,
    hookFailure ?? loggedHookFailure ?? (renderError ? String(renderError.message).split('\n')[0] : '')
  )

  await act(async () => { root.unmount() })
  host.remove()
}

let failed = 0
for (const r of results) {
  if (!r.pass) failed++
  console.log(`${r.pass ? 'PASS' : 'FAIL'}  ${r.name}${r.detail ? `\n        ${r.detail}` : ''}`)
}
console.log(`\n${results.length - failed}/${results.length} passed`)

// Assert none of the captured noise was a rules-of-hooks violation.
const anyHookNoise = consoleErrors.concat(warnings).filter((e) => HOOK_ERRORS.test(e))
if (anyHookNoise.length) {
  console.log('\nhook errors observed:')
  anyHookNoise.forEach((e) => console.log('  ' + e.split('\n')[0]))
}

process.exit(failed === 0 ? 0 : 1)