import { Router } from 'express';
import multer from 'multer';

import { askJava } from '../services/javaBridge.js';
import { PHOTO_FACES, readFacePhotos } from '../services/photoReader.js';
import { facesToStickers } from '../utils/validate.js';
import { HttpError } from '../utils/HttpError.js';

// Photos stay in memory (we never save them to disk).
const upload = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: 8 * 1024 * 1024, files: 6 },
});

const photoFields = PHOTO_FACES.map((face) => ({ name: face, maxCount: 1 }));

// Face order of a standard 54-letter cube text (used by most online cube tools).
const STANDARD_ORDER = ['U', 'R', 'F', 'D', 'L', 'B'];

// Asks Java whether the detected cube is real, so the user knows before pressing Solve.
async function checkWithJava(bridge, state) {
  const reply = await askJava(bridge, `VALIDATE ${facesToStickers(state)}`);
  return { valid: reply.valid, message: reply.message };
}

export function createUploadRouter(bridge) {
  const router = Router();

  // POST /api/upload/photos    form-data with 6 image files named U, R, F, D, L, B
  router.post('/photos', upload.fields(photoFields), async (request, response) => {
    const files = request.files || {};

    const missing = PHOTO_FACES.filter((face) => !files[face]);
    if (missing.length > 0) {
      throw new HttpError(400, `Please upload a photo for every face. Missing: ${missing.join(', ')}`);
    }

    const photos = {};
    for (const face of PHOTO_FACES) {
      photos[face] = files[face][0].buffer;
    }

    const detected = readFacePhotos(photos);
    const check = await checkWithJava(bridge, detected.state);

    response.json({
      success: true,
      state: detected.state,
      uncertain: detected.uncertain,
      warnings: detected.warnings,
      valid: check.valid,
      message: check.message,
    });
  });

  // POST /api/upload/state-file    form-data with one text/JSON file named "file"
  router.post('/state-file', upload.single('file'), async (request, response) => {
    if (!request.file) {
      throw new HttpError(400, 'Please choose a file');
    }
    if (request.file.size > 20000) {
      throw new HttpError(400, 'The file is too big for a cube description');
    }

    const state = parseStateFile(request.file.buffer.toString('utf8'));
    const check = await checkWithJava(bridge, state);

    response.json({
      success: true,
      state,
      uncertain: [],
      warnings: [],
      valid: check.valid,
      message: check.message,
    });
  });

  return router;
}

// Accepts either:
//   * JSON:  {"U":"UUUUUUUUU","D":"...", ...}   (or {"state": {...}})
//   * Text:  54 letters in the order U R F D L B, spaces and new lines allowed
function parseStateFile(text) {
  const trimmed = text.trim();

  if (trimmed.startsWith('{')) {
    let data;
    try {
      data = JSON.parse(trimmed);
    } catch (error) {
      throw new HttpError(400, 'The file looks like JSON but cannot be read');
    }
    const faces = data.state || data;
    facesToStickers(faces); // throws a clear 400 error if the shape is wrong
    return faces;
  }

  const letters = trimmed.replace(/\s+/g, '').toUpperCase();
  if (!/^[UDFBLR]{54}$/.test(letters)) {
    throw new HttpError(400, 'The file must contain exactly 54 letters from U, D, F, B, L, R (or JSON)');
  }

  const faces = {};
  STANDARD_ORDER.forEach((face, number) => {
    faces[face] = letters.slice(number * 9, number * 9 + 9);
  });
  return faces;
}
