import { BookOpen, ClipboardCheck, FileText, FolderTree } from 'lucide-react'

/**
 * The "Lessons" tab: every lesson in the course as one flat list.
 *
 * The modules tab shows lessons nested inside the module they belong to, which is the right
 * shape for rearranging content but means seeing every lesson means expanding each module by
 * hand. This tab loads whatever is not loaded yet and lists them all together.
 */

/** Flattens modules + the loaded lessons map into a single ordered list. */
export function flattenLessons(modules, lessons) {
  const ordered = [...modules].sort((a, b) => (a.orderNumber ?? 0) - (b.orderNumber ?? 0))

  return ordered.flatMap((module) =>
    [...(lessons[module.id] ?? [])]
      .sort((a, b) => (a.orderNumber ?? 0) - (b.orderNumber ?? 0))
      .map((lesson) => ({ ...lesson, moduleId: module.id, moduleTitle: module.title }))
  )
}

export function LessonsPanel({
  modules,
  lessons,
  onOpenLesson,
  onEditLesson,
  onDeleteLesson,
  activeLesson,
}) {
  const rows = flattenLessons(modules, lessons)

  // A lesson can only be opened from inside its module, so when nothing is loaded yet say
  // so instead of implying the course has no lessons.
  const nothingLoaded = modules.some((module) => !(module.id in lessons))

  if (modules.length === 0) {
    return (
      <EmptyPanel
        icon={FolderTree}
        title="No modules yet"
        message="Add a module on the Modules tab first — every lesson lives inside one."
      />
    )
  }

  return (
    <div className="mt-6">
      <p className="mb-3 text-sm text-slate-600">
        {rows.length} {rows.length === 1 ? 'lesson' : 'lessons'} across {modules.length}{' '}
        {modules.length === 1 ? 'module' : 'modules'}.
      </p>

      {rows.length === 0 && nothingLoaded ? (
        <EmptyPanel
          icon={BookOpen}
          title="Loading lessons..."
          message="Fetching the lessons for each module."
        />
      ) : rows.length === 0 ? (
        <EmptyPanel
          icon={BookOpen}
          title="No lessons yet"
          message="Open a module on the Modules tab and add its first lesson."
        />
      ) : (
        <ul className="space-y-2">
          {rows.map((lesson) => (
            <li
              key={lesson.id}
              className={`flex flex-wrap items-center justify-between gap-3 rounded-xl border p-3 ${
                activeLesson === lesson.id ? 'border-violet-300 bg-violet-50' : 'border-slate-200'
              }`}
            >
              <div className="min-w-0">
                <p className="font-semibold text-slate-900">
                  <span className="text-slate-400">{lesson.orderNumber}.</span> {lesson.title}
                </p>
                <p className="mt-0.5 text-xs text-slate-500">
                  in {lesson.moduleTitle}
                  {lesson.duration ? ` · ${lesson.duration}` : ''}
                </p>
              </div>

              <div className="flex shrink-0 gap-2 text-xs">
                <button
                  type="button"
                  onClick={() => onOpenLesson(lesson.moduleId, lesson.id)}
                  className="font-semibold text-violet-700 underline"
                >
                  Manage materials
                </button>
                <button type="button" onClick={() => onEditLesson(lesson)} className="text-slate-600">
                  Edit
                </button>
                <button type="button" onClick={() => onDeleteLesson(lesson.id)} className="text-red-700">
                  Delete
                </button>
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}

/**
 * The "Materials" tab: every downloadable file in the course, grouped by its lesson.
 *
 * Materials are only reachable by expanding a lesson inside a module inside the Modules tab,
 * so on a course with real content they are several clicks down.
 */
export function MaterialsPanel({
  modules,
  lessons,
  materials,
  onOpenLesson,
  onEditMaterial,
  onDeleteMaterial,
}) {
  const nothingLoaded = modules.some((module) => !(module.id in lessons))

  const rows = flattenLessons(modules, lessons).flatMap((lesson) =>
    (materials[lesson.id] ?? []).map((material) => ({ ...material, lessonTitle: lesson.title }))
  )

  const grouped = new Map()
  for (const row of rows) {
    if (!grouped.has(row.lessonTitle)) grouped.set(row.lessonTitle, [])
    grouped.get(row.lessonTitle).push(row)
  }

  return (
    <div className="mt-6">
      {rows.length === 0 && nothingLoaded ? (
        <EmptyPanel
          icon={FileText}
          title="Loading materials..."
          message="Fetching lessons and their files."
        />
      ) : rows.length === 0 ? (
        <EmptyPanel
          icon={FileText}
          title="No materials yet"
          message="Open a lesson and attach a file for students to download."
        />
      ) : (
        <>
          <p className="mb-3 text-sm text-slate-600">
            {rows.length} {rows.length === 1 ? 'file' : 'files'} across{' '}
            {grouped.size} {grouped.size === 1 ? 'lesson' : 'lessons'}.
          </p>

          <div className="space-y-4">
            {[...grouped.entries()].map(([lessonTitle, list]) => (
              <section key={lessonTitle} className="rounded-xl border border-slate-200 p-4">
                <h4 className="font-semibold text-slate-900">{lessonTitle}</h4>
                <ul className="mt-3 space-y-2">
                  {list.map((material) => (
                    <li key={material.id} className="flex items-center justify-between gap-3 text-sm">
                      <a
                        href={material.filePath}
                        target="_blank"
                        rel="noreferrer"
                        className="truncate font-medium text-teal-700 underline"
                      >
                        {material.title}
                      </a>
                      <div className="flex shrink-0 gap-2 text-xs">
                        {material.fileType && (
                          <span className="rounded bg-slate-100 px-1.5 py-0.5 text-slate-600">
                            {material.fileType}
                          </span>
                        )}
                        <button type="button" onClick={() => onEditMaterial(material)} className="text-violet-700">
                          Edit
                        </button>
                        <button
                          type="button"
                          onClick={() => onDeleteMaterial(material.id)}
                          className="text-red-700"
                        >
                          Delete
                        </button>
                      </div>
                    </li>
                  ))}
                </ul>
              </section>
            ))}
          </div>
        </>
      )}
    </div>
  )
}

/** The "Assignments" tab: create and manage the graded work. */
export function AssignmentsPanel({
  assignments,
  form,
  setForm,
  editingId,
  onSubmit,
  onEdit,
  onDelete,
}) {
  return (
    <div className="mt-6">
      <form
        onSubmit={onSubmit}
        className="grid gap-2 rounded-xl bg-slate-50 p-4 sm:grid-cols-2"
      >
        <input
          required
          placeholder="Assignment title"
          value={form.title}
          onChange={(event) => setForm({ ...form, title: event.target.value })}
          className="rounded-lg border px-3 py-2"
        />
        <input
          required
          type="datetime-local"
          value={form.dueDate}
          onChange={(event) => setForm({ ...form, dueDate: event.target.value })}
          className="rounded-lg border px-3 py-2"
        />
        <textarea
          placeholder="Description"
          value={form.description}
          onChange={(event) => setForm({ ...form, description: event.target.value })}
          className="rounded-lg border px-3 py-2 sm:col-span-2"
        />
        <label className="text-sm text-slate-600">
          Maximum marks
          <input required type="number" min="1" step="0.01" value={form.maxMarks} onChange={(event) => setForm({ ...form, maxMarks: event.target.value })} className="mt-1 w-full rounded-lg border px-3 py-2" />
        </label>
        <label className="text-sm text-slate-600">
          Maximum file size (MB)
          <input required type="number" min="1" value={Math.round((form.maxFileSize || 5242880) / 1024 / 1024)} onChange={(event) => setForm({ ...form, maxFileSize: Number(event.target.value) * 1024 * 1024 })} className="mt-1 w-full rounded-lg border px-3 py-2" />
        </label>
        <fieldset className="sm:col-span-2">
          <legend className="text-sm font-semibold text-slate-700">Allowed submission methods</legend>
          <div className="mt-1 flex flex-wrap gap-3 text-sm">
            {['FILE', 'URL', 'TEXT'].map((method) => (
              <label key={method} className="flex items-center gap-1">
                <input type="checkbox" checked={form.allowedSubmissionMethods?.includes(method)} onChange={(event) => setForm({ ...form, allowedSubmissionMethods: event.target.checked ? [...(form.allowedSubmissionMethods || []), method] : (form.allowedSubmissionMethods || []).filter((item) => item !== method) })} />
                {method}
              </label>
            ))}
          </div>
        </fieldset>
        <label className="text-sm text-slate-600 sm:col-span-2">
          Allowed file types (comma separated)
          <input value={(form.allowedFileTypes || []).join(', ')} onChange={(event) => setForm({ ...form, allowedFileTypes: event.target.value.split(',').map((item) => item.trim().toUpperCase()).filter(Boolean) })} className="mt-1 w-full rounded-lg border px-3 py-2" />
        </label>
        <button className="rounded-lg bg-slate-900 px-3 py-2 text-sm font-semibold text-white">
          {editingId ? 'Update assignment' : 'Add assignment'}
        </button>
        {editingId && (
          <p className="text-xs text-slate-500 sm:col-span-2">
            Editing an existing assignment — save to apply, or select another to cancel.
          </p>
        )}
      </form>

      {assignments.length === 0 ? (
        <EmptyPanel
          icon={ClipboardCheck}
          title="No assignments yet"
          message="Add the first assignment above. Students see it once they enrol."
        />
      ) : (
        <ul className="mt-4 space-y-2">
          {assignments.map((assignment) => (
            <li
              key={assignment.id}
              className="flex items-center justify-between gap-3 rounded-lg border p-3"
            >
              <div className="min-w-0">
                <p className="truncate font-semibold">{assignment.title}</p>
                <p className="text-xs text-slate-500">
                  Due{' '}
                  {assignment.dueDate
                    ? new Date(assignment.dueDate).toLocaleString()
                    : 'No due date'}
                </p>
              </div>
              <div className="flex shrink-0 gap-2 text-xs">
                <button type="button" onClick={() => onEdit(assignment)}>Edit</button>
                <button
                  type="button"
                  onClick={() => onDelete(assignment.id)}
                  className="text-red-700"
                >
                  Delete
                </button>
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}

/** The "Outline" tab: a read-only view of the whole course structure. */
export function CourseOutline({ modules, lessons, materials, assignments, onOpenModule }) {
  const ordered = [...modules].sort((a, b) => (a.orderNumber ?? 0) - (b.orderNumber ?? 0))

  return (
    <div className="mt-6">
      <dl className="grid gap-3 sm:grid-cols-4">
        {[
          { label: 'Modules', value: modules.length },
          { label: 'Lessons', value: flattenLessons(modules, lessons).length },
          {
            label: 'Materials',
            value: Object.values(materials).reduce((total, list) => total + list.length, 0),
          },
          { label: 'Assignments', value: assignments.length },
        ].map((stat) => (
          <div key={stat.label} className="rounded-xl bg-slate-50 p-4 text-center ring-1 ring-slate-200">
            <dt className="text-xs font-semibold uppercase tracking-wide text-slate-500">
              {stat.label}
            </dt>
            <dd className="mt-1 text-2xl font-bold text-slate-900">{stat.value}</dd>
          </div>
        ))}
      </dl>

      <h4 className="mt-6 font-semibold text-slate-900">Structure</h4>
      {ordered.length === 0 ? (
        <p className="mt-2 text-sm text-slate-600">Nothing here yet. Start on the Modules tab.</p>
      ) : (
        <ol className="mt-3 space-y-3">
          {ordered.map((module) => {
            const moduleLessons = flattenLessons({ id: module.id, orderNumber: module.orderNumber, title: module.title }, lessons)
            return (
              <li key={module.id} className="rounded-xl border border-slate-200 p-4">
                <button
                  type="button"
                  onClick={() => onOpenModule(module.id)}
                  className="text-left font-semibold text-slate-900 underline decoration-slate-300"
                >
                  {module.orderNumber}. {module.title}
                </button>
                {moduleLessons.length === 0 ? (
                  <p className="mt-2 text-sm text-slate-500">No lessons in this module yet.</p>
                ) : (
                  <ul className="mt-2 space-y-1 text-sm text-slate-600">
                    {moduleLessons.map((lesson) => (
                      <li key={lesson.id}>
                        {lesson.orderNumber}. {lesson.title}
                        {(materials[lesson.id] ?? []).length > 0 && (
                          <span className="ml-2 rounded bg-slate-100 px-1.5 py-0.5 text-xs">
                            {(materials[lesson.id] ?? []).length}{' '}
                            {(materials[lesson.id] ?? []).length === 1 ? 'file' : 'files'}
                          </span>
                        )}
                      </li>
                    ))}
                  </ul>
                )}
              </li>
            )
          })}
        </ol>
      )}
    </div>
  )
}

function EmptyPanel({ icon: Icon, title, message }) {
  return (
    <div className="mt-6 rounded-xl border border-dashed border-slate-300 bg-slate-50 p-8 text-center">
      <span className="mx-auto flex h-11 w-11 items-center justify-center rounded-2xl bg-white text-slate-400 ring-1 ring-slate-200">
        <Icon size={20} aria-hidden="true" />
      </span>
      <p className="mt-3 font-semibold text-slate-900">{title}</p>
      <p className="mx-auto mt-1 max-w-sm text-sm text-slate-600">{message}</p>
    </div>
  )
}