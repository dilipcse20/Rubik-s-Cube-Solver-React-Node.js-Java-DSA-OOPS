import jpeg from 'jpeg-js';
import { PNG } from 'pngjs';

import { HttpError } from '../utils/HttpError.js';

// Reads the colours of a cube from 6 photos (one photo per face).
//
// How it works, in simple steps:
//   1. Decode each photo into pixels.
//   2. Cut the photo into a 3x3 grid and take the middle colour of every square.
//   3. The CENTRE square of a face never moves, so its colour names that face.
//      (It does not matter which colour scheme your cube has.)
//   4. Give every other square the face whose centre colour looks most similar.
//      Every colour must be used exactly 9 times, so we hand squares out
//      "most certain first" and never give a colour more than 9 squares.
//   5. Squares where two colours looked almost the same are reported as "uncertain",
//      so the user can double-check them.

// The order of the photos, and the order of faces in the net.
export const PHOTO_FACES = ['U', 'R', 'F', 'D', 'L', 'B'];

const UNCERTAIN_RATIO = 1.4; // second-best colour closer than 1.4x the best -> uncertain

// ---------- 1. decode the photo ----------

function decodeImage(buffer) {
  const isPng = buffer[0] === 0x89 && buffer[1] === 0x50;
  const isJpeg = buffer[0] === 0xff && buffer[1] === 0xd8;

  if (isPng) {
    const png = PNG.sync.read(buffer);
    return { width: png.width, height: png.height, data: png.data };
  }
  if (isJpeg) {
    const image = jpeg.decode(buffer, { useTArray: true, maxMemoryUsageInMB: 512 });
    return { width: image.width, height: image.height, data: image.data };
  }
  throw new HttpError(400, 'Photos must be PNG or JPEG files');
}

// ---------- 2. colour of each of the 9 squares ----------

function medianOf(numbers) {
  numbers.sort((a, b) => a - b);
  return numbers[Math.floor(numbers.length / 2)];
}

// Median colour of the middle part of one square (the median ignores glare and edges).
function squareColour(image, left, top, size) {
  const reds = [];
  const greens = [];
  const blues = [];

  const from = Math.floor(size * 0.3);
  const to = Math.floor(size * 0.7);
  for (let y = top + from; y < top + to; y++) {
    for (let x = left + from; x < left + to; x++) {
      const index = (y * image.width + x) * 4;
      reds.push(image.data[index]);
      greens.push(image.data[index + 1]);
      blues.push(image.data[index + 2]);
    }
  }
  return [medianOf(reds), medianOf(greens), medianOf(blues)];
}

// Returns 9 colours (row by row) from the biggest square in the middle of the photo.
function readNineSquares(image) {
  const side = Math.min(image.width, image.height);
  const startX = Math.floor((image.width - side) / 2);
  const startY = Math.floor((image.height - side) / 2);
  const cell = Math.floor(side / 3);

  if (cell < 6) {
    throw new HttpError(400, 'A photo is too small. Use at least 60x60 pixels.');
  }

  const colours = [];
  for (let row = 0; row < 3; row++) {
    for (let col = 0; col < 3; col++) {
      colours.push(squareColour(image, startX + col * cell, startY + row * cell, cell));
    }
  }
  return colours;
}

// ---------- 3. compare colours ----------

// Converts RGB to the "Lab" colour space, where distance matches how different
// two colours LOOK to a person. Plain RGB distance is worse for orange vs red.
function toLab([red, green, blue]) {
  const linear = [red, green, blue].map((value) => {
    const v = value / 255;
    return v <= 0.04045 ? v / 12.92 : ((v + 0.055) / 1.055) ** 2.4;
  });

  const x = (linear[0] * 0.4124 + linear[1] * 0.3576 + linear[2] * 0.1805) / 0.95047;
  const y = linear[0] * 0.2126 + linear[1] * 0.7152 + linear[2] * 0.0722;
  const z = (linear[0] * 0.0193 + linear[1] * 0.1192 + linear[2] * 0.9505) / 1.08883;

  const f = (t) => (t > 0.008856 ? Math.cbrt(t) : 7.787 * t + 16 / 116);
  return [116 * f(y) - 16, 500 * (f(x) - f(y)), 200 * (f(y) - f(z))];
}

function distance(labA, labB) {
  return Math.hypot(labA[0] - labB[0], labA[1] - labB[1], labA[2] - labB[2]);
}

// ---------- the main function ----------

// photos: { U: Buffer, R: Buffer, F: Buffer, D: Buffer, L: Buffer, B: Buffer }
// Returns { state, uncertain, warnings }
//   state:     { U: "UUUUUUUUU", ... } the detected cube
//   uncertain: [{ face: "F", index: 3 }, ...] squares worth double-checking
export function readFacePhotos(photos) {
  const labs = {};
  for (const face of PHOTO_FACES) {
    const squares = readNineSquares(decodeImage(photos[face]));
    labs[face] = squares.map(toLab);
  }

  // The centre colours (square number 4) name the faces.
  const centres = PHOTO_FACES.map((face) => ({ letter: face, lab: labs[face][4] }));
  const warnings = findSimilarCentres(centres);

  // Distance from every non-centre square to every centre colour.
  const candidates = [];
  for (const face of PHOTO_FACES) {
    for (let index = 0; index < 9; index++) {
      if (index === 4) {
        continue;
      }
      const distances = centres.map((centre) => distance(labs[face][index], centre.lab));
      candidates.push({ face, index, distances });
    }
  }

  const assigned = assignWithLimits(candidates, centres);
  return buildResult(assigned, candidates, centres, warnings);
}

function findSimilarCentres(centres) {
  const warnings = [];
  for (let a = 0; a < centres.length; a++) {
    for (let b = a + 1; b < centres.length; b++) {
      if (distance(centres[a].lab, centres[b].lab) < 12) {
        warnings.push(`The centres of faces ${centres[a].letter} and ${centres[b].letter} look almost the same. Check the photo order and lighting.`);
      }
    }
  }
  return warnings;
}

// Gives each square a colour. Most certain squares choose first, and a colour
// can be used at most 8 times (plus its centre = 9).
function assignWithLimits(candidates, centres) {
  const choices = [];
  for (const candidate of candidates) {
    candidate.distances.forEach((value, colour) => {
      choices.push({ candidate, colour, value });
    });
  }
  choices.sort((a, b) => a.value - b.value);

  const used = centres.map(() => 0);
  const result = new Map();
  for (const { candidate, colour } of choices) {
    const key = `${candidate.face}${candidate.index}`;
    if (!result.has(key) && used[colour] < 8) {
      result.set(key, colour);
      used[colour]++;
    }
  }
  return result;
}

function buildResult(assigned, candidates, centres, warnings) {
  const state = {};
  for (const face of PHOTO_FACES) {
    state[face] = '';
    for (let index = 0; index < 9; index++) {
      state[face] += index === 4 ? face : centres[assigned.get(`${face}${index}`)].letter;
    }
  }

  const uncertain = [];
  for (const candidate of candidates) {
    const sorted = [...candidate.distances].sort((a, b) => a - b);
    const bestColour = candidate.distances.indexOf(sorted[0]);
    const chosenColour = assigned.get(`${candidate.face}${candidate.index}`);
    const closeCall = sorted[1] < sorted[0] * UNCERTAIN_RATIO + 4;

    if (closeCall || chosenColour !== bestColour) {
      uncertain.push({ face: candidate.face, index: candidate.index });
    }
  }
  return { state, uncertain, warnings };
}
