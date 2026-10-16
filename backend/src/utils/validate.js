import { HttpError } from './HttpError.js';

// The algorithm names the Java solver understands.
export const ALGORITHMS = ['BFS', 'DFS', 'IDDFS', 'IDASTAR', 'TWOPHASE'];

// The order of faces inside the 54-letter text we send to Java.
const FACE_ORDER = ['U', 'D', 'F', 'B', 'L', 'R'];

const MOVE_PATTERN = /^[UDFBLR][2']?$/;

// Turns {U: "UUUUUUUUU", D: "...", ...} into one 54-letter text.
// Java does the full check (colour counts, real pieces ...). Here we only check the shape.
export function facesToStickers(faces) {
  if (!faces || typeof faces !== 'object' || Array.isArray(faces)) {
    throw new HttpError(400, 'state must be an object with the faces U, D, F, B, L, R');
  }

  let stickers = '';
  for (const face of FACE_ORDER) {
    const letters = faces[face];
    if (typeof letters !== 'string' || letters.length !== 9) {
      throw new HttpError(400, `Face ${face} must be a text with exactly 9 stickers`);
    }
    stickers += letters;
  }
  return stickers;
}

// Reads a list like ["R", "U'"] and makes sure every move is valid.
export function readMoves(moves) {
  if (!Array.isArray(moves)) {
    throw new HttpError(400, 'moves must be a list such as ["R", "U\'"]');
  }
  if (moves.length > 500) {
    throw new HttpError(400, 'At most 500 moves can be applied at once');
  }
  for (const move of moves) {
    if (typeof move !== 'string' || !MOVE_PATTERN.test(move)) {
      throw new HttpError(400, `Invalid move: '${move}'`);
    }
  }
  return moves;
}

// Reads a whole number from the request. Uses defaultValue when it is missing.
export function readInteger(value, name, min, max, defaultValue) {
  if (value === undefined || value === null) {
    return defaultValue;
  }
  if (!Number.isInteger(value) || value < min || value > max) {
    throw new HttpError(400, `${name} must be a whole number between ${min} and ${max}`);
  }
  return value;
}

export function readAlgorithm(value) {
  if (typeof value !== 'string') {
    throw new HttpError(400, `algorithm is required. Use one of ${ALGORITHMS.join(', ')}`);
  }
  const name = value.trim().toUpperCase().replace('*', 'STAR');
  if (!ALGORITHMS.includes(name)) {
    throw new HttpError(400, `Unknown algorithm '${value}'. Use one of ${ALGORITHMS.join(', ')}`);
  }
  return name;
}
