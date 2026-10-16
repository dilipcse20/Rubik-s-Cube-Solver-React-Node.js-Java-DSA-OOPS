import path from 'node:path';
import { fileURLToPath } from 'node:url';

import jpeg from 'jpeg-js';
import { PNG } from 'pngjs';

import { createApp } from '../src/app.js';
import { JavaBridge } from '../src/services/javaBridge.js';

// Colours of a typical cube (red, orange, ... are chosen by the face that owns them).
const COLOURS = {
  U: [240, 240, 240], // white
  R: [200, 30, 30], // red
  F: [30, 160, 60], // green
  D: [240, 210, 40], // yellow
  L: [240, 130, 20], // orange
  B: [30, 80, 200], // blue
};

// Starts the real API (with the real Java solver) on a free port.
export async function startTestServer() {
  const here = path.dirname(fileURLToPath(import.meta.url));
  const bridge = new JavaBridge({ classPath: path.join(here, '..', '..', 'solver-java', 'out') });
  await bridge.start();

  const app = createApp(bridge);
  const server = await new Promise((resolve) => {
    const listening = app.listen(0, () => resolve(listening));
  });

  return {
    url: `http://localhost:${server.address().port}/api`,
    close: () => {
      server.close();
      bridge.stop();
    },
  };
}

export async function getJson(url) {
  const response = await fetch(url);
  return { status: response.status, body: await response.json() };
}

export async function postJson(url, body) {
  const response = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });
  return { status: response.status, body: await response.json() };
}

// Draws a fake photo of one cube face: 3x3 coloured squares with black gaps,
// some random noise, and a brightness change (like different lighting).
export function renderFacePhoto(letters, { brightness = 1, noise = 10, format = 'png', size = 240, seed = 1 } = {}) {
  let randomState = seed;
  const random = () => {
    randomState = (randomState * 1664525 + 1013904223) % 4294967296;
    return randomState / 4294967296;
  };

  const image = { width: size, height: size, data: Buffer.alloc(size * size * 4) };
  const cell = size / 3;
  const gap = 8;

  for (let y = 0; y < size; y++) {
    for (let x = 0; x < size; x++) {
      const row = Math.min(2, Math.floor(y / cell));
      const col = Math.min(2, Math.floor(x / cell));
      const inGap = y % cell < gap || x % cell < gap;
      const colour = COLOURS[letters[row * 3 + col]];

      const index = (y * size + x) * 4;
      for (let channel = 0; channel < 3; channel++) {
        const wobble = (random() - 0.5) * 2 * noise;
        const value = inGap ? 10 : colour[channel] * brightness + wobble;
        image.data[index + channel] = Math.max(0, Math.min(255, Math.round(value)));
      }
      image.data[index + 3] = 255;
    }
  }

  if (format === 'jpeg') {
    return Buffer.from(jpeg.encode(image, 92).data);
  }
  return PNG.sync.write(image);
}

// Builds the multipart form for the 6 photos of a cube state.
export function photoForm(state, options = {}) {
  const form = new FormData();
  ['U', 'R', 'F', 'D', 'L', 'B'].forEach((face, number) => {
    const photo = renderFacePhoto(state[face], { ...options, seed: number + 1 });
    const type = options.format === 'jpeg' ? 'image/jpeg' : 'image/png';
    form.append(face, new Blob([photo], { type }), `${face}.${options.format === 'jpeg' ? 'jpg' : 'png'}`);
  });
  return form;
}
