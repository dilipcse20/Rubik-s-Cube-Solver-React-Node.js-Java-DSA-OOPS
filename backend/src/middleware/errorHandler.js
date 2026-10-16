// Turns every error into a clean JSON answer: { success: false, error: "..." }.
// Routes just "throw" errors; they do not need their own try/catch.
export function errorHandler(error, request, response, next) {
  // multer's "file too large" and "too many files" errors
  if (error.code === 'LIMIT_FILE_SIZE') {
    return response.status(400).json({ success: false, error: 'A photo is too big (limit 8 MB each)' });
  }
  if (error.code && error.code.startsWith('LIMIT_')) {
    return response.status(400).json({ success: false, error: `Upload problem: ${error.message}` });
  }

  // Bad JSON in the request body
  if (error.type === 'entity.parse.failed') {
    return response.status(400).json({ success: false, error: 'Request body is not valid JSON' });
  }

  if (error.status) {
    return response.status(error.status).json({ success: false, error: error.message });
  }

  console.error(error);
  return response.status(500).json({ success: false, error: 'Unexpected server error' });
}
