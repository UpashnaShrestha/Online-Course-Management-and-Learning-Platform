import { useEffect, useState } from 'react'
import { DashboardLayout } from '../../layouts/DashboardLayout'
import api from '../../services/api'

export default function InstructorAssignments() {
  const [courses, setCourses] = useState([])
  const [assignments, setAssignments] = useState([])
  const [submissions, setSubmissions] = useState([])
  const [selectedAssignment, setSelectedAssignment] = useState(null)
  const [gradingSubmissionId, setGradingSubmissionId] = useState(null)
  const [grades, setGrades] = useState({})
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true

    const loadAssignments = async () => {
      try {
        const courseResponse = await api.get('/api/instructor/courses')
        const currentCourses = courseResponse.data?.data || []
        const responses = await Promise.all(currentCourses.map((course) => api.get(`/api/instructor/courses/${course.id}/assignments`)))

        if (!active) return

        setCourses(currentCourses)
        setAssignments(responses.flatMap((response) => response.data?.data || []))
      } catch (requestError) {
        if (!active) return
        setError(requestError.response?.data?.message || 'Unable to load assignments.')
      }
      finally {
        if (active) setLoading(false)
      }
    }

    loadAssignments()

    return () => {
      active = false
    }
  }, [])

  const loadSubmissions = async (assignmentId) => {
    setSelectedAssignment(assignmentId)
    setGradingSubmissionId(null)
    setSubmissions([])
    try {
      const response = await api.get(`/api/instructor/assignments/${assignmentId}/submissions`)
      setSubmissions(response.data?.data || [])
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'Unable to load submissions.')
    }
  }

  const openGrading = (submission) => {
    setGrades((current) => ({
      ...current,
      [submission.id]: {
        grade: submission.grade ?? '',
        feedback: submission.feedback ?? '',
      },
    }))
    setGradingSubmissionId(submission.id)
  }

  const gradeSubmission = async (submissionId) => {
    const form = grades[submissionId] || {}
    const grade = Number(form.grade)

    if (form.grade === undefined || form.grade === '' || Number.isNaN(grade)) {
      setError('Enter a numeric grade before saving.')
      return
    }

    const openFile = async (submission, download = false) => {
      try {
        const response = await api.get(`/api/submissions/${submission.id}/file?download=${download}`, { responseType: 'blob' })
        const url = URL.createObjectURL(response.data)
        if (download) {
          const link = document.createElement('a')
          link.href = url
          link.download = submission.fileName || 'submission'
          link.click()
        } else {
          window.open(url, '_blank', 'noopener,noreferrer')
        }
        window.setTimeout(() => URL.revokeObjectURL(url), 60000)
      } catch (requestError) {
        setError(requestError.response?.data?.message || 'Unable to open submission file.')
      }
    }

    setError('')

    try {
      const response = await api.put(`/api/instructor/submissions/${submissionId}/grade`, {
        grade,
        feedback: form.feedback ?? '',
      })
      setSubmissions((current) =>
        current.map((submission) => (submission.id === submissionId ? response.data.data : submission)),
      )
      setGradingSubmissionId(null)
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'Unable to grade submission.')
    }
  }

  const getFileIcon = (fileType) => {
    if (!fileType) return 'cloud'
    switch (fileType.toLowerCase()) {
      case 'pdf': return 'picture_as_pdf'
      case 'doc':
      case 'docx': return 'picture_as_pdf'
      case 'jpg':
      case 'jpeg':
      case 'png':
      case 'webp': return 'image'
      case 'gif': return 'image'
      case 'txt': return 'text_fields'
      default: return 'attach_file'
    }
  }

  const getFileColor = (fileType) => {
    if (!fileType) return 'grey'
    switch (fileType.toLowerCase()) {
      case 'pdf': return 'teal'
      case 'doc':
      case 'docx': return 'blue'
      case 'jpg':
      case 'jpeg':
      case 'png':
      case 'webp': return 'red'
      default: return 'grey'
    }
  }

  const statusLabel = (submission) => {
    if (submission.status === 'NOT_SUBMITTED') return 'Not submitted'
    if (submission.status === 'GRADED') return 'Graded'
    if (submission.status === 'LATE') return 'Submitted late - Not graded'
    return 'Submitted - Not graded'
  }

  return (
    <DashboardLayout role="INSTRUCTOR" title="Assignments" subtitle="Review learner submissions and monitor class activity.">
      {error && <p className="mb-4 rounded-xl bg-red-50 p-4 text-red-700">{error}</p>}
      {loading && <p className="text-slate-600">Loading assignments...</p>}
      {!loading && (
      <div className="space-y-4">
        {assignments.map((assignment) => (
          <div key={assignment.id} className="rounded-2xl bg-slate-50 p-5 ring-1 ring-slate-200">
            <div className="flex items-center justify-between gap-3">
              <div>
                <h2 className="text-xl font-bold text-slate-900">{assignment.title}</h2>
                <p className="mt-1 text-sm text-slate-500">Course #{assignment.courseId} Due {assignment.dueDate ? new Date(assignment.dueDate).toLocaleString() : 'No due date'}</p>
              </div>
              <button type="button" onClick={() => loadSubmissions(assignment.id)} className="rounded-full bg-violet-700 px-4 py-2 text-sm font-semibold text-white">View submissions</button>
            </div>
            {selectedAssignment === assignment.id && <div className="mt-4 space-y-3">{submissions.map((submission) => {
              const fileType = submission.fileType
              const filePath = submission.filePath
              const fileName = submission.fileName || (filePath ? filePath.split('/').pop() : '')
              
              return (
                <div key={submission.id} className="rounded-xl bg-white p-4 ring-1 ring-slate-200">
                  <div className="flex items-start gap-3">
                    <div className={`w-10 h-10 rounded-md bg-${getFileColor(fileType)} flex items-center justify-center text-sm text-white`}>
                      {getFileIcon(fileType)}
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="font-medium truncate">{submission.studentName || submission.studentUsername || 'Unknown student'}</p>
                      {submission.studentUsername && <p className="text-xs text-slate-400">@{submission.studentUsername}</p>}
                      <p className="text-xs text-slate-500">{fileName || 'No filename'}</p>
                    </div>
                  </div>
                  <p className={`mt-2 text-sm font-semibold ${submission.status === 'GRADED' ? 'text-emerald-700' : 'text-amber-700'}`}>
                    Status: {statusLabel(submission)}
                    {submission.grade !== null && submission.grade !== undefined && ` • ${submission.grade}/${assignment.maxMarks ?? 100}`}
                  </p>
                  {submission.feedback && submission.status === 'GRADED' && (
                    <p className="mt-1 text-sm text-slate-600">Feedback: {submission.feedback}</p>
                  )}
                  <div className="mt-3 flex flex-wrap gap-3">
                    {submission.submissionMethod === 'FILE' && submission.id && (
                      <>
                        <button type="button" onClick={() => openFile(submission)} className="text-violet-700 underline text-sm">Preview file</button>
                        <button type="button" onClick={() => openFile(submission, true)} className="text-violet-700 underline text-sm">Download</button>
                      </>
                    )}
                    {submission.submissionMethod === 'URL' && submission.filePath && (
                      <a href={submission.filePath} target="_blank" rel="noreferrer" className="text-violet-700 underline text-sm">Open URL</a>
                    )}
                    {submission.submissionMethod === 'TEXT' && submission.textContent && (
                      <details className="w-full text-sm"><summary className="cursor-pointer text-violet-700">Read response</summary><p className="mt-2 whitespace-pre-wrap text-slate-700">{submission.textContent}</p></details>
                    )}
                    {submission.status !== 'NOT_SUBMITTED' && <button
                      type="button"
                      onClick={() => openGrading(submission)}
                      className="rounded-lg bg-violet-700 px-3 py-2 text-sm font-semibold text-white"
                    >
                      {submission.status === 'GRADED' ? 'Edit grade' : 'Grade student'}
                    </button>}
                  </div>
                  {gradingSubmissionId === submission.id && (
                    <div className="mt-3">
                      <input type="number" min="0" max={assignment.maxMarks ?? 100} placeholder={`Grade (max ${assignment.maxMarks ?? 100})`} value={grades[submission.id]?.grade ?? submission.grade ?? ''} onChange={(event) => setGrades((current) => ({ ...current, [submission.id]: { ...current[submission.id], grade: event.target.value } }))} className="rounded-lg border px-3 py-2 w-full" />
                      <input type="text" placeholder="Feedback" value={grades[submission.id]?.feedback ?? submission.feedback ?? ''} onChange={(event) => setGrades((current) => ({ ...current, [submission.id]: { ...current[submission.id], feedback: event.target.value } }))} className="rounded-lg border px-3 py-2 w-full my-2" />
                      <div className="flex gap-2">
                        <button type="button" onClick={() => gradeSubmission(submission.id)} className="rounded-lg bg-violet-700 px-3 py-2 text-sm font-semibold text-white">Save grade</button>
                        <button type="button" onClick={() => setGradingSubmissionId(null)} className="rounded-lg border border-slate-300 px-3 py-2 text-sm font-semibold text-slate-700">Cancel</button>
                      </div>
                    </div>
                  )}
                </div>
              )
            })}{submissions.length === 0 && <p className="text-sm text-slate-500">No submissions yet.</p>}</div>}
          </div>
        ))}
        {!assignments.length && !error && <p className="text-sm text-slate-500">No assignments found across {courses.length} courses.</p>}
      </div>
      )}
    </DashboardLayout>
  )
}