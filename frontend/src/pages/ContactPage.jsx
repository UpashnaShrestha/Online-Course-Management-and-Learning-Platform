import { useState } from 'react'
import SiteFooter from '../components/SiteFooter'
import SiteNavbar from '../components/SiteNavbar'

export default function ContactPage() {
  const [form, setForm] = useState({ name: '', email: '', subject: '', message: '' })

  const handleSubmit = (event) => {
    event.preventDefault()

    // There is no contact-messages endpoint in the API yet, so compose the message in the
    // user's mail client rather than pretending the message was stored.
    const subject = encodeURIComponent(`CourseFlow: ${form.subject || 'Contact'}`)
    const body = encodeURIComponent(
      `${form.message}\n\n---\nFrom: ${form.name} (${form.email})`,
    )

    window.location.href = `mailto:upashnaLearnTide@gmail.com?subject=${subject}&body=${body}`
  }

  return (
    <main className="min-h-screen bg-surface">
      <SiteNavbar />
      <section className="mx-auto grid max-w-6xl gap-10 px-6 py-16 lg:grid-cols-[0.8fr_1.2fr]">
        <div>
          <p className="text-sm font-bold uppercase tracking-[0.24em] text-brand">Contact us</p>
          <h1 className="mt-4 text-5xl font-bold tracking-tight text-brand-navy">Let’s make learning work better.</h1>
          <p className="mt-6 text-lg leading-8 text-slate-600">Have a question about courses, accounts, or the platform? Send a message and our team will get back to you.</p>
          <div className="mt-8 space-y-3 text-sm text-slate-600"><p><strong className="text-slate-900">Email:</strong> upashna_learnTide@gmail.com</p><p><strong className="text-slate-900">Response time:</strong> Within one business day</p></div>
        </div>
        <form onSubmit={handleSubmit} className="card p-6 md:p-8">
          <div className="grid gap-4 md:grid-cols-2">
            <label className="text-sm font-semibold text-slate-700" htmlFor="contact-name">Name<input id="contact-name" required value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} className="field mt-2" /></label>
            <label className="text-sm font-semibold text-slate-700" htmlFor="contact-email">Email<input id="contact-email" required type="email" value={form.email} onChange={(event) => setForm({ ...form, email: event.target.value })} className="field mt-2" /></label>
          </div>
          <label className="mt-4 block text-sm font-semibold text-slate-700" htmlFor="contact-subject">Subject<input id="contact-subject" required value={form.subject} onChange={(event) => setForm({ ...form, subject: event.target.value })} className="field mt-2" /></label>
          <label className="mt-4 block text-sm font-semibold text-slate-700" htmlFor="contact-message">Message<textarea id="contact-message" required rows="5" value={form.message} onChange={(event) => setForm({ ...form, message: event.target.value })} className="field mt-2 resize-y" /></label>
          <button type="submit" className="btn-primary mt-5 w-full">Open in your email app</button>
          <p className="mt-3 text-xs text-slate-500">Submitting opens your default mail client with the message pre-filled.</p>
        </form>
      </section>
      <SiteFooter />
    </main>
  )
}
