/**
 * Turns a picked photo into an avatar-sized JPEG before it is uploaded.
 *
 * This exists because phone cameras produce 3-12 MB images. Sending one straight to the server
 * would blow past any sane upload limit, and storing one as an avatar would make every page
 * that shows it load megabytes for a 64-pixel circle. Downscaling in the browser fixes both,
 * and it means a user on a slow connection is not waiting on their whole photo library.
 */

/** Longest edge of the stored image. Large enough to stay crisp on a retina avatar. */
const MAX_EDGE = 512

/** JPEG quality. 0.85 is visually lossless for a photograph at this size. */
const QUALITY = 0.85

/**
 * @param {File} file
 * @returns {Promise<{ blob: Blob, width: number, height: number }>}
 */
export async function prepareAvatarImage(file) {
  if (!file.type.startsWith('image/')) {
    throw new Error('Please choose an image file.')
  }

  const bitmap = await loadBitmap(file)

  const { width, height } = fitWithin(bitmap.width, bitmap.height, MAX_EDGE)

  const canvas = document.createElement('canvas')
  canvas.width = width
  canvas.height = height

  const context = canvas.getContext('2d')
  if (!context) {
    throw new Error('Your browser could not process this image.')
  }

  // Transparent PNGs would otherwise turn black once flattened onto a JPEG.
  context.fillStyle = '#ffffff'
  context.fillRect(0, 0, width, height)
  context.drawImage(bitmap, 0, 0, width, height)

  close(bitmap)

  const blob = await toBlob(canvas)
  if (!blob) {
    throw new Error('Your browser could not process this image.')
  }

  return { blob, width, height }
}

/** Scales (w, h) down so neither edge exceeds max, preserving the aspect ratio. */
export function fitWithin(width, height, max) {
  if (!Number.isFinite(width) || !Number.isFinite(height) || width <= 0 || height <= 0) {
    throw new Error('That image has no readable dimensions.')
  }

  // Already small enough: keep the original size rather than re-encoding a thumbnail.
  if (width <= max && height <= max) {
    return { width: Math.round(width), height: Math.round(height) }
  }

  const scale = max / Math.max(width, height)
  return {
    width: Math.max(1, Math.round(width * scale)),
    height: Math.max(1, Math.round(height * scale)),
  }
}

/**
 * Decodes the file into something drawable.
 *
 * createImageBitmap is the fast path. Safari <15 and older browsers lack it, so those fall back
 * to an <img> element; the try/catch also covers genuinely corrupt files, which would otherwise
 * leave the promise unsettled.
 */
async function loadBitmap(file) {
  if (typeof createImageBitmap === 'function') {
    try {
      return await createImageBitmap(file)
    } catch {
      // Fall through to the <img> path.
    }
  }

  return await loadViaImageElement(file)
}

function loadViaImageElement(file) {
  return new Promise((resolve, reject) => {
    const objectUrl = URL.createObjectURL(file)
    const image = new Image()

    image.onload = () => {
      URL.revokeObjectURL(objectUrl)
      resolve(image)
    }
    image.onerror = () => {
      URL.revokeObjectURL(objectUrl)
      reject(new Error('That file could not be read as an image.'))
    }

    image.src = objectUrl
  })
}

function toBlob(canvas) {
  return new Promise((resolve) => {
    if (canvas.toBlob) {
      canvas.toBlob(resolve, 'image/jpeg', QUALITY)
    } else {
      resolve(null)
    }
  })
}

/** Frees the decoded image when the API supports it. */
function close(bitmap) {
  if (typeof bitmap.close === 'function') {
    bitmap.close()
  }
}

/** Human-readable file size, for the summary line under the preview. */
export function describeSize(bytes) {
  if (!Number.isFinite(bytes)) return ''
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${Math.round(bytes / 1024)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}