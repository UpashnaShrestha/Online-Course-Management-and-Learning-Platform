import { useEffect, useId, useRef, useState } from 'react'
import { Image as ImageIcon, Loader2, Trash2, Upload } from 'lucide-react'
import api from '../services/api'
import { resolveImageUrl } from '../utils/format'
import { describeSize, fitWithin, prepareAvatarImage } from '../utils/imageUpload'

/**
 * Profile picture picker.
 *
 * Offers both routes the user asked for: choose a photo from the device (which opens the
 * gallery on a phone), or paste a URL for an image hosted elsewhere. Both write to the same
 * `profileImage` field, so the rest of the app does not care which was used.
 *
 * The picked file is downscaled in the browser before upload -- see utils/imageUpload.js for
 * why -- and the preview below shows exactly what will be saved.
 *
 * @param {string}   value       the current image URL, or ''
 * @param {Function} onChange    called with the new URL, or '' when the picture is removed
 * @param {string}   initials    fallback shown when there is no picture
 */
export default function AvatarUpload({ value = '', onChange, initials = 'U', disabled = false }) {
  const inputId = useId()
  const inputRef = useRef(null)
  const [preview, setPreview] = useState('')
  const [pending, setPending] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [dragOver, setDragOver] = useState(false)

  // A local object URL must be revoked, or each preview leaks the whole decoded image.
  useEffect(() => {
    if (!preview) return undefined
    return () => URL.revokeObjectURL(preview)
  }, [preview])

  useEffect(() => {
    if (!error) return undefined
    const timer = setTimeout(() => setError(''), 6000)
    return () => clearTimeout(timer)
  }, [error])

  const shown = preview || value

  const handleFile = async (file) => {
    if (!file) return

    setError('')
    setNotice('')
    setPending(true)

    try {
      const { blob, width, height } = await prepareAvatarImage(file)

      if (preview) URL.revokeObjectURL(preview)
      setPreview(URL.createObjectURL(blob))

      const form = new FormData()
      // The filename is advisory; the server sniffs the real type and renames the file.
      form.append('file', blob, 'avatar.jpg')

      const response = await api.post('/api/users/me/profile-image', form)
      const saved = response.data?.data?.profileImage ?? ''

      onChange?.(saved)
      setNotice(`Saved as ${width}×${height}, ${describeSize(blob.size)}.`)
    } catch (uploadError) {
      setError(
        uploadError.response?.data?.message
        || uploadError.message
        || 'Could not upload that image. Please try another file.'
      )
    } finally {
      setPending(false)
      // Allow re-picking the same file after a failure.
      if (inputRef.current) inputRef.current.value = ''
    }
  }

  const handleRemove = async () => {
    setError('')
    setNotice('')
    setPending(true)

    try {
      // Only call the API when the picture is one we uploaded; a remote URL has no file
      // behind it to delete.
      if (value && value.startsWith('/uploads/')) {
        await api.delete('/api/users/me/profile-image')
      }
      if (preview) URL.revokeObjectURL(preview)
      setPreview('')
      onChange?.('')
      setNotice('Profile picture removed.')
    } catch (removeError) {
      setError(removeError.response?.data?.message || 'Could not remove the picture.')
    } finally {
      setPending(false)
    }
  }

  const onDrop = (event) => {
    event.preventDefault()
    setDragOver(false)
    if (disabled) return
    handleFile(event.dataTransfer?.files?.[0])
  }

  return (
    <div className="rounded-2xl bg-slate-50 p-4 ring-1 ring-slate-200">
      <div className="flex flex-wrap items-center gap-4">
        {/* Preview */}
        <div className="relative shrink-0">
          {shown ? (
            <img
              src={resolveImageUrl(shown)}
              alt="Profile picture preview"
              className="h-20 w-20 rounded-full object-cover ring-2 ring-white"
            />
          ) : (
            <span
              aria-hidden="true"
              className="flex h-20 w-20 items-center justify-center rounded-full bg-[#0f172a] text-2xl font-bold text-white"
            >
              {initials}
            </span>
          )}

          {pending && (
            <span
              aria-hidden="true"
              className="absolute inset-0 flex items-center justify-center rounded-full bg-black/50 text-white"
            >
              <Loader2 size={20} className="animate-spin" />
            </span>
          )}
        </div>

        {/* Controls */}
        <div className="min-w-0 flex-1">
          <p className="text-sm font-semibold text-slate-900">Profile picture</p>
          <p className="mt-0.5 text-xs text-slate-600">
            Choose a photo from your device, or paste a link to one you already host.
            Photos are resized to {fitWithin(1000, 1000, 512).width}px before upload.
          </p>

          <div className="mt-3 flex flex-wrap items-center gap-2">
            <label
              htmlFor={inputId}
              onDragOver={(event) => { event.preventDefault(); setDragOver(true) }}
              onDragLeave={() => setDragOver(false)}
              onDrop={onDrop}
              className={`inline-flex cursor-pointer items-center gap-2 rounded-xl px-3 py-2 text-sm font-semibold transition ${
                disabled
                  ? 'cursor-not-allowed bg-slate-200 text-slate-500'
                  : dragOver
                    ? 'bg-violet-700 text-white'
                    : 'bg-violet-700 text-white hover:bg-violet-800'
              }`}
            >
              <Upload size={15} aria-hidden="true" />
              {pending ? 'Uploading...' : 'Choose photo'}
            </label>

            {shown && (
              <button
                type="button"
                onClick={handleRemove}
                disabled={disabled || pending}
                className="inline-flex items-center gap-2 rounded-xl border border-red-200 px-3 py-2 text-sm font-semibold text-red-700 transition hover:bg-red-50 disabled:opacity-50"
              >
                <Trash2 size={15} aria-hidden="true" />
                Remove
              </button>
            )}
          </div>

          {/* Hidden input. `accept="image/*"` is what makes a phone offer the gallery. */}
          <input
            id={inputId}
            ref={inputRef}
            type="file"
            accept="image/*"
            className="sr-only"
            disabled={disabled || pending}
            onChange={(event) => handleFile(event.target.files?.[0])}
          />
        </div>
      </div>

      {error && (
        <p role="alert" className="mt-3 flex items-start gap-2 text-sm text-red-700">
          <ImageIcon size={15} aria-hidden="true" className="mt-0.5 shrink-0" />
          {error}
        </p>
      )}

      {notice && !error && (
        <p role="status" className="mt-3 text-sm text-emerald-700">{notice}</p>
      )}
    </div>
  )
}