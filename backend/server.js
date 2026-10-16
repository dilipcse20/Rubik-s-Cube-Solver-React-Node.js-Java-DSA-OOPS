import path from 'node:path';
import { fileURLToPath } from 'node:url';

import { createApp } from './src/app.js';
import { JavaBridge } from './src/services/javaBridge.js';

const PORT = process.env.PORT || 8080;

// The compiled Java solver lives in ../solver-java/out (see README to compile it).
const here = path.dirname(fileURLToPath(import.meta.url));
const classPath = path.join(here, '..', 'solver-java', 'out');

const bridge = new JavaBridge({ classPath });
const app = createApp(bridge);

app.listen(PORT, () => {
  console.log(`Node API running on http://localhost:${PORT}`);
  // Start Java now so the first request is fast. If it fails we only warn;
  // the API will try again on the next request.
  bridge.start().then(
    () => console.log('Java solver is ready.'),
    (error) => console.warn(`Java solver is not ready: ${error.message}`)
  );
});

// Stop Java when Node stops.
process.on('SIGINT', () => {

  bridge.stop();
  process.exit(0);

});
