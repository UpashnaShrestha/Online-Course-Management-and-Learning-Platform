/**
 * Tabbed navigation for the course builder.
 *
 * The builder previously stacked modules, lessons, materials and assignments into one very
 * long column, so finding one of them meant scrolling past the others. These tabs scope the
 * panel to one job and make each area reachable in a single click.
 *
 * The tab strip is horizontal on desktop and scrolls on narrow screens, so it works inside the
 * existing responsive layout without a separate mobile menu.
 */
import { useEffect, useRef, useState } from 'react'
import {
  BookOpen,
  ClipboardCheck,
  FileText,
  Layers,
  LayoutList,
} from 'lucide-react'

/**
 * @typedef {{ id: string, label: string, icon: React.ComponentType, hint: string }} CourseTab
 */

/**
 * @type {CourseTab[]}
 * `count` is filled in by the caller when the area has a known total, so the tab can show
 * how much content it holds without the panel having to be visited first.
 */
export const COURSE_TABS = [
  { id: 'modules', label: 'Modules', icon: Layers, hint: 'Group lessons into sections' },
  { id: 'lessons', label: 'Lessons', icon: BookOpen, hint: 'The individual teaching units' },
  { id: 'materials', label: 'Materials', icon: FileText, hint: 'Downloads and reference files' },
  { id: 'assignments', label: 'Assignments', icon: ClipboardCheck, hint: 'Graded work for students' },
  { id: 'outline', label: 'Outline', icon: LayoutList, hint: 'Everything at a glance' },
]

/**
 * @param {CourseTab[]} tabs
 * @param {Record<string, number>} [counts]
 * @param {string} [ariaLabel]
 */
export default function CourseTabBar({ tabs = COURSE_TABS, counts = {}, ariaLabel = 'Course sections' }) {
  const [active, setActive] = useState(tabs[0]?.id)
  const stripRef = useRef(null)

  // Follow the caller: when a different course is selected, reset to the first tab rather
  // than leaving the user on a tab that may not apply to the new course.
  useEffect(() => {
    setActive(tabs[0]?.id)
  }, [tabs])

  const move = (index) => {
    const next = tabs[(index + tabs.length) % tabs.length]
    if (next) setActive(next.id)
  }

  return (
    <div
      role="tablist"
      aria-label={ariaLabel}
      ref={stripRef}
      className="flex gap-1.5 overflow-x-auto border-b border-slate-200 pb-px"
      // Arrow keys move between tabs, which is the expected behaviour for a tablist.
      onKeyDown={(event) => {
        const index = tabs.findIndex((tab) => tab.id === active)
        if (event.key === 'ArrowRight') { event.preventDefault(); move(index + 1) }
        if (event.key === 'ArrowLeft') { event.preventDefault(); move(index - 1) }
        if (event.key === 'Home') { event.preventDefault(); setActive(tabs[0]?.id) }
        if (event.key === 'End') { event.preventDefault(); setActive(tabs.at(-1)?.id) }
      }}
    >
      {tabs.map((tab, index) => {
        const selected = tab.id === active
        const Icon = tab.icon
        const count = counts[tab.id]

        return (
          <button
            key={tab.id}
            type="button"
            role="tab"
            id={`course-tab-${tab.id}`}
            aria-selected={selected}
            aria-controls={`course-panel-${tab.id}`}
            tabIndex={selected ? 0 : -1}
            onClick={() => setActive(tab.id)}
            title={tab.hint}
            className={`flex shrink-0 items-center gap-2 rounded-t-xl border-b-2 px-4 py-2.5 text-sm font-semibold transition ${
              selected
                ? 'border-violet-700 bg-violet-50 text-violet-900'
                : 'border-transparent text-slate-600 hover:bg-slate-50 hover:text-slate-900'
            }`}
          >
            <Icon size={16} aria-hidden="true" />
            {tab.label}
            {count !== undefined && (
              <span
                className={`rounded-full px-1.5 py-0.5 text-xs font-bold ${
                  selected ? 'bg-violet-200 text-violet-900' : 'bg-slate-200 text-slate-700'
                }`}
              >
                {count}
              </span>
            )}
            <span className="sr-only">{tab.hint}</span>
          </button>
        )
      })}
    </div>
  )
}