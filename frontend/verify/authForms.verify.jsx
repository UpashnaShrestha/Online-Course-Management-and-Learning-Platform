/**
 * Interaction harness for the auth forms.
 *
 * There is no test framework in this repo, so this is a standalone script: it renders the
 * real components into jsdom and dispatches real change events. It exists to catch the
 * class of bug where a controlled input is wired to an `event.target.name` handler but the
 * field renders without a `name` attribute -- every keystroke then writes to an empty-string
 * key and the input stays blank. `vite build` cannot see that; only real events can.
 */
import { createRoot } from 'react-dom/client'
import { act, createElement } from 'react'
import { MemoryRouter } from 'react-router-dom'
import { AuthProvider } from '../src/context/AuthContext'
import Login from '../src/pages/auth/Login'
import ResetPassword from '../src/pages/auth/ResetPassword'

const sleep = (ms) => new Promise((r) => setTimeout(r, ms))

/** Types `text` one character at a time so React sees a real change per keystroke. */
async function typeInto(input, text) {
  for (const ch of text) {
    const setter = Object.getOwnPropertyDescriptor(
      globalThis.HTMLInputElement.prototype, 'value').set
    setter.call(input, input.value + ch)
    input.dispatchEvent(new globalThis.Event('input', { bubbles: true }))
    await act(async () => { await sleep(0) })
  }
}

function findByLabel(text) {
  const label = [...document.querySelectorAll('label')]
    .find((l) => l.textContent.trim().toLowerCase() === text.toLowerCase())
  if (!label) throw new Error(`no label "${text}"`)
  return document.getElementById(label.htmlFor)
}

async function mount(element) {
  const host = document.createElement('div')
  document.body.appendChild(host)
  const root = createRoot(host)
  await act(async () => { root.render(element) })
  return root
}

const results = []
function check(name, pass, detail = '') {
  results.push({ name, pass, detail })
}

async function testLogin() {
  const root = await mount(createElement(MemoryRouter, { initialEntries: ['/login'] },
    createElement(AuthProvider, null, createElement(Login))))

  const password = findByLabel('Password')
  check('Login: password field exists', !!password)
  check('Login: password field has a name attribute',
    password.getAttribute('name') === 'password',
    `name="${password.getAttribute('name')}"`)

  await typeInto(password, 'secret123')

  check('Login: typed value reaches state (input not stuck blank)',
    password.value === 'secret123',
    `value="${password.value}"`)

  const toggle = document.querySelector('button[aria-label^="Show password"], button[aria-label^="Hide password"]')
  check('Login: visibility toggle present', !!toggle)
  const before = password.getAttribute('type')
  await act(async () => { toggle.dispatchEvent(new globalThis.MouseEvent('click', { bubbles: true })) })
  check('Login: toggle flips input type',
    password.getAttribute('type') !== before,
    `${before} -> ${password.getAttribute('type')}`)

  await act(async () => { root.unmount() })
}

/** Submits a form and flushes the async submit handler before asserting. */
async function submit(form) {
  await act(async () => {
    form.dispatchEvent(new globalThis.Event('submit', { bubbles: true, cancelable: true }))
    await sleep(10)
  })
}

/** Every visible message on the page, joined -- used to assert on validation copy. */
function visibleText() {
  return [...document.querySelectorAll('p, span, li')].map((el) => el.textContent).join(' | ')
}

async function testResetPassword() {
  const root = await mount(createElement(MemoryRouter, { initialEntries: ['/reset-password'] },
    createElement(AuthProvider, null, createElement(ResetPassword))))

  const otp = findByLabel('One-time code')
  check('Reset: OTP maxLength is 6', otp.getAttribute('maxlength') === '6',
    `maxlength=${otp.getAttribute('maxlength')}`)

  await typeInto(otp, '12345')
  await submit(otp.closest('form'))
  check('Reset: 5-digit OTP rejected with a 6-digit message',
    visibleText().includes('6-digit'), visibleText().slice(0, 200))

  await typeInto(otp, '6')
  check('Reset: OTP accepts the 6th digit', otp.value === '123456', `value="${otp.value}"`)

  await typeInto(findByLabel('New password'), 'short')
  await submit(document.querySelector('form'))
  check('Reset: short password rejected with the 8-character rule',
    visibleText().includes('At least 8 characters'), visibleText().slice(0, 240))

  // The whole policy is enforced, not just the length: "Str0ng!Pass" is the minimum bar.
  const strong = findByLabel('New password')
  await typeInto(strong, 'Str0ng!Pass')
  const allRulesText = visibleText()
  check('Reset: a fully compliant password clears the policy error',
    !allRulesText.includes('Password needs:'), allRulesText.slice(0, 200))

  await act(async () => { root.unmount() })
}

await testLogin()
await testResetPassword()

let failed = 0
for (const r of results) {
  if (!r.pass) failed++
  console.log(`${r.pass ? 'PASS' : 'FAIL'}  ${r.name}${r.detail ? `\n        ${r.detail}` : ''}`)
}
console.log(`\n${results.length - failed}/${results.length} passed`)
process.exit(failed === 0 ? 0 : 1)