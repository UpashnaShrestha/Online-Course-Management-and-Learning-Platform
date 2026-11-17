import { useEffect, useMemo, useState } from 'react'
import { Search } from 'lucide-react'
import api from '../services/api'
import SiteNavbar from '../components/SiteNavbar'
import SiteFooter from '../components/SiteFooter'
import CourseCard from '../components/CourseCard'
import { EmptyState, ErrorState, LoadingState } from '../components/PageState'
import { COURSE_LEVELS, normalizeLevel } from '../utils/courseOptions'

export default function CoursesPage() {
  const [courses, setCourses] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [query, setQuery] = useState('')
  const [level, setLevel] = useState('ALL')
  const [category, setCategory] = useState('ALL')

  const fetchCourses = async () => {
    setLoading(true)
    setError('')
    try {
      const response = await api.get('/api/courses')
      setCourses(response.data?.data ?? [])
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'Please try again in a moment.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchCourses()
  }, [])

  const categories = useMemo(() => [...new Set(courses.map((course) => course.category?.name).filter(Boolean))], [courses])

  // Built from the closed level set rather than from the data, so "BEGINNER" and "Beginner"
  // cannot appear as two separate filter entries, and so an unrecognised legacy value does
  // not create an empty option.
  const levels = useMemo(
    () => COURSE_LEVELS.filter((option) =>
      courses.some((course) => normalizeLevel(course.level) === option)
    ),
    [courses]
  )

  const filteredCourses = useMemo(() => courses.filter((course) => {
    const searchable = `${course.title} ${course.description || ''} ${course.category?.name || ''}`.toLowerCase()
    return searchable.includes(query.toLowerCase())
      && (level === 'ALL' || normalizeLevel(course.level) === level)
      && (category === 'ALL' || course.category?.name === category)
  }), [category, courses, level, query])

  return (
    <main className="portal-frame">
      <SiteNavbar />
      <div className="surface-cream px-6 py-14">
        <div className="mx-auto max-w-6xl">
          <section className="relative overflow-hidden rounded-[2rem] bg-[#0b1628] px-6 py-10 text-white md:px-10 md:py-14">
            <div className="absolute right-0 top-0 h-full w-1/2 bg-[radial-gradient(circle_at_70%_35%,rgba(57,96,171,0.7),transparent_55%)]" />
            <div className="relative max-w-2xl">
              <p className="text-sm font-bold uppercase tracking-[0.24em] text-[#f45135]">The CourseFlow catalog</p>
              <h1 className="mt-3 text-4xl font-bold tracking-tight md:text-6xl">Find your next useful skill.</h1>
              <p className="mt-4 max-w-xl text-base leading-7 text-slate-300">Browse practical courses built for steady progress, from first principles to advanced projects.</p>
            </div>
          </section>

          <section className="-mt-6 relative rounded-2xl border border-slate-200 bg-white p-4 shadow-xl shadow-slate-200/60 md:p-5">
            <div className="grid gap-3 md:grid-cols-[1fr_10rem_10rem_auto]">
              <label className="relative"><span className="sr-only">Search courses</span><Search size={18} aria-hidden="true" className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" /><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search courses, topics, or skills" className="search-field bg-slate-50 focus:bg-white" /></label>
              <select value={category} onChange={(event) => setCategory(event.target.value)} className="rounded-xl border border-slate-300 bg-slate-50 px-3 py-3 text-sm outline-none focus:border-teal-600"><option value="ALL">All categories</option>{categories.map((item) => <option key={item}>{item}</option>)}</select>
              <select value={level} onChange={(event) => setLevel(event.target.value)} className="rounded-xl border border-slate-300 bg-slate-50 px-3 py-3 text-sm outline-none focus:border-teal-600"><option value="ALL">All levels</option>{levels.map((item) => <option key={item}>{item}</option>)}</select>
              <button type="button" onClick={() => { setQuery(''); setCategory('ALL'); setLevel('ALL') }} className="rounded-xl border border-slate-300 px-4 py-3 text-sm font-semibold text-slate-700 hover:bg-slate-50">Clear</button>
            </div>
          </section>

          <div className="mb-6 mt-12 flex items-end justify-between gap-4"><div><p className="text-sm font-bold uppercase tracking-[0.2em] text-teal-700">Explore</p><h2 className="mt-2 text-3xl font-bold text-slate-950">All courses</h2></div><p className="text-sm text-slate-500">{filteredCourses.length} result{filteredCourses.length === 1 ? '' : 's'}</p></div>

          {loading && <LoadingState label="Finding courses..." />}
          {!loading && error && <ErrorState message={error} onRetry={fetchCourses} />}
          {!loading && !error && filteredCourses.length === 0 && <EmptyState title="No courses match those filters" message="Try a different search term or clear the filters to explore the full catalog." action={<button type="button" onClick={() => { setQuery(''); setCategory('ALL'); setLevel('ALL') }} className="rounded-full bg-slate-950 px-5 py-2.5 text-sm font-semibold text-white">Show all courses</button>} />}
          {!loading && !error && filteredCourses.length > 0 && <div className="grid gap-6 md:grid-cols-2 xl:grid-cols-3">{filteredCourses.map((course) => <CourseCard key={course.id} course={course} />)}</div>}
        </div>
      </div>
      <SiteFooter />
    </main>
  )
}
