import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import SiteNavbar from '../components/SiteNavbar'
import SiteFooter from '../components/SiteFooter'
import api from '../services/api'
import { resolveImageUrl } from '../utils/format'

export default function InstructorPublicProfile() {
  const { id } = useParams()
  const [profile, setProfile] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    api.get(`/api/instructors/${id}/public-profile`)
      .then((response) => setProfile(response.data?.data || null))
      .catch((requestError) => setError(requestError.response?.data?.message || 'Instructor profile unavailable.'))
  }, [id])

  return (
    <main className="min-h-screen bg-[#0B132B] text-white">
      <SiteNavbar />
      <div className="mx-auto max-w-5xl px-6 py-12">
        {error && <p className="rounded-xl bg-red-50 p-4 text-red-700">{error}</p>}
        {!profile && !error && <p className="text-slate-300">Loading instructor profile...</p>}
        {profile && (
          <>
            <section className="rounded-3xl bg-[#1B2A52] p-6 md:p-8">
              <div className="flex flex-col gap-5 sm:flex-row sm:items-center">
                {profile.profileImage ? <img src={resolveImageUrl(profile.profileImage)} alt="" className="h-24 w-24 rounded-full object-cover" /> : <div className="flex h-24 w-24 items-center justify-center rounded-full bg-[#FF573B] text-3xl font-bold">{profile.firstName?.[0]}{profile.lastName?.[0]}</div>}
                <div><p className="text-sm font-bold uppercase tracking-widest text-[#FFF8DC]">Instructor</p><h1 className="mt-2 text-3xl font-bold">{profile.firstName} {profile.lastName}</h1><p className="mt-1 text-[#C5D1E0]">{profile.professionalTitle || 'Course instructor'}</p>{profile.email && <a href={`mailto:${profile.email}`} className="mt-2 inline-block text-sm text-[#FFF8DC] underline">{profile.email}</a>}</div>
              </div>
              {profile.bio && <p className="mt-6 leading-7 text-[#C5D1E0]">{profile.bio}</p>}
              <div className="mt-6 grid gap-4 sm:grid-cols-3">
                {profile.expertise && <div><h2 className="font-semibold">Expertise</h2><p className="mt-1 text-sm text-[#C5D1E0]">{profile.expertise}</p></div>}
                {profile.qualifications && <div><h2 className="font-semibold">Qualifications</h2><p className="mt-1 text-sm text-[#C5D1E0]">{profile.qualifications}</p></div>}
                {profile.teachingExperience && <div><h2 className="font-semibold">Teaching experience</h2><p className="mt-1 text-sm text-[#C5D1E0]">{profile.teachingExperience}</p></div>}
              </div>
            </section>
            <section className="mt-8 rounded-3xl bg-white p-6 text-slate-900 md:p-8"><h2 className="text-2xl font-bold">Courses taught</h2><div className="mt-4 grid gap-3 sm:grid-cols-2">{profile.courses?.map((course) => <Link key={course.id} to={`/courses/${course.id}`} className="rounded-xl border border-slate-200 p-4 hover:border-teal-500"><p className="font-semibold text-teal-700">{course.title}</p></Link>)}</div></section>
          </>
        )}
      </div>
      <SiteFooter />
    </main>
  )
}
