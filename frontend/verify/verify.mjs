/**
 * Bundles frontend/verify/*.verify.jsx with esbuild and runs it under jsdom.
 *
 * esbuild comes from the project's own devDependencies. jsdom is resolved through
 * JSDOM_DIR so the app's package.json never gains a build-time-only dependency:
 *
 *   npm install --prefix <somewhere> jsdom
 *   JSDOM_DIR=<somewhere> npm run verify
 *
 * (or `npm i -D jsdom` in frontend/ and drop the variable).
 *
 * The app's API base URL defaults to http://localhost:8081, which is where the backend is
 * normally started for verification. Override with VERIFY_API_BASE_URL when it is elsewhere.
 *
 * Usage: node verify.mjs [entry-relative-to-frontend]
 */
import { createRequire } from 'node:module'
import esbuild from 'esbuild'
import { mkdtempSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import path from 'node:path'
import { pathToFileURL, fileURLToPath } from 'node:url'

const HERE = path.dirname(fileURLToPath(import.meta.url))
const FRONTEND = path.resolve(HERE, '..')

// Resolve jsdom from wherever it was installed, so it never becomes a project dependency.
const jsdomRequire = createRequire(
  process.env.JSDOM_DIR ? path.join(process.env.JSDOM_DIR, 'noop.cjs') : path.join(HERE, 'noop.cjs'))
const { JSDOM } = jsdomRequire('jsdom')
const entry = process.argv[2] ?? 'verify/authForms.verify.jsx'
const outDir = mkdtempSync(path.join(tmpdir(), 'cf-verify-'))
const outFile = path.join(outDir, 'bundle.mjs')

// jsdom must exist in the DOM before React is imported.
// The document origin must look like the Vite dev server. The app's API calls go through
// axios in jsdom, which enforces CORS like a browser does -- so an origin the backend has not
// been told about makes every request fail, and the pages quietly render their empty states.
const ORIGIN = process.env.VERIFY_PAGE_ORIGIN ?? 'http://localhost:5173'

const dom = new JSDOM('<!doctype html><html><body></body></html>', {
  url: `${ORIGIN}/`,
  pretendToBeVisual: true,
})
for (const key of [
  'window', 'document', 'navigator', 'location', 'history', 'HTMLElement', 'HTMLInputElement',
  'HTMLFormElement', 'HTMLSelectElement', 'HTMLOptionElement', 'HTMLButtonElement',
  'HTMLAnchorElement', 'HTMLImageElement', 'SVGElement', 'Node', 'NodeList', 'Text', 'Comment',
  'Event', 'CustomEvent', 'MouseEvent', 'KeyboardEvent', 'FocusEvent', 'InputEvent',
  'getComputedStyle', 'requestAnimationFrame', 'cancelAnimationFrame', 'localStorage',
  'sessionStorage', 'IntersectionObserver', 'MutationObserver', 'ResizeObserver',
  'DOMParser', 'XMLSerializer', 'AbortController', 'URL', 'URLSearchParams', 'FormData', 'Blob',
  'CSS', 'matchMedia', 'fetch', 'Headers', 'Request', 'Response', 'TextEncoder', 'TextDecoder',
  // axios's browser adapter reads the global XMLHttpRequest. Without this it has no transport
  // at all and every request hangs forever, which looks exactly like an empty page.
  'XMLHttpRequest',
]) {
  // Some globals (navigator) are getter-only on modern node, so define instead of assign.
  if (dom.window[key] === undefined) continue
  try {
    globalThis[key] = dom.window[key]
  } catch {
    Object.defineProperty(globalThis, key, { value: dom.window[key], configurable: true })
  }
}
if (!dom.window.matchMedia) {
  dom.window.matchMedia = (query) => ({
    matches: false, media: query, onchange: null,
    addEventListener() {}, removeEventListener() {},
    addListener() {}, removeListener() {}, dispatchEvent() { return false },
  })
  globalThis.matchMedia = dom.window.matchMedia
}
// jsdom implements neither scrollIntoView nor getBoundingClientRect sizes, and components
// call all three; stub them before anything renders.
if (globalThis.Element) {
  globalThis.Element.prototype.scrollIntoView = function scrollIntoView() {}
  globalThis.Element.prototype.scrollTo = function scrollTo() {}
  globalThis.Element.prototype.setSelectionRange = function setSelectionRange() {}
}

globalThis.IS_REACT_ACT_ENVIRONMENT = true

// esbuild resolves react through the `import` condition, which points at the production
// build, and the production `act()` does not batch -- every keystroke then logs React's
// "update was not wrapped in act" advisory. The assertions are unaffected, so the noise is
// filtered rather than papered over with a real test runner.
const realConsoleError = console.error
console.error = (...args) => {
  if (typeof args[0] === 'string' && args[0].includes('not wrapped in act')) return
  realConsoleError(...args)
}

await esbuild.build({
  entryPoints: [path.join(FRONTEND, entry)],
  bundle: true,
  format: 'esm',
  platform: 'browser',
  jsx: 'automatic',
  outfile: outFile,
  // Point the app's axios instance at the port the harness itself checked. Deriving it
  // matters: baking in a fixed port means every page fetches a dead address when the
  // backend is running elsewhere, and the pages silently render their empty states.
  define: {
    'process.env.NODE_ENV': '"development"',
    'import.meta.env.VITE_API_BASE_URL': JSON.stringify(
      process.env.VERIFY_API_BASE_URL ?? 'http://localhost:8081'),
  },
  logLevel: 'warning',
})

let exitCode = 1
try {
  await import(pathToFileURL(outFile).href)
  exitCode = 0
} catch (error) {
  if (error && typeof error.code === 'number') exitCode = error.code
  else { console.error(error); exitCode = 2 }
} finally {
  rmSync(outDir, { recursive: true, force: true })
}
process.exit(exitCode)