import { useEffect, useState } from 'react'
import { DashboardLayout } from '../../layouts/DashboardLayout'
import api from '../../services/api'

export default function AdminCategories() {
  const [categories, setCategories] = useState([])
  const [form, setForm] = useState({ name: '', description: '' })
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [openCategories, setOpenCategories] = useState({})

  const loadCategories = () => api.get('/api/admin/categories').then((response) => setCategories(response.data?.data || [])).catch((requestError) => setError(requestError.response?.data?.message || 'Unable to load categories.')).finally(() => setLoading(false))
  useEffect(() => { loadCategories() }, [])

  const createCategory = async (event) => {
    event.preventDefault()
    try {
      await api.post('/api/admin/categories', form)
      setForm({ name: '', description: '' })
      await loadCategories()
    } catch (requestError) { setError(requestError.response?.data?.message || 'Unable to create category.') }
  }

  return (
    <DashboardLayout role="ADMIN" title="Category management" subtitle="Organize the course catalog for instructors and learners.">
      {error && <p className="mb-4 rounded-xl bg-red-50 p-4 text-red-700">{error}</p>}
      {loading && <p className="mb-4 text-slate-600">Loading categories...</p>}
      <form onSubmit={createCategory} className="rounded-2xl bg-slate-50 p-5 ring-1 ring-slate-200">
        <h2 className="text-xl font-bold text-slate-900">Create category</h2>
        <div className="mt-4 flex flex-col gap-3 sm:flex-row"><input required placeholder="Category name" value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} className="flex-1 rounded-xl border border-slate-300 px-3 py-2" /><input placeholder="Description" value={form.description} onChange={(event) => setForm({ ...form, description: event.target.value })} className="flex-1 rounded-xl border border-slate-300 px-3 py-2" /><button className="rounded-xl bg-teal-700 px-4 py-2 font-semibold text-white">Add category</button></div>
      </form>
      <div className="mt-5 grid gap-3">{categories.map((category) => {
        const isOpen = openCategories[category.id] !== false
        return (
          <section key={category.id} className="rounded-2xl bg-white p-5 ring-1 ring-slate-200">
            <button type="button" onClick={() => setOpenCategories((current) => ({ ...current, [category.id]: !isOpen }))} className="flex w-full flex-wrap items-start justify-between gap-3 text-left">
              <span><span className="font-bold text-slate-900">{category.name}</span><span className="mt-1 block text-sm text-slate-600">{category.description || 'No description'}</span></span>
              <span className="flex items-center gap-2"><span className="rounded-full bg-slate-100 px-3 py-1 text-xs font-semibold text-slate-700">{category.courses?.length || 0} course{category.courses?.length === 1 ? '' : 's'}</span><span className="text-xs font-semibold text-teal-700">{isOpen ? 'Show less' : 'Show courses'}</span></span>
            </button>
            {isOpen && (category.courses?.length ? (
              <div className="mt-4 space-y-2">
                {category.courses.map((course) => (
                  <details key={course.id} className="rounded-xl border border-slate-200 p-3">
                    <summary className="cursor-pointer list-none">
                      <div className="flex flex-wrap items-center justify-between gap-2">
                        <h3 className="font-semibold text-teal-700 underline">{course.title}</h3>
                        <span className="rounded-full bg-slate-100 px-2 py-1 text-xs font-semibold text-slate-600">{course.status}</span>
                      </div>
                    </summary>
                    <div className="mt-3 border-t border-slate-100 pt-3 text-sm text-slate-700">
                      <p className="whitespace-pre-wrap">{course.description || 'No description provided.'}</p>
                      <div className="mt-2 grid gap-2 text-xs text-slate-500 sm:grid-cols-3">
                        <span>Level: {course.level || 'Not specified'}</span>
                        <span>Duration: {course.duration || 'Not specified'}</span>
                        <span>Price: {course.price ?? 0}</span>
                      </div>
                      <p className="mt-2 text-xs text-slate-500">Instructor{course.instructors?.length === 1 ? '' : 's'}: {course.instructors?.length ? course.instructors.map((instructor) => `${instructor.firstName || ''} ${instructor.lastName || ''}`.trim() || instructor.username).join(', ') : 'Not assigned'}</p>
                    </div>
                  </details>
                ))}
              </div>
            ) : <p className="mt-4 text-sm text-slate-500">No courses assigned to this category.</p>)}
          </section>
        )
      })}{!loading && categories.length === 0 && <p className="text-sm text-slate-500">No categories yet.</p>}</div>
    </DashboardLayout>
  )
}
