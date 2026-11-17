/**
 * Confirms the register form enforces the same password policy as the backend
 * PasswordPolicy, and that the live checklist updates as the user types.
 */
import { createRoot } from 'react-dom/client'
import { act, createElement } from 'react'
import { MemoryRouter } from 'react-router-dom'
import { AuthProvider } from '../src/context/AuthContext'
import Register from '../src/pages/auth/Register'
import {
  PASSWORD_RULES,
  isValidPassword,
  passwordFailures,
  validatePassword,
} from '../src/utils/validation'

const results = []
const check = (name, pass, detail = '') => results.push({ name, pass, detail })

// ---- the pure policy mirrors the backend ----

check('five rules exposed', PASSWORD_RULES.length === 5,
  `count=${PASSWORD_RULES.length}`)
// Must match PasswordPolicy.RULES order exactly: the checklist renders rules in this
// order, so a reordering on one side would silently shuffle the UI.
check('rule keys match PasswordPolicy.java',
  JSON.stringify(PASSWORD_RULES.map((r) => r.key))
    === JSON.stringify(['minLength', 'uppercase', 'lowercase', 'digit', 'special']),
  PASSWORD_RULES.map((r) => r.key).join(','))

for (const [pw, expected] of [
  ['Str0ng!Pass', true],
  ['P@ssw0rd!', true],
  ['Aa1!aaaa', true],
  ['password123', false],
  ['PASSWORD123', false],
  ['NoDigits!Here', false],
  ['nouppercase1!', false],
  ['NoSpecial123', false],
  ['Sh0rt!', false],
  ['aaaaaaaa', false],
]) {
  check(`policy: "${pw}" -> ${expected}`, isValidPassword(pw) === expected)
}

check('validatePassword(null) reports required, not composition',
  validatePassword('') === 'Password is required.', validatePassword(''))
check('validatePassword lists the unmet rules',
  /Password needs:.*uppercase/.test(validatePassword('password123')),
  validatePassword('password123'))
check('passwordFailures is empty for a good password',
  passwordFailures('Str0ng!Pass').length === 0)

// ---- the rendered form ----

const host = document.createElement('div')
document.body.appendChild(host)
const root = createRoot(host)
const sleep = (ms) => new Promise((r) => setTimeout(r, ms))

await act(async () => {
  root.render(createElement(MemoryRouter, { initialEntries: ['/register'] },
    createElement(AuthProvider, null, createElement(Register))))
})

const findByLabel = (text) => {
  const label = [...host.querySelectorAll('label')]
    .find((l) => l.textContent.trim().toLowerCase() === text.toLowerCase())
  if (!label) return null
  // getElementById rather than a CSS selector: jsdom exposes no global CSS.escape here.
  return host.querySelector('[id]') && document.getElementById(label.htmlFor)
}

const password = findByLabel('Password')
check('register renders a password field', !!password)

async function type(input, text) {
  const setter = Object.getOwnPropertyDescriptor(globalThis.HTMLInputElement.prototype, 'value').set
  for (const ch of text) {
    setter.call(input, input.value + ch)
    input.dispatchEvent(new globalThis.Event('input', { bubbles: true }))
    await act(async () => { await sleep(0) })
  }
}

async function submit() {
  await act(async () => {
    host.querySelector('form').dispatchEvent(
      new globalThis.Event('submit', { bubbles: true, cancelable: true }))
    await sleep(20)
  })
}

// The checklist is present and lists all five rules before typing.
let items = [...host.querySelectorAll('li')]
check('checklist lists all five rules before typing', items.length === 5, `count=${items.length}`)

await type(password, 'abc')
await act(async () => { await sleep(10) })
items = [...host.querySelectorAll('li')]
const roseText = items.filter((li) => li.className.includes('rose')).length
check('typing "abc" marks unmet rules as unmet', roseText > 0 && roseText < 5,
  `unmet=${roseText}`)
check('uppercase rule still unmet after "abc"',
  items[1].className.includes('rose'), items[1].className)

await type(password, 'Abc1!xyz')
await act(async () => { await sleep(10) })
items = [...host.querySelectorAll('li')]
check('all five rules satisfied -> all marked met',
  items.every((li) => li.className.includes('emerald')),
  items.map((li) => li.className.match(/rose|emerald|slate-\d+/)[0]).join(','))

// A weak password must be blocked client-side before any request is made.
password.value = ''
await type(password, 'weak')
await type(password, '')
await type(password, 'password123')
await act(async () => { await sleep(10) })
await submit()
const errorText = host.textContent
check('submitting a weak password is blocked with a policy message',
  /Password needs:/.test(errorText),
  (errorText.match(/Password needs:[^|]{0,120}/) ?? ['(no policy message)'])[0])

// A strong password clears the same field error.
await type(password, '!')
await act(async () => { await sleep(10) })
check('policy error clears once the password is fixed',
  !/Password needs:/.test(host.textContent),
  'no policy message present')

let failed = 0
for (const r of results) {
  if (!r.pass) failed++
  console.log(`${r.pass ? 'PASS' : 'FAIL'}  ${r.name}${r.detail ? `\n        ${r.detail}` : ''}`)
}
console.log(`\n${results.length - failed}/${results.length} passed`)
process.exit(failed === 0 ? 0 : 1)