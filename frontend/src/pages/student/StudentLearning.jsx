import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { DashboardLayout } from '../../layouts/DashboardLayout'
import api from '../../services/api'

const submissionStatusLabel = (submission) => {
  if (submission.status === 'GRADED') return 'Graded'
  if (submission.status === 'NOT_SUBMITTED') return 'Not submitted'
  if (submission.status === 'LATE') return 'Submitted late - awaiting grade'
  return 'Submitted - awaiting grade'
}

const methodLabel = (method) => method === 'FILE' ? 'File' : method === 'URL' ? 'URL' : 'Written text'

export default function StudentLearning() {
  const { courseId } = useParams()
  const [course, setCourse] = useState(null)
  const [modules, setModules] = useState([])
  const [lessons, setLessons] = useState({})
  const [materials, setMaterials] = useState({})
  const [assignments, setAssignments] = useState([])
  const [progress, setProgress] = useState(null)
  const [completedLessons, setCompletedLessons] = useState({})
  const [submissionInputs, setSubmissionInputs] = useState({})
  const [submissions, setSubmissions] = useState({})
  const [submissionHistory, setSubmissionHistory] = useState({})
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let active = true
    setLoading(true)
    setError('')

    const loadCourse = async () => {

      try {
        const [courseResponse, moduleResponse, assignmentResponse, progressResponse, submissionsResponse] = await Promise.all([
          api.get(`/api/courses/${courseId}`),
          api.get(`/api/courses/${courseId}/modules`),
          api.get(`/api/courses/${courseId}/assignments`),
          api.get(`/api/student/courses/${courseId}/progress`),
          api.get('/api/student/submissions'),
        ])

        if (!active) return

        setCourse(courseResponse.data?.data || null)
        setModules(moduleResponse.data?.data || [])
        setAssignments(assignmentResponse.data?.data || [])
        setProgress(progressResponse.data?.data || null)
        // Restore per-lesson completion so a reload does not reset "Mark complete".
        const progressData = progressResponse.data?.data
        setCompletedLessons(
          Object.fromEntries((progressData?.completedLessonIds || []).map((lessonId) => [lessonId, true])),
        )
        const history = {}
        ;(submissionsResponse.data?.data || []).forEach((submission) => {
          history[submission.assignmentId] = [...(history[submission.assignmentId] || []), submission]
        })
        setSubmissionHistory(history)
        setSubmissions(Object.fromEntries(Object.entries(history).map(([id, entries]) => [id, entries[0]])))
      } catch (requestError) {
        if (active) setError(requestError.response?.data?.message || 'Unable to load this course.')
      } finally {
        if (active) setLoading(false)
      }
    }

    loadCourse()

    return () => {
      active = false
    }
  }, [courseId])

  const loadLessons = async (moduleId) => {
    if (lessons[moduleId]) return

    try {
      const response = await api.get(`/api/modules/${moduleId}/lessons`)
      setLessons((current) => ({ ...current, [moduleId]: response.data?.data || [] }))
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'Unable to load lessons.')
    }
  }

  const loadMaterials = async (lessonId) => {
    if (materials[lessonId]) return

    try {
      const response = await api.get(`/api/lessons/${lessonId}/materials`)
      setMaterials((current) => ({ ...current, [lessonId]: response.data?.data || [] }))
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'Unable to load lesson materials.')
    }
  }

  const completeLesson = async (lessonId) => {
    try {
      await api.post(`/api/student/lessons/${lessonId}/complete`)
      setCompletedLessons((current) => ({ ...current, [lessonId]: true }))
      const response = await api.get(`/api/student/courses/${courseId}/progress`)
      setProgress(response.data?.data || null)
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'Unable to update lesson progress.')
    }
  }

  const submitAssignment = async (assignment) => {
    const input = submissionInputs[assignment.id] || { method: 'FILE', file: null, url: '', text: '' }
    if (input.method === 'FILE' && !input.file) {
      setError('Choose a file before submitting.')
      return
    }
    if (input.method === 'URL' && !input.url?.trim()) {
      setError('Enter a URL before submitting.')
      return
    }
    if (input.method === 'TEXT' && !input.text?.trim()) {
      setError('Enter a written response before submitting.')
      return
    }
    if (input.method === 'FILE' && input.file) {
      const maxSize = assignment.maxFileSize || 5 * 1024 * 1024
      const allowedTypes = (assignment.allowedFileTypes || []).map((type) => type.toLowerCase().replace('.', ''))
      const extension = input.file.name.includes('.') ? input.file.name.split('.').pop().toLowerCase() : ''
      if (input.file.size > maxSize) {
        setError(`The selected file must not exceed ${Math.round(maxSize / 1024 / 1024)} MB.`)
        return
      }
      if (allowedTypes.length && !allowedTypes.includes(extension)) {
        setError(`Allowed file types: ${allowedTypes.join(', ')}`)
        return
      }
    }

    try {
      const formData = new FormData()
      formData.append('submissionMethod', input.method)
      if (input.method === 'FILE') formData.append('file', input.file)
      if (input.method === 'URL') formData.append('url', input.url.trim())
      if (input.method === 'TEXT') formData.append('text', input.text.trim())
      const response = await api.post(`/api/student/assignments/${assignment.id}/submit`, formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
      const submission = response.data?.data
      setSubmissions((current) => ({ ...current, [assignment.id]: submission }))
      setSubmissionHistory((current) => ({ ...current, [assignment.id]: [submission, ...(current[assignment.id] || [])] }))
      setSubmissionInputs((current) => ({ ...current, [assignment.id]: { ...input, file: null, url: '', text: '' } }))
      setError('')
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'Unable to submit assignment.')
    }
  }

  if (loading) {
    return <DashboardLayout role="STUDENT" title="Loading course" subtitle="Preparing your lessons and assignments..." />
  }

  if (error && !course) {
    return <DashboardLayout role="STUDENT" title="Course unavailable"><p className="rounded-xl bg-red-50 p-4 text-red-700">{error}</p></DashboardLayout>
  }

  return (
    <DashboardLayout
      role="STUDENT"
      title={course?.title || 'Course workspace'}
      subtitle="Work through lessons, download materials, and submit assignments."
    >
      {error && <p className="mb-5 rounded-xl bg-red-50 p-4 text-sm text-red-700">{error}</p>}

      <div className="mb-8 rounded-2xl bg-slate-900 p-5 text-white">
        <div className="flex flex-wrap items-end justify-between gap-4">
          <div>
            <p className="text-sm text-teal-300">Course progress</p>
            <p className="mt-2 text-4xl font-bold">{progress?.percentage ?? 0}%</p>
          </div>
          <p className="text-sm text-slate-300">{progress?.completedLessons ?? 0} of {progress?.totalLessons ?? 0} lessons completed</p>
        </div>
        <div className="mt-4 h-2.5 rounded-full bg-slate-700">
          <div className="h-2.5 rounded-full bg-teal-400" style={{ width: `${progress?.percentage ?? 0}%` }} />
        </div>
      </div>

      <div className="space-y-4">
        {modules.map((module) => (
          <details key={module.id} className="rounded-2xl bg-slate-50 p-5 ring-1 ring-slate-200" onToggle={(event) => event.currentTarget.open && loadLessons(module.id)}>
            <summary className="cursor-pointer list-none font-bold text-slate-900">
              <span>{module.orderNumber}. {module.title}</span>
              <span className="ml-3 text-sm font-normal text-slate-500">{module.description}</span>
            </summary>
            <div className="mt-4 space-y-3">
              {(lessons[module.id] || []).map((lesson) => (
                <article key={lesson.id} className="rounded-xl bg-white p-4 ring-1 ring-slate-200">
                  <div className="flex flex-wrap items-start justify-between gap-3">
                    <div>
                      <h3 className="font-semibold text-slate-900">{lesson.orderNumber}. {lesson.title}</h3>
                      <p className="mt-1 text-sm text-slate-600">{lesson.description}</p>
                    </div>
                    <button
                      type="button"
                      onClick={() => completeLesson(lesson.id)}
                      disabled={completedLessons[lesson.id]}
                      className="rounded-full bg-teal-700 px-3 py-1.5 text-xs font-semibold text-white disabled:bg-emerald-600"
                    >
                      {completedLessons[lesson.id] ? 'Completed' : 'Mark complete'}
                    </button>
                  </div>
                  {lesson.content && <p className="mt-3 whitespace-pre-wrap text-sm leading-6 text-slate-700">{lesson.content}</p>}
                  {lesson.videoUrl && <a href={lesson.videoUrl} target="_blank" rel="noreferrer" className="mt-3 inline-block text-sm font-semibold text-teal-700 underline">Open lesson video</a>}
                  <button type="button" onClick={() => loadMaterials(lesson.id)} className="mt-3 block text-sm font-semibold text-slate-700 underline">
                    {materials[lesson.id] ? 'Materials' : 'Load materials'}
                  </button>
                  {materials[lesson.id] && (
                    <ul className="mt-2 space-y-2 text-sm text-slate-600">
                      {materials[lesson.id].map((material) => (
                        <li key={material.id}>
                          <a href={material.filePath} target="_blank" rel="noreferrer" className="text-teal-700 underline">{material.title || material.fileName}</a>
                        </li>
                      ))}
                    </ul>
                  )}
                </article>
              ))}
              {lessons[module.id] && lessons[module.id].length === 0 && <p className="text-sm text-slate-500">No lessons in this module yet.</p>}
            </div>
          </details>
        ))}
      </div>

      <section className="mt-8 rounded-2xl bg-slate-50 p-5 ring-1 ring-slate-200">
        <div className="flex items-center justify-between gap-3">
          <h2 className="text-xl font-bold text-slate-900">Assignments</h2>
          <Link to="/student/courses" className="text-sm font-semibold text-teal-700 underline">Back to courses</Link>
        </div>
        <div className="mt-4 space-y-4">
          {assignments.map((assignment) => (
            <article key={assignment.id} className="rounded-xl bg-white p-4 ring-1 ring-slate-200">
              <h3 className="font-semibold text-slate-900">{assignment.title}</h3>
              <p className="mt-1 text-sm text-slate-600">{assignment.description}</p>
              <p className="mt-2 text-xs text-slate-500">Due: {assignment.dueDate ? new Date(assignment.dueDate).toLocaleString() : 'No due date'}</p>
              <p className="mt-2 text-xs text-slate-500">
                Maximum marks: {assignment.maxMarks ?? 100} · Allowed: {(assignment.allowedSubmissionMethods || ['FILE', 'URL', 'TEXT']).map(methodLabel).join(', ')}
              </p>
              {(() => {
                const allowedMethods = assignment.allowedSubmissionMethods || ['FILE', 'URL', 'TEXT']
                const input = submissionInputs[assignment.id] || { method: allowedMethods[0], file: null, url: '', text: '' }
                return (
                  <div className="mt-4 space-y-2">
                    <select
                      value={input.method}
                      onChange={(event) => setSubmissionInputs((current) => ({ ...current, [assignment.id]: { ...input, method: event.target.value, file: null } }))}
                      className="rounded-xl border border-slate-300 px-3 py-2 text-sm"
                    >
                      {allowedMethods.map((method) => <option key={method} value={method}>{methodLabel(method)}</option>)}
                    </select>
                    {input.method === 'FILE' && (
                      <div className="flex flex-col gap-2 sm:flex-row sm:items-center">
                        <input
                          type="file"
                          accept={(assignment.allowedFileTypes || []).map((type) => `.${type.replace('.', '')}`).join(',')}
                          onChange={(event) => setSubmissionInputs((current) => ({ ...current, [assignment.id]: { ...input, file: event.target.files?.[0] || null } }))}
                          className="flex-1 rounded-xl border border-slate-300 px-3 py-2 text-sm"
                        />
                        {input.file && <span className="text-xs text-slate-600">{input.file.name}</span>}
                      </div>
                    )}
                    {input.method === 'URL' && (
                      <input value={input.url} onChange={(event) => setSubmissionInputs((current) => ({ ...current, [assignment.id]: { ...input, url: event.target.value } }))} placeholder="https://..." className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm" />
                    )}
                    {input.method === 'TEXT' && (
                      <textarea value={input.text} onChange={(event) => setSubmissionInputs((current) => ({ ...current, [assignment.id]: { ...input, text: event.target.value } }))} placeholder="Write your response" rows={4} className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm" />
                    )}
                    <button type="button" onClick={() => submitAssignment(assignment)} className="rounded-xl bg-teal-700 px-4 py-2 text-sm font-semibold text-white">Submit attempt</button>
                  </div>
                )
              })()}
              {submissions[assignment.id] && (
                <p className={`mt-3 text-sm ${submissions[assignment.id].status === 'GRADED' ? 'text-emerald-700' : 'text-amber-700'}`}>
                  Status: {submissionStatusLabel(submissions[assignment.id])}.
                  {submissions[assignment.id].grade != null ? ` Grade: ${submissions[assignment.id].grade}/${assignment.maxMarks ?? 100}.` : ''}
                  {submissions[assignment.id].feedback && ` Feedback: ${submissions[assignment.id].feedback}`}
                </p>
              )}
              {(submissionHistory[assignment.id] || []).length > 1 && (
                <details className="mt-3 text-sm">
                  <summary className="cursor-pointer font-semibold text-slate-700">Previous attempts ({submissionHistory[assignment.id].length})</summary>
                  <ul className="mt-2 space-y-1 text-slate-600">
                    {submissionHistory[assignment.id].map((attempt) => <li key={attempt.id}>Attempt {attempt.attemptNumber}: {submissionStatusLabel(attempt)}{attempt.grade != null ? ` · ${attempt.grade}/${assignment.maxMarks ?? 100}` : ''}</li>)}
                  </ul>
                </details>
              )}
            </article>
          ))}
          {assignments.length === 0 && <p className="text-sm text-slate-500">No assignments for this course.</p>}
        </div>
      </section>
    </DashboardLayout>
  )
}
