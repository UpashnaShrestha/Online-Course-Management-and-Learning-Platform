/**
 * Confirms the real CourseCard renders the corrected prices with no leaked comment.
 * Runs the actual component, not a reimplementation of the formatter.
 */
import { createRoot } from 'react-dom/client'
import { act, createElement } from 'react'
import { MemoryRouter } from 'react-router-dom'
import CourseCard from '../src/components/CourseCard'

const courses = [
  // level/duration deliberately use the legacy stored forms, to prove they are normalised.
  { id: 9, title: 'Digital Marketing Basics', level: 'BEGINNER', duration: '5 weeks', price: 199,
    description: 'Learn SEO, social media marketing, content strategy and digital advertising fundamentals.',
    category: { name: 'Marketing' }, instructors: [{ firstName: 'Aarav', lastName: 'Shrestha' }], duration: '6 weeks' },
  { id: 1, title: 'Java Programming Fundamentals', level: 'BEGINNER', price: 250 },
  { id: 3, title: 'React for Beginners', level: 'BEGINNER', price: 440 },
  { id: 2, title: 'Spring Boot Backend Development', level: 'INTERMEDIATE', price: 650 },
  { id: 7, title: 'React Native Mobile Development', level: 'INTERMEDIATE', price: 750 },
  { id: 10, title: 'Python', level: 'BEGINNER', price: 0 },
  // A large amount, to prove lakh/crore grouping still holds for NPR.
  { id: 99, title: 'Advanced Cloud Architecture', level: 'ADVANCED', price: 125000 },
]

const host = document.createElement('div')
document.body.appendChild(host)
const root = createRoot(host)

await act(async () => {
  // CourseCard renders a <Link>, so it needs router context.
  root.render(
    createElement(MemoryRouter, { initialEntries: ['/courses'] },
      createElement('div', null,
        courses.map((c) => createElement(CourseCard, { key: c.id, course: c })))))
})

const cards = [...host.querySelectorAll('article')]
const text = host.textContent

const results = []
const check = (name, pass, detail = '') => results.push({ name, pass, detail })

check('every course rendered a card', cards.length === courses.length,
  `cards=${cards.length} courses=${courses.length}`)

// The reported bug: the CSS note was rendered as visible page text.
for (const leak of ['Underscores become spaces', 'calc(100%-11rem)', 'silently dropped by CSS', '/*', '*/']) {
  check(`no leaked source text: "${leak}"`, !text.includes(leak))
}

const priceOf = (title) => {
  const card = cards.find((c) => c.textContent.includes(title))
  return card?.textContent.match(/Rs\.[\d,]+/)?.[0]
}

for (const [title, expected] of [
  ['Digital Marketing Basics', 'Rs.199'],
  ['Java Programming Fundamentals', 'Rs.250'],
  ['React for Beginners', 'Rs.440'],
  ['Spring Boot Backend Development', 'Rs.650'],
  ['React Native Mobile Development', 'Rs.750'],
  ['Python', 'Rs.0'],
  ['Advanced Cloud Architecture', 'Rs.1,25,000'],
]) {
  const actual = priceOf(title)
  check(`${title} renders ${expected}`, actual === expected, `got ${actual}`)
}

// Whole rupees only: no ".99" or any decimal should survive on a card.
check('no decimal/.99 prices rendered', !/Rs\.\d+\.\d+/.test(text),
  (text.match(/Rs\.[\d,.]+/g) ?? []).join(' '))

// Levels render as display labels, never as raw enum names.
check('levels render as display labels, not enum names',
  /Beginner/.test(text) && !/\bBEGINNER\b/.test(text),
  (text.match(/\b(BEGINNER|INTERMEDIATE|ADVANCED)\b/g) ?? []).join(',') || 'no raw enum names')

let failed = 0
for (const r of results) {
  if (!r.pass) failed++
  console.log(`${r.pass ? 'PASS' : 'FAIL'}  ${r.name}${r.detail ? `\n        ${r.detail}` : ''}`)
}
console.log(`\n${results.length - failed}/${results.length} passed`)
process.exit(failed === 0 ? 0 : 1)