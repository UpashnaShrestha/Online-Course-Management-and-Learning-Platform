import { useEffect, useState } from 'react'

/**
 * Reactive `window.matchMedia` query.
 *
 * Reading matchMedia once during render is not enough: a user resizing the window across
 * the breakpoint would keep the old layout until something else happened to re-render.
 * This subscribes to the change event so the layout follows the viewport.
 *
 * @param {string} query e.g. '(min-width: 1024px)'
 * @returns {boolean} whether the query currently matches
 */
export function useMediaQuery(query) {
  const [matches, setMatches] = useState(() => {
    if (typeof window === 'undefined' || !window.matchMedia) return false
    return window.matchMedia(query).matches
  })

  useEffect(() => {
    if (typeof window === 'undefined' || !window.matchMedia) return undefined

    const list = window.matchMedia(query)
    const onChange = (event) => setMatches(event.matches)

    // Re-read in case the viewport changed between the initial render and this effect.
    setMatches(list.matches)
    list.addEventListener('change', onChange)

    return () => list.removeEventListener('change', onChange)
  }, [query])

  return matches
}

/** The 1024px breakpoint Tailwind's `lg:` prefix is generated from. */
export const DESKTOP_QUERY = '(min-width: 1024px)'

/** Convenience wrapper: is the viewport at least `lg`? */
export function useIsDesktop() {
  return useMediaQuery(DESKTOP_QUERY)
}