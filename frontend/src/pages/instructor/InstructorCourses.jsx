import { useEffect, useState } from 'react'
import { DashboardLayout } from '../../layouts/DashboardLayout'
import api from '../../services/api'
import { COURSE_DURATIONS, COURSE_LEVELS, normalizeDuration, normalizeLevel } from '../../utils/courseOptions'
import CourseTabBar from '../../components/CourseTabBar'
import CourseSectionNav from '../../components/CourseSectionNav'
import {
  AssignmentsPanel,
  CourseOutline,
  LessonsPanel,
  MaterialsPanel,
} from '../../components/course-builder/CoursePanels'

const blankCourse = { title: '', description: '', price: 0, level: '', duration: '', categoryId: '' }
const blankModule = { title: '', description: '', orderNumber: 1 }
const blankLesson = { title: '', description: '', content: '', videoUrl: '', orderNumber: 1 }
const blankMaterial = { title: '', description: '', fileName: '', filePath: '', fileType: '' }
const blankAssignment = {
  title: '',
  description: '',
  dueDate: '',
  maxMarks: 100,
  allowedSubmissionMethods: ['FILE', 'URL', 'TEXT'],
  allowedFileTypes: ['PDF', 'DOC', 'DOCX', 'JPG', 'PNG', 'WEBP'],
  maxFileSize: 5 * 1024 * 1024,
  allowLateSubmissions: true,
}

