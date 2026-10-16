import cors from 'cors';
import express from 'express';

import { errorHandler } from './middleware/errorHandler.js';
import { createCubeRouter } from './routes/cubeRoutes.js';
import { createUploadRouter } from './routes/uploadRoutes.js';

// Builds the Express app. "bridge" is the connection to the Java solver.
// (It is a parameter so the tests can use their own bridge.)
export function createApp(bridge) {
  const app = express();

  // The React dev server runs on port 5173, so the browser must be allowed to call us.
  app.use(cors({ origin: ['http://localhost:5173', 'http://127.0.0.1:5173'] }));
  app.use(express.json({ limit: '100kb' }));

  app.use('/api/cube', createCubeRouter(bridge));
  app.use('/api/upload', createUploadRouter(bridge));

  app.use((request, response) => {
    response.status(404).json({ success: false, error: `No such page: ${request.method} ${request.path}` });
  });
  app.use(errorHandler);

  return app;
}
