import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { ChevronUp } from 'lucide-react'

/**
 * In-page table of contents for the course builder.
 *
 * The builder has several distinct jobs (course details, modules, lessons, materials,
 * assignments, outline). After the tab bar was added they are one click away, but a page
 * this long still benefits from a persistent jump list, and it keeps every area listed even
 * while another tab is open.
 *
 * Scrolls the target into view under the sticky header instead of using hash links, which
 * the router would otherwise treat as navigation.
 */
export default function CourseSectionNav({ sections, className = '' }) {
  const [activeId, setActiveId] = useState(sections[0]?.id ?? null)

  // Highlight whichever section is currently in view. Uses a scroll listener rather than
  // IntersectionObserver because these panels are conditionally rendered, so there is no
  // stable sentinel element to observe.
  useEffect(() => {
    const onScroll = () => {
      const current = sections
        .map((section) => ({ section, top: document.getElementById(section.id)?.getBoundingClientRect().top }))
        .filter((entry) => typeof entry.top === 'number')
        .sort((a, b) => Math.abs(a.top) - Math.abs(b.top))[0]

      if (current) setActiveId(current.section.id)
    }

    window.addEventListener('scroll', onScroll, { passive: true })
    return () => window.removeEventListener('scroll', onScroll)
  }, [sections])

  const jump = (id) => {
    const target = document.getElementById(id)
    if (!target) return

    const reduced = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches
    target.scrollIntoView({ behavior: reduced ? 'auto' : 'smooth', block: 'start' })
    setActiveId(id)

    // Move focus so keyboard users land on the section they jumped to.
    if (!target.hasAttribute('tabindex')) target.setAttribute('tabindex', '-1')
    target.focus({ preventScroll: true })
  }

  return (
    <nav aria-label="Course sections" className={className}>
      <h2 className="text-xs font-bold uppercase tracking-[0.16em] text-slate-500">Jump to</h2>

      <ul className="mt-3 space-y-1">
        {sections.map((section) => {
          const active = section.id === activeId

          return (
            <li key={section.id}>
              {/*
                A button, not an anchor: these are in-page targets, and navigating to a
                "#id" would push a history entry and break the back button.
              */}
              <button
                type="button"
                onClick={() => jump(section.id)}
                aria-current={active ? 'true' : undefined}
                className={`flex w-full items-center justify-between gap-2 rounded-xl px-3 py-2 text-left text-sm font-medium transition ${
                  active
                    ? 'bg-violet-50 text-violet-900 ring-1 ring-violet-200'
                    : 'text-slate-600 hover:bg-slate-50 hover:text-slate-900'
                }`}
              >
                <span className="truncate">{section.label}</span>
                {section.count !== undefined && (
                  <span className="shrink-0 rounded-full bg-slate-200 px-1.5 py-0.5 text-xs font-bold text-slate-700">
                    {section.count}
                  </span>
                )}
              </button>
            </li>
          )
        })}
      </ul>

      {/*
        The builder's own "back" target. Included here so the whole navigation is one
        control rather than a stray link in the page body.
      */}
      <Link
        to="/instructor"
        className="mt-4 flex items-center gap-1.5 text-xs font-semibold text-slate-500 hover:text-slate-900"
      >
        <ChevronUp size={13} aria-hidden="true" className="rotate-180" />
        Back to dashboard
      </Link>
    </nav>
  )
}