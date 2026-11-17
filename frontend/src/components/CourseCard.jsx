import { Link } from 'react-router-dom'
import { formatNpr } from '../utils/format'
import { levelLabel, normalizeDuration } from '../utils/courseOptions'

export default function CourseCard({ course, compact = false }) {
  if (!course) {
    return <p>Course data is not available.</p>
  }

  const instructor = course.instructors?.[0]
  const instructorName = instructor ? `${instructor.firstName} ${instructor.lastName}` : 'Course instructor'

  return (
    <article className={`group overflow-hidden rounded-2xl border border-slate-200/80 bg-white shadow-xl shadow-slate-950/10 transition duration-300 hover:-translate-y-1 hover:shadow-2xl ${compact ? '' : 'h-full'}`}>
      <div className="relative h-44 overflow-hidden bg-gradient-to-br from-[#0b1221] via-[#1f3c78] to-[#4b68a8]">
        {course.thumbnail ? <img src={course.thumbnail} alt="" className="h-full w-full object-cover transition duration-500 group-hover:scale-105" /> : <div className="absolute inset-0 bg-[radial-gradient(circle_at_20%_20%,rgba(255,255,255,0.28),transparent_30%),linear-gradient(135deg,transparent_45%,rgba(255,255,255,0.12)_45%,transparent_70%)]" />}
        <span className="absolute left-4 top-4 rounded-full bg-[#fff5d6] px-3 py-1 text-xs font-bold text-[#0f172a]">{course.category?.name || 'General'}</span>
      </div>
      {/* Underscores become spaces so calc() is valid; `h-[calc(100%-11rem)]` is silently dropped by CSS. */}
      <div className="flex h-[calc(100%_-_11rem)] flex-col p-5">
        <div className="flex items-center justify-between gap-3 text-sm text-slate-500">
          <span>{levelLabel(course.level) || 'All levels'}</span>
          <span className="font-bold text-slate-900">{formatNpr(course.price)}</span>
        </div>
        <h2 className="mt-3 text-xl font-bold leading-tight text-slate-950">{course.title}</h2>
        <p className="mt-2 line-clamp-3 text-sm leading-6 text-slate-600">{course.description || 'Explore practical lessons and build skills through guided learning.'}</p>
        <div className="mt-auto flex items-end justify-between gap-3 pt-5">
          <div>
            <p className="text-sm font-semibold text-slate-900">{instructorName}</p>
            <p className="mt-1 text-xs text-slate-500">{normalizeDuration(course.duration) || 'Self-paced'}</p>
          </div>
          <Link to={`/courses/${course.id}`} className="coral-action px-4 py-2">
            View course
          </Link>
        </div>
      </div>
    </article>
  )
}
