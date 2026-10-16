import { Router } from 'express';

import { askJava } from '../services/javaBridge.js';
import {
  facesToStickers,
  readAlgorithm,
  readInteger,
  readMoves,
} from '../utils/validate.js';

// The cube API. Every route checks the input, then asks the Java solver.
// All cube logic and all search algorithms live in Java.
export function createCubeRouter(bridge) {
  const router = Router();

  // GET /api/cube/health  -> is Node (and Java behind it) working?
  router.get('/health', async (request, response) => {
    const java = await askJava(bridge, 'PING');
    response.json({
      status: 'UP',
      service: 'rubiks-cube-solver',
      backend: `node ${process.version}`,
      javaVersion: java.javaVersion,
      algorithms: java.algorithms,
      time: new Date().toISOString(),
    });
  });

  // GET /api/cube/solved  -> a solved cube
  router.get('/solved', async (request, response) => {
    response.json(await askJava(bridge, 'SOLVED'));
  });

  // POST /api/cube/scramble   body: { "moves": 5 }
  router.post('/scramble', async (request, response) => {
    const moves = readInteger(request.body?.moves, 'moves', 1, 40, 5);
    response.json(await askJava(bridge, `SCRAMBLE ${moves}`));
  });

  // POST /api/cube/apply   body: { "state": {...}, "moves": ["R", "U'"] }
  router.post('/apply', async (request, response) => {
    const stickers = facesToStickers(request.body?.state);
    const moves = readMoves(request.body?.moves);
    const moveText = moves.length > 0 ? moves.join(',') : '-';
    response.json(await askJava(bridge, `APPLY ${stickers} ${moveText}`));
  });

  // POST /api/cube/validate   body: { "state": {...} }
  router.post('/validate', async (request, response) => {
    const stickers = facesToStickers(request.body?.state);
    response.json(await askJava(bridge, `VALIDATE ${stickers}`));
  });

  // POST /api/cube/solve   body: { "state": {...}, "algorithm": "TWOPHASE", "maxDepth": 30, "maxStates": 5000000 }
  router.post('/solve', async (request, response) => {
    const stickers = facesToStickers(request.body?.state);
    const algorithm = readAlgorithm(request.body?.algorithm);
    const maxDepth = readInteger(request.body?.maxDepth, 'maxDepth', 1, 40, algorithm === 'TWOPHASE' ? 30 : 7);
    const maxStates = readInteger(request.body?.maxStates, 'maxStates', 1, 20000000, 5000000);

    response.json(await askJava(bridge, `SOLVE ${algorithm} ${maxDepth} ${maxStates} ${stickers}`));
  });

  return router;
}
