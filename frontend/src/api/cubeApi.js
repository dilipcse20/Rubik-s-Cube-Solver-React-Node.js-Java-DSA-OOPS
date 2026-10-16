// All communication with the Node.js backend lives in this file.
// (Node asks the Java solver to do the real work.)
// The frontend never rotates or solves the cube itself.

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';

async function request(path, options) {
  let response;

  try {
    response = await fetch(`${API_BASE_URL}${path}`, options);
  } catch (error) {
    throw new Error('Cannot reach the backend. Is it running on http://localhost:8080 ?');
  }

  let data = null;
  try {
    data = await response.json();
  } catch (error) {
    data = null;
  }

  if (!response.ok) {
    const message = data && data.error ? data.error : `Request failed (${response.status})`;
    throw new Error(message);
  }

  return data;
}

function postJson(path, body) {
  return request(path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });
}

export function getHealth() {
  return request('/cube/health');
}

export function getSolvedCube() {
  return request('/cube/solved');
}

export function scrambleCube(moves) {
  return postJson('/cube/scramble', { moves });
}

export function applyMoves(state, moves) {
  return postJson('/cube/apply', { state, moves });
}

export function validateCube(state) {
  return postJson('/cube/validate', { state });
}

export function solveCube(state, algorithm, maxDepth, maxStates) {
  return postJson('/cube/solve', { state, algorithm, maxDepth, maxStates });
}

// photos looks like { U: File, R: File, F: File, D: File, L: File, B: File }
export function uploadPhotos(photos) {
  const form = new FormData();
  Object.entries(photos).forEach(([face, file]) => form.append(face, file));
  return request('/upload/photos', { method: 'POST', body: form });
}

export function uploadStateFile(file) {
  const form = new FormData();
  form.append('file', file);
  return request('/upload/state-file', { method: 'POST', body: form });
}
