import assert from 'node:assert/strict';
import { after, before, describe, it } from 'node:test';

import { getJson, photoForm, postJson, startTestServer } from './helpers.js';

let server;

before(async () => {
  server = await startTestServer();
});

after(() => server.close());

async function upload(form) {
  const response = await fetch(`${server.url}/upload/photos`, { method: 'POST', body: form });
  return { status: response.status, body: await response.json() };
}

describe('photo upload', () => {
  it('reads a solved cube from clean photos', async () => {
    const solved = (await getJson(`${server.url}/cube/solved`)).body.state;
    const { status, body } = await upload(photoForm(solved));
    assert.equal(status, 200);
    assert.deepEqual(body.state, solved);
    assert.equal(body.valid, true);
  });

  it('reads a scrambled cube from noisy, dim JPEG photos and then solves it', async () => {
    const cube = (await postJson(`${server.url}/cube/scramble`, { moves: 30 })).body;
    const { status, body } = await upload(photoForm(cube.state, { format: 'jpeg', brightness: 0.8, noise: 14 }));

    assert.equal(status, 200);
    assert.deepEqual(body.state, cube.state);
    assert.equal(body.valid, true);

    const solve = await postJson(`${server.url}/cube/solve`, { state: body.state, algorithm: 'TWOPHASE' });
    assert.equal(solve.body.success, true);
  });

  it('works for several different scrambles', async () => {
    for (let round = 0; round < 5; round++) {
      const cube = (await postJson(`${server.url}/cube/scramble`, { moves: 25 })).body;
      const { body } = await upload(photoForm(cube.state, { brightness: 0.9 + round * 0.05, noise: 12 }));
      assert.deepEqual(body.state, cube.state, `round ${round}`);
    }
  });

  it('rejects the request when a photo is missing', async () => {
    const form = new FormData();
    form.append('U', new Blob([Buffer.from('x')]), 'U.png');
    const { status, body } = await upload(form);
    assert.equal(status, 400);
    assert.match(body.error, /Missing/);
  });

  it('rejects files that are not images', async () => {
    const form = new FormData();
    for (const face of ['U', 'R', 'F', 'D', 'L', 'B']) {
      form.append(face, new Blob([Buffer.from('this is not an image')]), `${face}.png`);
    }
    const { status } = await upload(form);
    assert.equal(status, 400);
  });
});

describe('state file upload', () => {
  async function uploadFile(text, name) {
    const form = new FormData();
    form.append('file', new Blob([text]), name);
    const response = await fetch(`${server.url}/upload/state-file`, { method: 'POST', body: form });
    return { status: response.status, body: await response.json() };
  }

  it('reads 54 letters in U R F D L B order', async () => {
    const solved = 'UUUUUUUUU RRRRRRRRR FFFFFFFFF DDDDDDDDD LLLLLLLLL BBBBBBBBB';
    const { status, body } = await uploadFile(solved, 'cube.txt');
    assert.equal(status, 200);
    assert.equal(body.state.R, 'RRRRRRRRR');
    assert.equal(body.valid, true);
  });

  it('reads a JSON file', async () => {
    const solved = (await getJson(`${server.url}/cube/solved`)).body.state;
    const { status, body } = await uploadFile(JSON.stringify(solved), 'cube.json');
    assert.equal(status, 200);
    assert.deepEqual(body.state, solved);
  });

  it('rejects a file with the wrong content', async () => {
    const { status } = await uploadFile('hello', 'cube.txt');
    assert.equal(status, 400);
  });

  it('tells the user when a cube is impossible', async () => {
    // Swap two stickers so a corner is twisted: counts are fine, the cube is not real.
    const text = 'UUUUUUUUF RRRRRRRRU FFFFFFFFR DDDDDDDDD LLLLLLLLL BBBBBBBBB';
    const { status, body } = await uploadFile(text, 'cube.txt');
    assert.equal(status, 200);
    assert.equal(body.valid, false);
    assert.ok(body.message.length > 0);
  });
});
