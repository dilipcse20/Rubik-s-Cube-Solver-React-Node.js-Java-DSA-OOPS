import assert from 'node:assert/strict';
import { after, before, describe, it } from 'node:test';

import { getJson, postJson, startTestServer } from './helpers.js';

let server;

before(async () => {
  server = await startTestServer();
});

after(() => server.close());

// Helper: a scrambled cube from the API.
async function scrambledCube(moves) {
  const { body } = await postJson(`${server.url}/cube/scramble`, { moves });
  return body;
}

describe('basic endpoints', () => {
  it('health reports Node and Java', async () => {
    const { status, body } = await getJson(`${server.url}/cube/health`);
    assert.equal(status, 200);
    assert.equal(body.status, 'UP');
    assert.ok(body.javaVersion);
    assert.ok(body.algorithms.includes('TWOPHASE'));
  });

  it('solved returns a solved cube', async () => {
    const { status, body } = await getJson(`${server.url}/cube/solved`);
    assert.equal(status, 200);
    assert.equal(body.solved, true);
    assert.equal(body.state.U, 'UUUUUUUUU');
  });

  it('scramble returns the requested number of moves', async () => {
    const body = await scrambledCube(6);
    assert.equal(body.moveCount, 6);
    assert.equal(body.scramble.length, 6);
  });

  it('apply matches real cube behaviour (R moves F up)', async () => {
    const solved = (await getJson(`${server.url}/cube/solved`)).body;
    const { body } = await postJson(`${server.url}/cube/apply`, { state: solved.state, moves: ['R'] });
    assert.equal(body.state.U, 'UUFUUFUUF');
    assert.equal(body.solved, false);
  });

  it('a move and its inverse give a solved cube', async () => {
    const solved = (await getJson(`${server.url}/cube/solved`)).body;
    const { body } = await postJson(`${server.url}/cube/apply`, { state: solved.state, moves: ["R", "U2", "U2", "R'"] });
    assert.equal(body.solved, true);
  });
});

describe('solving', () => {
  it('every simple algorithm solves a 3-move scramble correctly', async () => {
    const cube = await scrambledCube(3);
    for (const algorithm of ['BFS', 'DFS', 'IDDFS', 'IDASTAR']) {
      const { status, body } = await postJson(`${server.url}/cube/solve`, { state: cube.state, algorithm, maxDepth: 4 });
      assert.equal(status, 200, algorithm);
      assert.equal(body.success, true, algorithm);
      assert.equal(body.finalState.U, 'UUUUUUUUU', algorithm);
      assert.ok(body.moveCount <= 4, algorithm);
    }
  });

  it('IDDFS finds the shortest solution', async () => {
    const solved = (await getJson(`${server.url}/cube/solved`)).body;
    const mixed = (await postJson(`${server.url}/cube/apply`, { state: solved.state, moves: ['R', 'U'] })).body;
    const { body } = await postJson(`${server.url}/cube/solve`, { state: mixed.state, algorithm: 'iddfs', maxDepth: 6 });
    assert.equal(body.moveCount, 2);
    assert.deepEqual(body.solution, ["U'", "R'"]);
  });

  it('Two-Phase solves a fully scrambled cube', async () => {
    const cube = await scrambledCube(40);
    const { status, body } = await postJson(`${server.url}/cube/solve`, { state: cube.state, algorithm: 'TWOPHASE' });
    assert.equal(status, 200);
    assert.equal(body.success, true);
    assert.ok(body.moveCount <= 30);

    // Check with the apply endpoint that the moves really solve the cube.
    const check = await postJson(`${server.url}/cube/apply`, { state: cube.state, moves: body.solution });
    assert.equal(check.body.solved, true);
  });

  it('reports a state limit honestly instead of claiming success', async () => {
    const cube = await scrambledCube(12);
    const { status, body } = await postJson(`${server.url}/cube/solve`, {
      state: cube.state, algorithm: 'IDDFS', maxDepth: 8, maxStates: 1000,
    });
    assert.equal(status, 200);
    assert.equal(body.success, false);
    assert.equal(body.status, 'STATE_LIMIT_REACHED');
    assert.equal(body.moveCount, 0);
    assert.ok(body.error);
  });
});

describe('bad requests get a 400 with a clear message', () => {
  let state;
  before(async () => {
    state = (await getJson(`${server.url}/cube/solved`)).body.state;
  });

  it('unknown algorithm', async () => {
    const { status, body } = await postJson(`${server.url}/cube/solve`, { state, algorithm: 'MAGIC' });
    assert.equal(status, 400);
    assert.equal(body.success, false);
  });

  it('depth out of range', async () => {
    const { status } = await postJson(`${server.url}/cube/solve`, { state, algorithm: 'BFS', maxDepth: 99 });
    assert.equal(status, 400);
  });

  it('invalid move', async () => {
    const { status, body } = await postJson(`${server.url}/cube/apply`, { state, moves: ['R', 'X9'] });
    assert.equal(status, 400);
    assert.equal(body.error, "Invalid move: 'X9'");
  });

  it('missing face', async () => {
    const { status } = await postJson(`${server.url}/cube/solve`, { state: { U: 'UUUUUUUUU' }, algorithm: 'BFS' });
    assert.equal(status, 400);
  });

  it('wrong colour counts', async () => {
    const { status } = await postJson(`${server.url}/cube/solve`, { state: { ...state, U: 'UUUUUUUUD' }, algorithm: 'BFS' });
    assert.equal(status, 400);
  });

  it('a cube with one twisted corner cannot be solved (400)', async () => {
    // Rotate the three stickers of the up-right-front corner (U -> F -> R -> U).
    // Every colour still appears 9 times, and the corner is a real piece, but twisted.
    const twisted = { ...state, U: 'UUUUUUUUF', F: 'FFRFFFFFF', R: 'URRRRRRRR' };
    const { status } = await postJson(`${server.url}/cube/solve`, { state: twisted, algorithm: 'TWOPHASE' });
    assert.equal(status, 400);
  });

  it('malformed JSON', async () => {
    const response = await fetch(`${server.url}/cube/solve`, {
      method: 'POST', headers: { 'Content-Type': 'application/json' }, body: '{ not json',
    });
    assert.equal(response.status, 400);
  });

  it('unknown URL gives 404', async () => {
    const { status } = await getJson(`${server.url}/cube/nothing`);
    assert.equal(status, 404);
  });
});
