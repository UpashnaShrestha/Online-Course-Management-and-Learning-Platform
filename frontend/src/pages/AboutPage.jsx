import SiteFooter from '../components/SiteFooter'
import SiteNavbar from '../components/SiteNavbar'

export default function AboutPage() {
  return (
    <main className="min-h-screen bg-surface">
      <SiteNavbar />
      <section className="mx-auto max-w-6xl px-6 py-16">
        <div className="max-w-3xl">
          <p className="text-sm font-bold uppercase tracking-[0.24em] text-brand">About CourseFlow</p>
          <h1 className="mt-4 text-5xl font-bold tracking-tight text-brand-navy md:text-6xl">Learning that keeps moving.</h1>
          <p className="mt-6 text-lg leading-8 text-slate-600">CourseFlow brings courses, lessons, assignments, feedback, and progress into one focused learning platform for students, instructors, and academic teams.</p>
        </div>
        <div className="mt-12 grid gap-5 md:grid-cols-3">
          <article className="card p-6"><h2 className="text-xl font-bold text-brand-navy">For students</h2><p className="mt-3 text-sm leading-6 text-slate-600">Follow clear learning paths, complete lessons, submit work, and see your progress grow.</p></article>
          <article className="card p-6"><h2 className="text-xl font-bold text-brand-navy">For instructors</h2><p className="mt-3 text-sm leading-6 text-slate-600">Create rich courses, organize content, manage assignments, and give meaningful feedback.</p></article>
          <article className="card p-6"><h2 className="text-xl font-bold text-brand-navy">For administrators</h2><p className="mt-3 text-sm leading-6 text-slate-600">Keep the catalog, users, approvals, categories, and platform activity organized.</p></article>
        </div>
      </section>
      <SiteFooter />
    </main>
  )
}