export default function InstructorCourses() {
  const [courses, setCourses] = useState([])
  const [categories, setCategories] = useState([])
  const [selectedCourse, setSelectedCourse] = useState(null)
  const [modules, setModules] = useState([])
  const [lessons, setLessons] = useState({})
  const [materials, setMaterials] = useState({})
  const [assignments, setAssignments] = useState([])
  const [courseForm, setCourseForm] = useState(blankCourse)
  const [moduleForm, setModuleForm] = useState(blankModule)
  const [lessonForm, setLessonForm] = useState(blankLesson)
  const [materialForm, setMaterialForm] = useState(blankMaterial)
  const [assignmentForm, setAssignmentForm] = useState(blankAssignment)
  const [activeModule, setActiveModule] = useState(null)
  const [activeLesson, setActiveLesson] = useState(null)
  const [editing, setEditing] = useState({ course: null, module: null, lesson: null, material: null, assignment: null })
  const [notice, setNotice] = useState('')
  const [error, setError] = useState('')
  // Fallback to the mirrored constants; replaced by the server's list when it responds, so
  // the dropdown can never offer a value the backend would reject.
  const [levelOptions, setLevelOptions] = useState(COURSE_LEVELS)
  const [durationOptions, setDurationOptions] = useState(COURSE_DURATIONS)
  // Which builder section is open. Reset to 'modules' when a different course is selected.
  const [activeTab, setActiveTab] = useState('modules')

  const fail = (requestError) => setError(requestError.response?.data?.message || 'Request failed.')
  const refreshCourses = async () => { const response = await api.get('/api/instructor/courses'); setCourses(response.data?.data || []) }

  // Stable identity for the current module set, so the effect below refetches only when the
  // modules actually change rather than on every render.
  const moduleKey = modules.map((module) => module.id).join(',')

  useEffect(() => {
    Promise.all([refreshCourses(), api.get('/api/categories')]).then(([, response]) => setCategories(response.data?.data || [])).catch(fail)
  }, [])

  useEffect(() => {
    api.get('/api/courses/options')
      .then((response) => {
        const options = response.data?.data || {}
        if (options.levels?.length) setLevelOptions(options.levels.map((option) => option.value))
        if (options.durations?.length) setDurationOptions(options.durations.map((option) => option.value))
      })
      .catch(() => { /* keep the offline fallback; the dropdown still works */ })
  }, [])

  // The flat tabs need every module's lessons loaded before they can show anything.
  useEffect(() => {
    if (activeTab === 'lessons' || activeTab === 'materials' || activeTab === 'outline') {
      loadAllLessons()
    }
    // Keyed on the tab and the module ids only: loadAllLessons is recreated every render,
    // so depending on it would refetch on every keystroke.
  }, [activeTab, moduleKey])

  // Total lessons and materials across the course, so the tab badges are accurate without
  // loading every module's lessons up front.
  const lessonCount = Object.values(lessons).reduce((total, list) => total + list.length, 0)
  const materialCount = Object.values(materials).reduce((total, list) => total + list.length, 0)
  const tabCounts = {
    modules: modules.length,
    lessons: lessonCount,
    materials: materialCount,
    assignments: assignments.length,
  }

  const selectCourse = async (course) => {
    setSelectedCourse(course)
    setActiveTab('modules')
    // Normalise so a stored "BEGINNER" still matches the "Beginner" dropdown option,
    // otherwise editing the course would silently blank the field.
    setCourseForm({
      ...blankCourse,
      ...course,
      level: normalizeLevel(course.level) || '',
      duration: normalizeDuration(course.duration) || '',
      categoryId: course.category?.id || '',
    })
    try {
      const [moduleResponse, assignmentResponse] = await Promise.all([
        api.get(`/api/instructor/courses/${course.id}/modules`),
        api.get(`/api/instructor/courses/${course.id}/assignments`),
      ])
      setModules(moduleResponse.data?.data || [])
      setAssignments(assignmentResponse.data?.data || [])
      setLessons({})
      setMaterials({})
    } catch (requestError) { fail(requestError) }
  }

  const saveCourse = async (event) => {
    event.preventDefault()
    try {
      const payload = { ...courseForm, price: Number(courseForm.price), categoryId: Number(courseForm.categoryId) }
      const response = editing.course ? await api.put(`/api/instructor/courses/${editing.course}`, payload) : await api.post('/api/instructor/courses', payload)
      await refreshCourses()
      await selectCourse(response.data.data)
      setEditing((current) => ({ ...current, course: null }))
      setNotice('Course saved.')
    } catch (requestError) { fail(requestError) }
  }

  const deleteCourse = async () => {
    try { await api.delete(`/api/instructor/courses/${selectedCourse.id}`); setSelectedCourse(null); await refreshCourses(); setNotice('Course deleted.') } catch (requestError) { fail(requestError) }
  }

  const submitCourse = async () => {
    try { const response = await api.post(`/api/instructor/courses/${selectedCourse.id}/submit`); await refreshCourses(); await selectCourse(response.data.data); setNotice('Course submitted for approval.') } catch (requestError) { fail(requestError) }
  }

  const saveModule = async (event) => {
    event.preventDefault()
    try {
      const payload = { ...moduleForm, orderNumber: Number(moduleForm.orderNumber) }
      const response = editing.module ? await api.put(`/api/instructor/modules/${editing.module}`, payload) : await api.post(`/api/instructor/courses/${selectedCourse.id}/modules`, payload)
      setModules((current) => editing.module ? current.map((item) => item.id === editing.module ? response.data.data : item) : [...current, response.data.data])
      setModuleForm(blankModule); setEditing((current) => ({ ...current, module: null }))
    } catch (requestError) { fail(requestError) }
  }

  const deleteModule = async (id) => { try { await api.delete(`/api/instructor/modules/${id}`); setModules((current) => current.filter((item) => item.id !== id)) } catch (requestError) { fail(requestError) } }
  const loadLessons = async (moduleId) => { setActiveModule(moduleId); try { const response = await api.get(`/api/instructor/modules/${moduleId}/lessons`); setLessons((current) => ({ ...current, [moduleId]: response.data?.data || [] })) } catch (requestError) { fail(requestError) } }

  // Loads any module whose lessons are not in state yet. The Lessons, Materials and Outline
  // tabs all need every module's lessons to show anything, and each module is fetched
  // separately, so they are requested together and cached.
  const loadAllLessons = async () => {
    const missing = modules.filter((module) => !(module.id in lessons))
    if (missing.length === 0) return

    // Ignore the result if the user switched course mid-flight: the responses belong to
    // modules that are no longer being displayed.
    const courseId = selectedCourse?.id
    const requestedIds = missing.map((module) => module.id)

    try {
      const responses = await Promise.all(
        requestedIds.map((moduleId) => api.get(`/api/instructor/modules/${moduleId}/lessons`)))
      if (selectedCourse?.id !== courseId) return

      setLessons((current) => {
        const next = { ...current }
        responses.forEach((response, index) => {
          next[requestedIds[index]] = response.data?.data || []
        })
        return next
      })
    } catch (requestError) {
      fail(requestError)
    }
  }

  // Jumps to the lesson's module in the tree, then opens it so its material form is visible.
  const openLesson = async (moduleId, lessonId) => {
    setActiveTab('modules')
    await loadLessons(moduleId)
    setActiveLesson(lessonId)
    setEditing((current) => ({ ...current, material: null }))
    setMaterialForm(blankMaterial)
  }

  const saveLesson = async (event) => {
    event.preventDefault()
    try {
      const payload = { title: lessonForm.title, description: lessonForm.description, orderNumber: Number(lessonForm.orderNumber) }
      const response = editing.lesson ? await api.put(`/api/instructor/lessons/${editing.lesson}`, payload) : await api.post(`/api/instructor/modules/${activeModule}/lessons`, payload)
      setLessons((current) => ({ ...current, [activeModule]: editing.lesson ? (current[activeModule] || []).map((item) => item.id === editing.lesson ? response.data.data : item) : [...(current[activeModule] || []), response.data.data] }))
      setLessonForm(blankLesson); setEditing((current) => ({ ...current, lesson: null }))
    } catch (requestError) { fail(requestError) }
  }

  const deleteLesson = async (id) => { try { await api.delete(`/api/instructor/lessons/${id}`); setLessons((current) => ({ ...current, [activeModule]: (current[activeModule] || []).filter((item) => item.id !== id) })) } catch (requestError) { fail(requestError) } }
  // Switching lesson clears any in-progress material edit so it cannot be
  // submitted against the wrong lesson.
  const loadMaterials = async (lessonId) => { setActiveLesson(lessonId); setEditing((current) => ({ ...current, material: null })); setMaterialForm(blankMaterial); try { const response = await api.get(`/api/instructor/lessons/${lessonId}/materials`); setMaterials((current) => ({ ...current, [lessonId]: response.data?.data || [] })) } catch (requestError) { fail(requestError) } }
  const saveMaterial = async (event) => {
    event.preventDefault()
    try {
      const response = editing.material
        ? await api.put(`/api/instructor/materials/${editing.material}`, materialForm)
        : await api.post(`/api/instructor/lessons/${activeLesson}/materials`, materialForm)
      setMaterials((current) => ({
        ...current,
        [activeLesson]: editing.material
          ? (current[activeLesson] || []).map((item) => item.id === editing.material ? response.data.data : item)
          : [...(current[activeLesson] || []), response.data.data],
      }))
      setMaterialForm(blankMaterial)
      setEditing((current) => ({ ...current, material: null }))
    } catch (requestError) { fail(requestError) }
  }
  const deleteMaterial = async (id) => { try { await api.delete(`/api/instructor/materials/${id}`); setMaterials((current) => ({ ...current, [activeLesson]: (current[activeLesson] || []).filter((item) => item.id !== id) })) } catch (requestError) { fail(requestError) } }

  const saveAssignment = async (event) => {
    event.preventDefault()

    if (!assignmentForm.dueDate) {
      fail({ response: { data: { message: 'A due date is required.' } } })
      return
    }
    if (!assignmentForm.allowedSubmissionMethods?.length) {
      fail({ response: { data: { message: 'Select at least one submission method.' } } })
      return
    }

    try {
      const payload = {
        title: assignmentForm.title,
        description: assignmentForm.description,
        dueDate: new Date(assignmentForm.dueDate).toISOString(),
        maxMarks: Number(assignmentForm.maxMarks),
        allowedSubmissionMethods: assignmentForm.allowedSubmissionMethods,
        allowedFileTypes: assignmentForm.allowedFileTypes,
        maxFileSize: Number(assignmentForm.maxFileSize),
        allowLateSubmissions: assignmentForm.allowLateSubmissions,
      }
      const response = editing.assignment ? await api.put(`/api/instructor/assignments/${editing.assignment}`, payload) : await api.post(`/api/instructor/courses/${selectedCourse.id}/assignments`, payload)
      setAssignments((current) => editing.assignment ? current.map((item) => item.id === editing.assignment ? response.data.data : item) : [...current, response.data.data])
      setAssignmentForm(blankAssignment); setEditing((current) => ({ ...current, assignment: null }))
    } catch (requestError) { fail(requestError) }
  }

  const deleteAssignment = async (id) => { try { await api.delete(`/api/instructor/assignments/${id}`); setAssignments((current) => current.filter((item) => item.id !== id)) } catch (requestError) { fail(requestError) } }

  return (
    <DashboardLayout role="INSTRUCTOR" title="Course builder" subtitle="Create, structure, and submit your courses for approval.">
      {(notice || error) && <p className={`mb-5 rounded-xl p-4 text-sm ${error ? 'bg-red-50 text-red-700' : 'bg-emerald-50 text-emerald-700'}`}>{error || notice}</p>}
      <div className="grid gap-6 xl:grid-cols-[0.8fr_1.5fr]">
        <section className="space-y-4">
          {/* Jump list: keeps every area of the builder one click away, whichever tab is open. */}
          <CourseSectionNav
            sections={[
              { id: 'builder-course-details', label: 'Course details' },
              { id: 'builder-modules', label: 'Modules', count: modules.length },
              { id: 'builder-lessons', label: 'Lessons', count: lessonCount },
              { id: 'builder-materials', label: 'Materials', count: materialCount },
              { id: 'builder-assignments', label: 'Assignments', count: assignments.length },
              { id: 'builder-outline', label: 'Outline' },
            ]}
            className="rounded-2xl bg-white p-4 ring-1 ring-slate-200"
          />

          <form id="builder-course-details" onSubmit={saveCourse} className="scroll-mt-20 rounded-2xl bg-slate-50 p-5 ring-1 ring-slate-200">
            <h2 className="text-xl font-bold text-slate-900">{editing.course ? 'Edit course' : 'Create course'}</h2>
            <div className="mt-4 space-y-3">
              <input required placeholder="Course title" value={courseForm.title} onChange={(event) => setCourseForm({ ...courseForm, title: event.target.value })} className="w-full rounded-xl border border-slate-300 px-3 py-2" />
              <textarea placeholder="Description" value={courseForm.description || ''} onChange={(event) => setCourseForm({ ...courseForm, description: event.target.value })} className="w-full rounded-xl border border-slate-300 px-3 py-2" />
              <div className="grid gap-3 sm:grid-cols-2"><input required type="number" min="0" step="0.01" placeholder="Price" value={courseForm.price} onChange={(event) => setCourseForm({ ...courseForm, price: event.target.value })} className="rounded-xl border border-slate-300 px-3 py-2" /><select required value={courseForm.categoryId} onChange={(event) => setCourseForm({ ...courseForm, categoryId: event.target.value })} className="rounded-xl border border-slate-300 px-3 py-2"><option value="">Category</option>{categories.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}</select></div>
              <div className="grid gap-3 sm:grid-cols-2">
                <label className="block">
                  <span className="mb-1 block text-xs font-semibold text-slate-600">Level</span>
                  <select required value={courseForm.level || ''} onChange={(event) => setCourseForm({ ...courseForm, level: event.target.value })} className="w-full rounded-xl border border-slate-300 px-3 py-2">
                    <option value="">Select a level</option>
                    {levelOptions.map((option) => <option key={option} value={option}>{option}</option>)}
                  </select>
                </label>
                <label className="block">
                  <span className="mb-1 block text-xs font-semibold text-slate-600">Duration</span>
                  <select required value={courseForm.duration || ''} onChange={(event) => setCourseForm({ ...courseForm, duration: event.target.value })} className="w-full rounded-xl border border-slate-300 px-3 py-2">
                    <option value="">Select a duration</option>
                    {durationOptions.map((option) => <option key={option} value={option}>{option}</option>)}
                  </select>
                </label>
              </div>
              <button className="w-full rounded-xl bg-violet-700 px-4 py-2 font-semibold text-white">{editing.course ? 'Save changes' : 'Create course'}</button>
            </div>
          </form>
          {courses.map((course) => <button type="button" key={course.id} onClick={() => selectCourse(course)} className={`w-full rounded-2xl p-4 text-left ring-1 ${selectedCourse?.id === course.id ? 'bg-violet-50 ring-violet-300' : 'bg-white ring-slate-200'}`}><div className="flex items-center justify-between gap-3"><span className="font-semibold text-slate-900">{course.title}</span><span className="text-xs font-semibold text-slate-500">{course.status}</span></div></button>)}
        </section>
        <section className="rounded-2xl bg-white p-5 ring-1 ring-slate-200">
          {!selectedCourse ? <p className="text-slate-600">Select a course to manage modules, lessons, materials, assignments, and approval.</p> : <>
            <div className="flex flex-wrap items-center justify-between gap-3"><div><h2 className="text-2xl font-bold text-slate-900">{selectedCourse.title}</h2><p className="text-sm text-slate-500">Status: {selectedCourse.status}</p></div><div className="flex gap-2"><button type="button" onClick={() => setEditing({ ...editing, course: selectedCourse.id })} className="rounded-lg border px-3 py-2 text-sm font-semibold">Edit</button><button type="button" onClick={deleteCourse} className="rounded-lg border border-red-200 px-3 py-2 text-sm font-semibold text-red-700">Delete</button>{['DRAFT', 'REJECTED'].includes(selectedCourse.status) && <button type="button" onClick={submitCourse} className="rounded-lg bg-violet-700 px-3 py-2 text-sm font-semibold text-white">Submit approval</button>}</div></div>
            <div className="mt-6">
              <CourseTabBar counts={tabCounts} />
            </div>

            {activeTab === 'modules' && <div id="builder-modules" className="scroll-mt-20">
            <form onSubmit={saveModule} className="mt-6 flex flex-col gap-2 rounded-xl bg-slate-50 prounded-xl bg-slate-50 p-4 sm:flex-row"><input required placeholder="Module title" value={moduleForm.title} onChange={(event) => setModuleForm({ ...moduleForm, title: event.target.value })} className="flex-1 rounded-lg border px-3 py-2" /><input required type="number" min="1" value={moduleForm.orderNumber} onChange={(event) => setModuleForm({ ...moduleForm, orderNumber: event.target.value })} className="w-24 rounded-lg border px-3 py-2" /><button className="rounded-lg bg-slate-900 px-3 py-2 text-sm font-semibold text-white">{editing.module ? 'Update module' : 'Add module'}</button></form>
            <div className="mt-4 space-y-3">{modules.map((module) => <div key={module.id} className="rounded-xl border p-4"><div className="flex items-center justify-between gap-3"><button type="button" onClick={() => loadLessons(module.id)} className="font-semibold underline">{module.orderNumber}. {module.title}</button><div className="flex gap-2"><button type="button" onClick={() => { setEditing({ ...editing, module: module.id }); setModuleForm(module) }} className="text-xs">Edit</button><button type="button" onClick={() => deleteModule(module.id)} className="text-xs text-red-700">Delete</button></div></div>{activeModule === module.id && <div className="mt-3 pl-3"><form onSubmit={saveLesson} className="flex flex-col gap-2 rounded-lg bg-slate-50 p-3 sm:flex-row"><input required placeholder="Lesson title" value={lessonForm.title} onChange={(event) => setLessonForm({ ...lessonForm, title: event.target.value })} className="flex-1 rounded-lg border px-3 py-2 text-sm" /><input required type="number" min="1" value={lessonForm.orderNumber} onChange={(event) => setLessonForm({ ...lessonForm, orderNumber: event.target.value })} className="w-20 rounded-lg border px-3 py-2 text-sm" /><button className="rounded-lg bg-violet-700 px-3 py-2 text-xs font-semibold text-white">{editing.lesson ? 'Update' : 'Add lesson'}</button></form>{(lessons[module.id] || []).map((lesson) => <div key={lesson.id} className="mt-3 rounded-lg border p-3"><div className="flex items-center justify-between"><button type="button" onClick={() => loadMaterials(lesson.id)} className="text-sm font-semibold underline">{lesson.orderNumber}. {lesson.title}</button><div className="flex gap-2"><button type="button" onClick={() => { setEditing({ ...editing, lesson: lesson.id }); setLessonForm(lesson) }} className="text-xs">Edit</button><button type="button" onClick={() => deleteLesson(lesson.id)} className="text-xs text-red-700">Delete</button></div></div>{activeLesson === lesson.id && <div className="mt-3"><form onSubmit={saveMaterial} className="grid gap-2 rounded-lg bg-slate-50 p-3 sm:grid-cols-2"><input required placeholder="Material title" value={materialForm.title} onChange={(event) => setMaterialForm({ ...materialForm, title: event.target.value })} className="rounded border px-2 py-1 text-sm" /><input required placeholder="File name" value={materialForm.fileName} onChange={(event) => setMaterialForm({ ...materialForm, fileName: event.target.value })} className="rounded border px-2 py-1 text-sm" /><input required placeholder="File URL/path" value={materialForm.filePath} onChange={(event) => setMaterialForm({ ...materialForm, filePath: event.target.value })} className="rounded border px-2 py-1 text-sm" /><button className="rounded bg-slate-900 px-2 py-1 text-sm font-semibold text-white">{editing.material ? 'Update material' : 'Add material'}</button></form>{(materials[lesson.id] || []).map((material) => <div key={material.id} className="mt-2 flex justify-between text-sm"><a href={material.filePath} target="_blank" rel="noreferrer" className="text-teal-700 underline">{material.title}</a><div className="flex gap-2"><button type="button" onClick={() => { setEditing({ ...editing, material: material.id }); setMaterialForm(material) }} className="text-violet-700">Edit</button><button type="button" onClick={() => deleteMaterial(material.id)} className="text-red-700">Delete</button></div></div>)}</div>}</div>)}</div>}</div>)}</div>
            </div>}

            {activeTab === 'lessons' && (
              <div id="builder-lessons" className="scroll-mt-20">
              <LessonsPanel
                modules={modules}
                lessons={lessons}
                onOpenLesson={openLesson}
                onEditLesson={(lesson) => { setEditing((current) => ({ ...current, lesson: lesson.id })); setLessonForm(lesson) }}
                onDeleteLesson={deleteLesson}
                activeLesson={activeLesson}
              />
              </div>
            )}

            {activeTab === 'materials' && (
              <div id="builder-materials" className="scroll-mt-20">
              <MaterialsPanel
                lessons={lessons}
                modules={modules}
                materials={materials}
                onOpenLesson={openLesson}
                onEditMaterial={(material) => { setEditing((current) => ({ ...current, material: material.id })); setMaterialForm(material) }}
                onDeleteMaterial={deleteMaterial}
              />
              </div>
            )}

            {activeTab === 'assignments' && (
              <div id="builder-assignments" className="scroll-mt-20">
              <AssignmentsPanel
                assignments={assignments}
                form={assignmentForm}
                setForm={setAssignmentForm}
                editingId={editing.assignment}
                onSubmit={saveAssignment}
                onEdit={(assignment) => { setEditing((current) => ({ ...current, assignment: assignment.id })); setAssignmentForm({ title: assignment.title, description: assignment.description || '', dueDate: assignment.dueDate ? assignment.dueDate.slice(0, 16) : '', maxMarks: assignment.maxMarks ?? 100, allowedSubmissionMethods: assignment.allowedSubmissionMethods || ['FILE', 'URL', 'TEXT'], allowedFileTypes: assignment.allowedFileTypes || ['PDF', 'DOC', 'DOCX', 'JPG', 'PNG', 'WEBP'], maxFileSize: assignment.maxFileSize || 5 * 1024 * 1024, allowLateSubmissions: assignment.allowLateSubmissions !== false }) }}
                onDelete={deleteAssignment}
              />
              </div>
            )}

            {activeTab === 'outline' && (
              <div id="builder-outline" className="scroll-mt-20">
              <CourseOutline
                modules={modules}
                lessons={lessons}
                materials={materials}
                assignments={assignments}
                onOpenModule={loadLessons}
              />
              </div>
            )}
          </>}
        </section>
      </div>
    </DashboardLayout>
  )
}
