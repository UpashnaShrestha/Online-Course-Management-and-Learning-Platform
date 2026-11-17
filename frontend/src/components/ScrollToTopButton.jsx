/**
 * Floating "back to top" control.
 *
 * Mounted once in App so it works on every route. Two behaviours matter:
 *  - it only appears once the page is actually scrolled, so it does not float over the
 *    hero section of a short page;
 *  - it appears after the user has scrolled a meaningful distance, not immediately.
 *
 * Uses a scroll listener rather than IntersectionObserver on a sentinel because the sentinel
 * approach needs an extra element and behaves oddly on pages shorter than the viewport.
 */
import { useCallback, useEffect, useState } from 'react'
import { ArrowUp } from 'lucide-react'

/** Pixels of scroll before the button appears. */
const SHOW_AFTER = 320

export default function ScrollToTopButton({ showAfter = SHOW_AFTER }) {
  const [visible, setVisible] = useState(false)

  useEffect(() => {
    // Called on every scroll, but only sets state when the threshold is crossed, so this
    // does not cause a re-render per pixel.
    const onScroll = () => setVisible(window.scrollY > showAfter)

    onScroll()
    window.addEventListener('scroll', onScroll, { passive: true })
    return () => window.removeEventListener('scroll', onScroll)
  }, [showAfter])

  const scrollToTop = useCallback(() => {
    // Respect a user's reduced-motion preference rather than always animating.
    const reduced = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches

    window.scrollTo({ top: 0, left: 0, behavior: reduced ? 'auto' : 'smooth' })

    // Return focus to the top of the document so keyboard and screen-reader users are not
    // left focused on a button that has just scrolled out of view.
    const main = document.querySelector('main, h1')
    if (main) {
      if (!main.hasAttribute('tabindex')) main.setAttribute('tabindex', '-1')
      main.focus({ preventScroll: true })
    }
  }, [])

  return (
    <button
      type="button"
      onClick={scrollToTop}
      aria-label="Back to top"
      title="Back to top"
      // Hidden from assistive tech as well as visually, so it is not announced while idle.
      aria-hidden={!visible}
      tabIndex={visible ? 0 : -1}
      className={`fixed bottom-6 right-6 z-30 flex h-11 w-11 items-center justify-center rounded-full bg-[#f45135] text-white shadow-xl shadow-black/25 transition duration-300 hover:bg-[#ff694d] focus-visible:outline focus-visible:outline-4 focus-visible:outline-offset-2 focus-visible:outline-[#f45135] ${
        visible ? 'translate-y-0 opacity-100' : 'pointer-events-none translate-y-3 opacity-0'
      }`}
    >
      <ArrowUp size={20} aria-hidden="true" />
    </button>
  )
}