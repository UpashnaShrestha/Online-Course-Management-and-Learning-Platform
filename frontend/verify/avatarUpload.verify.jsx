/**
 * Covers the image-resize helper and the upload-path resolver.
 *
 * Both exist because of concrete bugs: a phone photo is far too large to upload or to render
 * as an avatar, and a stored "/uploads/..." path silently 404s when the browser resolves it
 * against the Vite dev server instead of the API.
 */
import { createElement, act } from 'react'
import { MemoryRouter } from 'react-router-dom'
import { describeSize, fitWithin } from '../src/utils/imageUpload'
import { resolveImageUrl } from '../src/utils/format'

const results = []
const check = (name, pass, detail = '') => results.push({ name, pass, detail })

// ---- fitWithin: the arithmetic behind the resize ----

check('a large square is scaled to the max edge',
  JSON.stringify(fitWithin(4000, 4000, 512)) === JSON.stringify({ width: 512, height: 512 }),
  JSON.stringify(fitWithin(4000, 4000, 512)))

check('a landscape photo keeps its aspect ratio',
  JSON.stringify(fitWithin(4000, 3000, 512)) === JSON.stringify({ width: 512, height: 384 }),
  JSON.stringify(fitWithin(4000, 3000, 512)))

check('a portrait photo keeps its aspect ratio',
  JSON.stringify(fitWithin(3000, 4000, 512)) === JSON.stringify({ width: 384, height: 512 }),
  JSON.stringify(fitWithin(3000, 4000, 512)))

check('an already-small image is left at its original size',
  JSON.stringify(fitWithin(300, 200, 512)) === JSON.stringify({ width: 300, height: 200 }),
  JSON.stringify(fitWithin(300, 200, 512)))

check('a one-pixel-tall image still yields at least 1px',
  JSON.stringify(fitWithin(4000, 1, 512)) === JSON.stringify({ width: 512, height: 1 }),
  JSON.stringify(fitWithin(4000, 1, 512)))

for (const [w, h] of [[0, 100], [100, 0], [Number.NaN, 10], [-5, -5]]) {
  let threw = false
  try { fitWithin(w, h, 512) } catch { threw = true }
  check(`fitWithin(${w}, ${h}) rejects unusable dimensions`, threw)
}

// ---- describeSize: the "Saved as 512x512, 45 KB" line ----

check('bytes below 1 KB', describeSize(700) === '700 B', describeSize(700))
check('kilobytes are rounded', describeSize(45 * 1024) === '45 KB', describeSize(45 * 1024))
check('megabytes show one decimal', describeSize(3.5 * 1024 * 1024) === '3.5 MB',
  describeSize(3.5 * 1024 * 1024))

// ---- resolveImageUrl: the 404 fix ----

const api = 'http://localhost:8081'

check('a stored upload path is prefixed with the API base URL',
  resolveImageUrl('/uploads/profile-images/x.png') === `${api}/uploads/profile-images/x.png`,
  resolveImageUrl('/uploads/profile-images/x.png'))

check('a path without a leading slash still resolves',
  resolveImageUrl('uploads/x.png') === `${api}/uploads/x.png`,
  resolveImageUrl('uploads/x.png'))

check('an absolute URL is left untouched',
  resolveImageUrl('https://cdn.example.com/a.png') === 'https://cdn.example.com/a.png',
  resolveImageUrl('https://cdn.example.com/a.png'))

check('a protocol-relative URL is left untouched',
  resolveImageUrl('//cdn.example.com/a.png') === '//cdn.example.com/a.png',
  resolveImageUrl('//cdn.example.com/a.png'))

check('a data URL is left untouched',
  resolveImageUrl('data:image/png;base64,AAAA') === 'data:image/png;base64,AAAA',
  resolveImageUrl('data:image/png;base64,AAAA'))

for (const blank of ['', '   ', null, undefined]) {
  check(`blank input yields '' (${JSON.stringify(blank)})`, resolveImageUrl(blank) === '',
    JSON.stringify(resolveImageUrl(blank)))
}

// Sanity: importing react-router keeps the harness honest about the JSX pipeline.
void createElement
void act
void MemoryRouter

let failed = 0
for (const r of results) {
  if (!r.pass) failed++
  console.log(`${r.pass ? 'PASS' : 'FAIL'}  ${r.name}${r.detail ? `\n        ${r.detail}` : ''}`)
}
console.log(`\n${results.length - failed}/${results.length} passed`)
process.exit(failed === 0 ? 0 : 1)