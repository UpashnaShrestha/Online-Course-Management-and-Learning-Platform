import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import api from '../services/api'
import SiteNavbar from '../components/SiteNavbar'
import SiteFooter from '../components/SiteFooter'
import CourseCard from '../components/CourseCard'
import { EmptyState, ErrorState, LoadingState } from '../components/PageState'

export default function Home() {
  const [courses, setCourses] = useState([])
  const [categories, setCategories] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    const fetchCourses = async () => {
      try {
        const [courseResponse, categoryResponse] = await Promise.all([
          api.get('/api/courses'),
          api.get('/api/categories'),
        ])
        setCourses(courseResponse.data?.data ?? [])
        setCategories(categoryResponse.data?.data ?? [])
      } catch (fetchError) {
        setError(fetchError.response?.data?.message || 'Featured courses are temporarily unavailable.')
        setCourses([])
        setCategories([])
      } finally {
        setLoading(false)
      }
    }

    fetchCourses()
  }, [])

  return (
    <main className="portal-frame">
      <SiteNavbar />
      <div className="px-6 py-10 md:py-16">
        <section className="relative mx-auto max-w-7xl overflow-hidden rounded-[2rem] border border-white/10 bg-[#0b1628] px-8 py-14 text-white shadow-2xl shadow-black/30 md:px-16 md:py-20">
          <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_60%_0%,rgba(55,101,180,0.7),transparent_42%),linear-gradient(115deg,transparent_40%,rgba(244,81,53,0.08)_40%,transparent_62%)]" />
          <div className="relative grid gap-12 lg:grid-cols-[1.2fr_0.8fr] lg:items-end">
            <div>
              <p className="text-sm font-bold uppercase tracking-[0.28em] text-[#f45135]">A sharper way to learn online</p>
              <h1 className="mt-5 max-w-3xl text-5xl font-extrabold leading-[0.98] tracking-tight md:text-7xl">From curiosity<br />to capability.</h1>
              <p className="mt-6 max-w-xl text-lg leading-8 text-slate-300">CourseFlow turns structured courses into visible momentum, with every lesson, assignment, and milestone in one place.</p>
              <div className="mt-8 flex flex-wrap gap-3"><Link to="/courses" className="coral-action">Explore courses</Link><Link to="/register" className="rounded-lg border border-white/25 px-6 py-3 font-bold text-white transition hover:border-white hover:bg-white/10">Start learning</Link></div>
            </div>
            <div className="grid grid-cols-3 gap-3">
              <div className="rounded-2xl border border-white/10 bg-[#182b4d] p-4"><p className="text-xs text-slate-300">Courses</p><p className="mt-3 text-3xl font-bold text-[#fff5d6]">{courses.length}</p></div>
              <div className="rounded-2xl border border-white/10 bg-[#263f7a] p-4"><p className="text-xs text-slate-200">Instructors</p><p className="mt-3 text-3xl font-bold text-white">{new Set(courses.flatMap((course) => course.instructors || []).map((instructor) => instructor.id)).size}</p></div>
              <div className="rounded-2xl border border-white/10 bg-[#fff5d6] p-4 text-[#0f172a]"><p className="text-xs text-slate-600">Categories</p><p className="mt-3 text-3xl font-bold">{categories.length}</p></div>
            </div>
          </div>
        </section>

      <section className="surface-cream mx-auto mt-0 max-w-none px-6 py-14 md:px-[max(1.5rem,calc((100vw-80rem)/2))] md:py-20">
        <div className="mx-auto max-w-7xl">
        <div className="mb-6 flex items-center justify-between gap-4">
          <h2 className="text-3xl font-extrabold text-brand-navy">Featured courses</h2>
          <Link to="/courses" className="font-bold text-[#f45135] underline underline-offset-4">
            View all
          </Link>
        </div>

        {loading && <LoadingState label="Loading featured courses..." />}
        {!loading && error && <ErrorState message={error} />}
        {!loading && !error && courses.length === 0 && (
            <EmptyState
              title="No courses yet"
              message="New courses are being prepared. Check back soon."
              action={(
                <Link to="/courses" className="font-semibold text-teal-700 underline">
                  Browse the catalog
                </Link>
              )}
            />
          )}
        {!loading && !error && courses.length > 0 && <div className="grid gap-6 md:grid-cols-3">{courses.slice(0, 3).map((course) => <CourseCard key={course.id} course={course} compact />)}</div>}
        </div>
      </section>
      </div>
      <SiteFooter />
    </main>
  )
}
