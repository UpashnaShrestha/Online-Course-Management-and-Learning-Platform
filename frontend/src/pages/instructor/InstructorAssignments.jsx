import { useEffect, useState } from 'react'
import { DashboardLayout } from '../../layouts/DashboardLayout'
import api from '../../services/api'

export default function InstructorAssignments() {
  const [courses, setCourses] = useState([])
  const [assignments, setAssignments] = useState([])
  const [submissions, setSubmissions] = useState([])
  const [selectedCourseId, setSelectedCourseId] = useState(null)
  const [visibleAssignments, setVisibleAssignments] = useState([])
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

  const loadCourseSubmissions = async (course) => {
    setSelectedCourseId(course.id)
    setVisibleAssignments(assignments.filter((assignment) => assignment.courseId === course.id))
    setGradingSubmissionId(null)
    setSubmissions([])
    try {
      const courseAssignments = assignments.filter((assignment) => assignment.courseId === course.id)
      setVisibleAssignments(courseAssignments)
      const responses = await Promise.all(
        courseAssignments.map((assignment) =>
          api.get(`/api/instructor/assignments/${assignment.id}/submissions`)
        ),
      )
      setSubmissions(
        responses.flatMap((response, index) =>
          (response.data?.data || [])
            .filter((submission) => submission.status !== 'NOT_SUBMITTED')
            .map((submission) => ({ ...submission, assignmentTitle: courseAssignments[index].title })),
        ),
      )
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
    if (submission.status === 'GRADED') return submission.late ? 'Graded - submitted late' : 'Graded'
    if (submission.status === 'LATE' || submission.late) return 'Late submission - submitted after the deadline'
    return 'Submitted - Not graded'
  }

  const submissionsByStudent = Array.from(
    submissions.reduce((groups, submission) => {
      const key = submission.studentId
      if (!groups.has(key)) {
        groups.set(key, {
          studentId: key,
          studentName: submission.studentName || submission.studentUsername || 'Unknown student',
          studentUsername: submission.studentUsername,
          submissions: [],
        })
      }
      groups.get(key).submissions.push(submission)
      return groups
    }, new Map()).values(),
  )

  return (
    <DashboardLayout role="INSTRUCTOR" title="Assignments" subtitle="Review learner submissions and monitor class activity.">
      {error && <p className="mb-4 rounded-xl bg-red-50 p-4 text-red-700">{error}</p>}
      {loading && <p className="text-slate-600">Loading assignments...</p>}
      {!loading && (
      <div className="space-y-4">
        {courses.map((course) => (
          <section key={course.id} className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
            <header className="flex items-center justify-between gap-4 border-b border-slate-200 bg-slate-50 px-5 py-4">
              <div>
                <p className="text-xs font-semibold uppercase tracking-wide text-violet-700">Course submissions</p>
                <h2 className="mt-1 text-xl font-bold text-slate-900">{course.title}</h2>
              </div>
              <button type="button" onClick={() => loadCourseSubmissions(course)} className="shrink-0 rounded-lg bg-violet-700 px-4 py-2 text-sm font-semibold text-white">
                {selectedCourseId === course.id ? 'Refresh submissions' : 'View submissions'}
              </button>
            </header>
            {selectedCourseId === course.id && (
              <div className="border-t-0">
                <div className="grid grid-cols-2 border-b border-slate-200 bg-white sm:grid-cols-4">
                  <div className="border-b border-slate-200 px-5 py-3 sm:border-b-0 sm:border-r">
                    <p className="text-xs text-slate-500">Assignments</p>
                    <p className="mt-1 text-lg font-bold text-slate-900">{visibleAssignments.length}</p>
                  </div>
                  <div className="border-b border-slate-200 px-5 py-3 sm:border-b-0 sm:border-r">
                    <p className="text-xs text-slate-500">Students</p>
                    <p className="mt-1 text-lg font-bold text-slate-900">{submissionsByStudent.length}</p>
                  </div>
                  <div className="border-r border-slate-200 px-5 py-3">
                    <p className="text-xs text-slate-500">Submissions</p>
                    <p className="mt-1 text-lg font-bold text-slate-900">{submissions.length}</p>
                  </div>
                  <div className="px-5 py-3">
                    <p className="text-xs text-slate-500">Graded</p>
                    <p className="mt-1 text-lg font-bold text-emerald-700">{submissions.filter((submission) => submission.status === 'GRADED').length}</p>
                  </div>
                </div>
                {submissionsByStudent.length > 0 ? (
                  <div className="overflow-x-auto">
                  <table className="w-full min-w-[1100px] text-left text-sm text-slate-700">
                    <thead className="bg-slate-100 text-xs uppercase tracking-wide text-slate-600">
                      <tr>
                        <th className="w-16 border-b px-4 py-3 text-center">S/N</th>
                        <th className="w-64 border-b px-4 py-3">Student</th>
                        <th className="border-b px-4 py-3">Assignment submissions</th>
                      </tr>
                    </thead>
                    <tbody>
                      {submissionsByStudent.map((student, index) => (
                        <tr key={student.studentId} className="border-t border-slate-200 align-top">
                          <td className="border-r px-4 py-5 text-center font-bold text-slate-500">{index + 1}</td>
                          <td className="border-r px-4 py-5">
                            <div className="flex items-center gap-3">
                              <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-violet-100 text-sm font-bold text-violet-700">
                                {student.studentName.slice(0, 1).toUpperCase()}
                              </div>
                              <div>
                                <p className="font-semibold text-slate-900">{student.studentName}</p>
                                {student.studentUsername && <p className="mt-0.5 text-xs text-slate-500">@{student.studentUsername}</p>}
                              </div>
                            </div>
                          </td>
                          <td className="px-4 py-5">
                            <div className="overflow-x-auto rounded-lg border border-slate-200">
                              <table className="w-full min-w-[780px] text-xs">
                                <thead className="bg-slate-50 text-slate-600">
                                  <tr>
                                    <th className="border-b px-3 py-2.5 text-left">Assignment</th>
                                    <th className="border-b px-3 py-2.5 text-left">Submitted at</th>
                                    <th className="border-b px-3 py-2.5 text-left">Status</th>
                                    <th className="border-b px-3 py-2.5 text-left">Submission</th>
                                    <th className="border-b px-3 py-2.5 text-left">Grade / Feedback</th>
                                    <th className="border-b px-3 py-2.5 text-left">Action</th>
                                  </tr>
                                </thead>
                                <tbody>
                                  {student.submissions.map((submission) => {
                                    const assignment = visibleAssignments.find((item) => item.id === submission.assignmentId)
                                    const fileName = submission.fileName || (submission.filePath ? submission.filePath.split('/').pop() : '')
                                    return (
                                      <tr key={submission.id} className="border-b border-slate-100">
                                        <td className="px-3 py-3 font-semibold text-slate-900">{submission.assignmentTitle}</td>
                                        <td className="px-3 py-3 whitespace-nowrap text-slate-600">{submission.submittedAt ? new Date(submission.submittedAt).toLocaleString() : '—'}</td>
                                        <td className="px-3 py-3">
                                          <span className={`inline-block rounded-full px-2.5 py-1 font-semibold ${submission.status === 'GRADED' ? 'bg-emerald-100 text-emerald-700' : submission.status === 'LATE' || submission.late ? 'bg-amber-100 text-amber-700' : 'bg-sky-100 text-sky-700'}`}>
                                            {statusLabel(submission)}
                                          </span>
                                        </td>
                                        <td className="px-3 py-3">
                                          {submission.submissionMethod === 'FILE' && (
                                            <div className="flex gap-2">
                                              <button type="button" onClick={() => openFile(submission)} className="text-violet-700 underline">Preview</button>
                                              <button type="button" onClick={() => openFile(submission, true)} className="text-violet-700 underline">Download</button>
                                            </div>
                                          )}
                                          {submission.submissionMethod === 'URL' && submission.filePath && (
                                            <a href={submission.filePath} target="_blank" rel="noreferrer" className="text-violet-700 underline">Open URL</a>
                                          )}
                                          {submission.submissionMethod === 'TEXT' && submission.textContent && (
                                            <details><summary className="cursor-pointer text-violet-700">Read response</summary><p className="mt-2 max-w-md whitespace-pre-wrap text-slate-700">{submission.textContent}</p></details>
                                          )}
                                          {submission.submissionMethod === 'FILE' && <p className="mt-1 text-slate-500">{fileName || 'No filename'}</p>}
                                        </td>
                                        <td className="min-w-44 px-3 py-3">
                                          {submission.status === 'GRADED' && submission.grade != null && <p>{submission.grade}/{assignment?.maxMarks ?? 100}</p>}
                                          {submission.feedback && <p className="mt-1 text-slate-600">{submission.feedback}</p>}
                                          {!submission.grade && !submission.feedback && <span className="text-slate-400">Not graded</span>}
                                        </td>
                                        <td className="min-w-44 px-3 py-3">
                                          {gradingSubmissionId === submission.id ? (
                                            <div className="space-y-2">
                                              <input type="number" min="0" max={assignment?.maxMarks ?? 100} placeholder="Grade" value={grades[submission.id]?.grade ?? submission.grade ?? ''} onChange={(event) => setGrades((current) => ({ ...current, [submission.id]: { ...current[submission.id], grade: event.target.value } }))} className="w-full rounded border px-2 py-1" />
                                              <input type="text" placeholder="Feedback" value={grades[submission.id]?.feedback ?? submission.feedback ?? ''} onChange={(event) => setGrades((current) => ({ ...current, [submission.id]: { ...current[submission.id], feedback: event.target.value } }))} className="w-full rounded border px-2 py-1" />
                                              <div className="flex gap-2">
                                                <button type="button" onClick={() => gradeSubmission(submission.id)} className="rounded bg-violet-700 px-2 py-1 text-white">Save</button>
                                                <button type="button" onClick={() => setGradingSubmissionId(null)} className="rounded border px-2 py-1">Cancel</button>
                                              </div>
                                            </div>
                                          ) : (
                                            <button type="button" onClick={() => openGrading(submission)} className="rounded bg-violet-700 px-3 py-2 text-white">
                                              {submission.status === 'GRADED' ? 'Edit grade' : 'Grade'}
                                            </button>
                                          )}
                                        </td>
                                      </tr>
                                    )
                                  })}
                                </tbody>
                              </table>
                            </div>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                  </div>
                ) : (
                  <p className="p-5 text-sm text-slate-500">No submissions yet for this course.</p>
                )}
              </div>
            )}
          </section>
        ))}
        {!assignments.length && !error && <p className="text-sm text-slate-500">No assignments found across {courses.length} courses.</p>}
      </div>
      )}
    </DashboardLayout>
  )
}