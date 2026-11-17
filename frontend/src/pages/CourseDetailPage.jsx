
import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import api from '../services/api'
import { formatNpr, starRating } from '../utils/format'
import SiteNavbar from '../components/SiteNavbar'
import SiteFooter from '../components/SiteFooter'

export default function CourseDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const { isAuthenticated } = useAuth()

  const [course, setCourse] = useState(null)
  const [loading, setLoading] = useState(true)
  const [isEnrolling, setIsEnrolling] = useState(false)
  const [enrollmentMessage, setEnrollmentMessage] = useState('')
  const [reviews, setReviews] = useState([])
  const [reviewForm, setReviewForm] = useState({
    rating: 5,
    comment: '',
  })
  const [reviewMessage, setReviewMessage] = useState('')
  const [enrollment, setEnrollment] = useState(null)
  const [error, setError] = useState(null)

  useEffect(() => {
    let active = true

    setLoading(true)
    setError(null)

    const fetchCourse = async () => {
      try {
        const response = await api.get(`/api/courses/${id}`)

        if (!response.data?.data) {
          throw new Error('Course not found')
        }

        if (active) setCourse(response.data.data)
      } catch (requestError) {
        if (!active) return
        setError(requestError.response?.data?.message || requestError.message)
        setCourse(null)
      } finally {
        if (active) setLoading(false)
      }
    }

    fetchCourse()

    return () => {
      active = false
    }
  }, [id])

  useEffect(() => {
    let active = true

    if (!isAuthenticated) {
      setEnrollment(null)
      return undefined
    }

    api
      .get('/api/student/enrollments')
      .then((response) => {
        if (!active) return

        const match = (response.data?.data || []).find(
          (item) => String(item.courseId) === String(id)
        )

        setEnrollment(match || null)
      })
      .catch(() => {
        if (active) setEnrollment(null)
      })

    return () => {
      active = false
    }
  }, [id, isAuthenticated])

  useEffect(() => {
    let active = true

    api
      .get(`/api/courses/${id}/reviews`)
      .then((response) => {
        if (active) setReviews(response.data?.data || [])
      })
      .catch(() => {
        if (active) setReviews([])
      })

    return () => {
      active = false
    }
  }, [id])

  const handleEnroll = async () => {
    if (!isAuthenticated) {
      navigate('/login')
      return
    }

    setEnrollmentMessage('')
    setIsEnrolling(true)

    try {
      if (Number(course?.price || 0) > 0) {
        const response = await api.post('/api/payments/create', {
          courseId: Number(id),
          paymentMethod: 'DEMO',
        })

        const paymentId = response.data?.data?.id

        if (!paymentId) {
          setEnrollmentMessage('Unable to start checkout for this course.')
          return
        }

        navigate(`/payment/${paymentId}`)
      } else {
        await api.post(`/api/courses/${id}/enroll`)

        setEnrollment({
          courseId: Number(id),
          status: 'ACTIVE',
        })

        setEnrollmentMessage('You are enrolled in this course.')
      }
    } catch (error) {
      setEnrollmentMessage(
        error.response?.data?.message ||
        'Unable to enroll in this course.'
      )
    } finally {
      setIsEnrolling(false)
    }
  }

  const submitReview = async (event) => {
    event.preventDefault()

    if (!isAuthenticated) {
      navigate('/login')
      return
    }

    try {
      const response = await api.post(`/api/courses/${id}/reviews`, {
        rating: Number(reviewForm.rating),
        comment: reviewForm.comment,
      })

      const created = response.data?.data

      if (created) {
        setReviews((current) => [
          created,
          ...current,
        ])
      }

      setReviewForm({
        rating: 5,
        comment: '',
      })

      setReviewMessage('Review submitted.')
    } catch (error) {
      setReviewMessage(
        error.response?.data?.message ||
        'Unable to submit review.'
      )
    }
  }

  if (loading) {
    return (
      <main className="min-h-screen bg-[#0B132B] text-white">
        <SiteNavbar />

        <div className="flex min-h-[70vh] items-center justify-center">
          <p className="text-sm text-[#C5D1E0]">
            Loading course details...
          </p>
        </div>

        <SiteFooter />
      </main>
    )
  }

  if (!course) {
    return (
      <main className="min-h-screen bg-[#0B132B] text-white">
        <SiteNavbar />

        <div className="mx-auto flex min-h-[70vh] max-w-6xl flex-col items-center justify-center px-6 text-center">
          <h1 className="text-3xl font-bold text-white">
            Course not found
          </h1>

          {error && (
            <p className="mt-3 text-sm text-[#C5D1E0]">
              {error}
            </p>
          )}

          <Link
            to="/courses"
            className="mt-5 font-semibold text-[#FF573B] hover:text-[#FF6B52]"
          >
            ← Back to courses
          </Link>
        </div>

        <SiteFooter />
      </main>
    )
  }

  const instructors = course.instructors || []

  return (
    <main className="min-h-screen bg-[#0B132B] text-white">

      <SiteNavbar />

      {/* HERO */}
      <section className="bg-[#0B132B] px-6 py-12 md:py-16">
        <div className="mx-auto max-w-6xl">

          {/* Back Link */}
          <Link
            to="/courses"
            className="text-sm font-semibold text-[#C5D1E0] transition hover:text-white"
          >
            ← Back to catalog
          </Link>

          <div className="mt-8 grid gap-10 lg:grid-cols-[1.5fr_0.75fr]">

            {/* LEFT SIDE */}
            <div>

              {/* Course Image */}
              <div className="relative h-64 overflow-hidden rounded-3xl bg-gradient-to-br from-[#213A70] via-[#1B2A52] to-[#0A1128] shadow-2xl md:h-80">

                {course.thumbnail && (
                  <img
                    src={course.thumbnail}
                    alt={course.title}
                    className="h-full w-full object-cover"
                  />
                )}

                {!course.thumbnail && (
                  <div className="flex h-full items-center justify-center">
                    <span className="text-5xl font-bold text-white/20">
                      Learn
                    </span>
                  </div>
                )}

              </div>

              {/* Course Info */}
              <div className="mt-8">

                <div className="flex flex-wrap items-center gap-3">

                  <span className="rounded-full bg-[#FFF8DC] px-3 py-1 text-xs font-bold text-[#111827]">
                    {course.category?.name || 'General'}
                  </span>

                  <span className="rounded-full border border-[#213A70] bg-[#1B2A52] px-3 py-1 text-xs font-semibold text-[#C5D1E0]">
                    {course.level}
                  </span>

                </div>

                <h1 className="mt-5 text-4xl font-bold tracking-tight text-white md:text-5xl">
                  {course.title}
                </h1>

                <p className="mt-5 max-w-3xl text-lg leading-8 text-[#C5D1E0]">
                  {course.description}
                </p>

              </div>

              {/* Course Stats */}
              <div className="mt-8 grid gap-4 sm:grid-cols-3">

                <div className="rounded-2xl border border-[#213A70] bg-[#1B2A52] p-5">
                  <p className="text-sm text-[#C5D1E0]">
                    Duration
                  </p>

                  <p className="mt-2 font-bold text-white">
                    {course.duration}
                  </p>
                </div>

                <div className="rounded-2xl border border-[#213A70] bg-[#1B2A52] p-5">
                  <p className="text-sm text-[#C5D1E0]">
                    Level
                  </p>

                  <p className="mt-2 font-bold text-white">
                    {course.level}
                  </p>
                </div>

                <div className="rounded-2xl border border-[#213A70] bg-[#1B2A52] p-5">
                  <p className="text-sm text-[#C5D1E0]">
                    Status
                  </p>

                  <p className="mt-2 font-bold text-white">
                    {course.status}
                  </p>
                </div>

              </div>

            </div>

            {/* RIGHT SIDE — ENROLLMENT CARD */}
            <aside className="h-fit rounded-3xl border border-[#213A70] bg-[#213A70] p-6 shadow-2xl lg:sticky lg:top-24">

              <p className="text-xs font-bold uppercase tracking-[0.2em] text-[#FFF8DC]">
                Instructor
              </p>

              <div className="mt-3 space-y-3">
                {instructors.length ? instructors.map((instructor) => (
                  <Link key={instructor.id} to={`/instructors/${instructor.id}`} className="flex items-center gap-3 rounded-xl p-2 hover:bg-white/10">
                    {instructor.profileImage ? <img src={instructor.profileImage} alt="" className="h-12 w-12 rounded-full object-cover" /> : <div className="flex h-12 w-12 items-center justify-center rounded-full bg-[#FF573B] font-bold">{instructor.firstName?.[0]}{instructor.lastName?.[0]}</div>}
                    <span><span className="block font-bold text-white">{instructor.firstName} {instructor.lastName}</span><span className="block text-xs text-[#C5D1E0]">{instructor.professionalTitle || 'Course instructor'}</span><span className="text-xs text-[#FFF8DC]">View profile</span></span>
                  </Link>
                )) : <p className="text-[#C5D1E0]">Course instructor</p>}
              </div>

              <div className="my-6 h-px bg-[#FFFFFF]/10" />

              <p className="text-sm text-[#C5D1E0]">
                Course price
              </p>

              <p className="mt-1 text-3xl font-bold text-white">
                {formatNpr(course.price)}
              </p>

              <p className="mt-3 text-sm leading-6 text-[#C5D1E0]">
                One-time purchase • Includes certificates and projects
              </p>

              {/* Primary CTA */}
              <button
                type="button"
                onClick={() =>
                  enrollment
                    ? navigate(`/student/courses/${id}/learn`)
                    : handleEnroll()
                }
                disabled={isEnrolling}
                className="mt-6 w-full rounded-xl bg-[#FF573B] px-4 py-3.5 font-bold text-white shadow-lg shadow-[#FF573B]/20 transition hover:bg-[#FF6B52] disabled:cursor-not-allowed disabled:opacity-60"
              >
                {isEnrolling
                  ? 'Preparing...'
                  : enrollment
                    ? 'Continue learning'
                    : Number(course.price || 0) > 0
                      ? 'Enroll and pay'
                      : 'Enroll now'}
              </button>

              {enrollment && !enrollmentMessage && (
                <p className="mt-3 text-center text-sm font-medium text-[#FFF8DC]">
                  You are enrolled in this course.
                </p>
              )}

              {enrollmentMessage && (
                <p className="mt-3 text-center text-sm font-medium text-[#FFF8DC]">
                  {enrollmentMessage}
                </p>
              )}

              </aside>

          </div>
        </div>
      </section>

      {/* CREAM INFORMATION SECTION */}
      <section className="bg-[#FFF8DC] px-6 py-12">

        <div className="mx-auto max-w-6xl">

          <div className="rounded-3xl bg-white p-6 shadow-xl md:p-8">

            <h2 className="text-2xl font-bold text-[#111827]">
              Learner reviews
            </h2>

            {/* Reviews */}
            <div className="mt-6 space-y-3">

              {reviews.map((review) => (
                <article
                  key={review.id}
                  className="rounded-2xl border border-slate-200 bg-slate-50 p-5"
                >

                  <div className="flex items-center justify-between gap-3">

                    <p className="font-semibold text-[#111827]">
                      {review.studentUsername}
                    </p>

                    <p className="text-sm font-semibold text-[#FF573B]">
                      {starRating(review.rating)}
                    </p>

                  </div>

                  <p className="mt-2 text-sm leading-6 text-slate-600">
                    {review.comment || 'No comment provided.'}
                  </p>

                </article>
              ))}

              {reviews.length === 0 && (
                <p className="text-sm text-slate-500">
                  No reviews yet.
                </p>
              )}

            </div>
            
            {/* Review Form */}
            <form
              onSubmit={submitReview}
              className="mt-8 rounded-2xl border border-slate-200 bg-slate-50 p-5"
            >

              <h3 className="font-semibold text-[#111827]">
                Share your experience
              </h3>

              <div className="mt-4 flex flex-col gap-3 sm:flex-row">

                <select
                  value={reviewForm.rating}
                  onChange={(event) =>
                    setReviewForm((current) => ({
                      ...current,
                      rating: event.target.value,
                    }))
                  }
                  className="rounded-xl border border-slate-300 bg-white px-3 py-2 text-sm text-[#111827] focus:border-[#FF573B] focus:outline-none focus:ring-4 focus:ring-orange-100"
                >
                  <option value="5">5 stars</option>
                  <option value="4">4 stars</option>
                  <option value="3">3 stars</option>
                  <option value="2">2 stars</option>
                  <option value="1">1 star</option>
                </select>

                <input
                  value={reviewForm.comment}
                  onChange={(event) =>
                    setReviewForm((current) => ({
                      ...current,
                      comment: event.target.value,
                    }))
                  }
                  placeholder="Write a review"
                  className="flex-1 rounded-xl border border-slate-300 bg-white px-3 py-2 text-sm text-[#111827] outline-none transition focus:border-[#FF573B] focus:ring-4 focus:ring-orange-100"
                />

                <button
                  type="submit"
                  className="rounded-xl bg-[#FF573B] px-5 py-2 text-sm font-bold text-white transition hover:bg-[#FF6B52]"
                >
                  Submit review
                </button>
              </div>

              {reviewMessage && (
                <p className="mt-3 text-sm font-medium text-[#FF573B]">
                  {reviewMessage}
                </p>
              )}
            </form>
          </div>
        </div>
      </section>
      <SiteFooter />
    </main>
  )
}