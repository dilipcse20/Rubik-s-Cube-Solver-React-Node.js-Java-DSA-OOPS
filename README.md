# Rubik's Cube Solver | React + Node.js + Java | DSA + OOPS

A 3x3x3 Rubik's Cube solver with three parts that work together:

```
 React (browser)  ──►  Node.js + Express (API)  ──►  Java (cube + search algorithms)
 draws the cube        checks requests, handles        BFS, DFS, IDDFS, IDA*,
 and buttons           uploads, reads photos           Two-Phase solver
 port 5173             port 8080                       runs as a helper program
```

* **Java** does all the thinking: the cube model and every solving algorithm (the DSA part).
* **Node.js** is the middle man: it receives requests, checks them, reads uploaded photos
  and asks Java to solve.
* **React** only shows things. It never rotates or solves the cube itself.

## What it can do

* Make a solved cube, or a random scramble from 1 to 25 moves.
* Solve with **BFS, DFS, IDDFS, IDA\*** (the learning algorithms, good for short scrambles)
  or **Two-Phase** (solves *any* legal cube, usually in 20-26 moves, in milliseconds).
* **Upload your own cube**: six photos (colours are detected for you), or a text/JSON file.
* **Paint stickers** by hand to fix a colour or type in your own cube.
* **Check** whether a cube is real (a cube with one twisted corner cannot exist, and is rejected).
* Step through the solution move by move, or play it automatically.

## What you need

| Tool    | Version     | Check with      | Used for                  |
|---------|-------------|-----------------|---------------------------|
| JDK     | 17 or newer | `javac -version`| compiling the Java solver |
| Node.js | 18 or newer | `node -v`       | backend and frontend      |

You need the **JDK** (it contains `javac`), not only the Java runtime. No Maven, no Docker,
no database, no paid services.

## Run it (Windows PowerShell)

Open three PowerShell windows, one after the other.

**1. Compile the Java solver (once, and again after editing Java files)**
```powershell
cd solver-java
.\build.ps1
```
If PowerShell blocks the script, run `powershell -ExecutionPolicy Bypass -File .\build.ps1`.

**2. Start the Node backend (http://localhost:8080)**
```powershell
cd backend
npm install
npm start
```
You should see `Node API running ...` and then `Java solver is ready.`

**3. Start the React frontend (http://localhost:5173)**
```powershell
cd frontend
npm install
npm run dev
```
Open http://localhost:5173. A green dot in the header means everything is connected.

## How to use it

1. Press **Generate Scramble**, choose **Two-Phase**, press **Solve Cube**, then **Play Solution**.
2. For short scrambles (1-5 moves) try BFS, DFS, IDDFS and IDA\* and compare *Moves*, *Time*
   and *States explored* in the Solution panel. DFS often gives a longer answer. That is the lesson.
3. **Upload a cube** (middle column):
   * *Six photos*: one per face. Take each photo straight on, good light, face in the middle.
     Hold the cube like this for each photo (this matches the flat picture on screen):
     U: from above, Back edge at the top of the photo. D: from below, Front edge at the top.
     F, R, B, L: from the side, Up edge at the top.
   * Press **Detect Colours From Photos**. Stickers with a **yellow outline** were hard to read:
     compare them with your real cube.
   * Fix mistakes by picking a colour in the palette and clicking stickers. Centres cannot be changed.
   * Press **Check This Cube**. If it says the cube is valid, press **Solve Cube**.
   * *File*: a `.txt` file with 54 letters in the order U R F D L B (spaces allowed), or a `.json` file.
4. Use **Reset** to return to the cube you started from.

## Tests

```powershell
# Java tests (no libraries needed)
cd solver-java
java -cp out com.example.rubik.SelfTest

# Node API + photo tests (they start the real Java solver, so compile it first)
cd backend
npm test
```
The Java tests check cube rotations, solver correctness, shortest-solution behaviour, state
limits, the IDA\* heuristic, the Two-Phase solver on fully scrambled cubes and impossible-cube detection.
The Node tests call the real endpoints and read generated photos (noisy, dim, JPEG and PNG).

## API (Node, port 8080)

| Method | URL                      | Body                                                     | What it does |
|--------|--------------------------|----------------------------------------------------------|--------------|
| GET    | /api/cube/health         |                                                          | Node and Java status |
| GET    | /api/cube/solved         |                                                          | A solved cube |
| POST   | /api/cube/scramble       | `{"moves": 5}` (1-40)                                    | Random scramble and its cube |
| POST   | /api/cube/apply          | `{"state": {...}, "moves": ["R","U'"]}`                  | Apply moves |
| POST   | /api/cube/validate       | `{"state": {...}}`                                       | Is this a real, solvable cube? |
| POST   | /api/cube/solve          | `{"state": {...}, "algorithm": "TWOPHASE", "maxDepth": 30, "maxStates": 5000000}` | Solve |
| POST   | /api/upload/photos       | form-data: six images named `U R F D L B`                | Detect cube from photos |
| POST   | /api/upload/state-file   | form-data: one file named `file`                         | Read a cube from a file |

A cube `state` is JSON with 9 letters per face (row by row, top-left first):
`{"U":"UUUUUUUUU","D":"DDDDDDDDD","F":"FFFFFFFFF","B":"BBBBBBBBB","L":"LLLLLLLLL","R":"RRRRRRRRR"}`.
Each letter is the face the sticker belongs to on a solved cube (so `U` = the colour of the Up face).

Algorithm names: `BFS`, `DFS`, `IDDFS`, `IDASTAR`, `TWOPHASE`.
`maxDepth` 1-40, `maxStates` 1-20,000,000.

Status codes: **200** for a valid request (even if the search fails: check `success`, `status` and
`error`), **400** for bad input (bad move, bad cube, impossible cube, unknown algorithm, wrong file),
**503/504** if the Java solver cannot start or takes too long, **500** for a bug.

Try it in PowerShell:
```powershell
Invoke-RestMethod http://localhost:8080/api/cube/health

$cube = Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/cube/scramble `
  -ContentType 'application/json' -Body '{"moves": 25}'
$body = @{ state = $cube.state; algorithm = 'TWOPHASE' } | ConvertTo-Json -Depth 5
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/cube/solve `
  -ContentType 'application/json' -Body $body
```

## What each algorithm can really do

Measured on one test machine (yours will differ). A "move" is one face turn: R, R' and R2 each count as 1.

| Algorithm | Shortest solution? | Memory    | Reliable up to  | Notes |
|-----------|--------------------|-----------|-----------------|-------|
| BFS       | Yes                | Very high | about 4 moves   | Stops at 500,000 stored states. |
| DFS       | **No**             | High      | about 5 moves   | Returns the first solution it finds. |
| IDDFS     | Yes                | Tiny      | about 6 moves   | 6 moves is about 4 million states. |
| IDA\*     | Yes                | Tiny      | about 8 moves   | 8 moves is about 10 million states. |
| Two-Phase | **No** (short, not shortest) | A few MB | **any legal cube** | About 23 moves on average, 25 at worst in 70 random tests, tens of milliseconds each after a one-time table build (about half a second). |

The cube has about 43 quintillion states, so the first four algorithms cannot solve a fully mixed
cube. The app says so (`STATE_LIMIT_REACHED` or `NOT_FOUND_WITHIN_DEPTH`) instead of pretending.
Two-Phase gets around this by splitting the problem in two (explained in `TwoPhaseSolver.java`).

**Not included:** a bitboard cube. The sticker-array `Cube` is finished and tested first. Because `Cube`
is immutable and only exposes `applied(move)`, `isSolved()` and `key()`, a bitboard version can be added later.

**Photo reading limits:** it needs a straight, evenly lit photo of each face and cuts the photo to its
middle square. Strong glare, shadows or very dim light can confuse colours, which is why uncertain stickers
are outlined and why you can repaint. It does not rotate photos for you (follow the holding guide above).
It was tested with generated photos, not with photos of a real cube.

## Every file in simple terms

### solver-java/ (the DSA part, plain Java, no libraries)

| File | What it is |
|------|------------|
| `build.ps1`, `build.sh` | One-line scripts that compile all Java files into the `out` folder. |
| `model/Face.java` | The 6 faces (U, D, F, B, L, R). Each face knows which way it points in 3D. That is what makes our rotations correct. |
| `model/Move.java` | One move such as `R`, `U'`, `F2`. Can read text (`parse`), undo itself (`inverse`) and list all 18 moves. |
| `model/CubeGeometry.java` | Builds, once, a table "when face X turns, sticker i goes to slot j". It rotates every sticker's 3D position by 90 degrees, so no table is typed by hand. |
| `model/Cube.java` | The cube: 54 stickers in an array. **Never changes**: `applied(move)` returns a new cube. This makes searching easy because going back is free. Also makes scrambles. |
| `model/CubeState.java` | A checked snapshot of a cube used for input and output. Rejects wrong sizes, wrong colour counts, wrong centres and impossible pieces. |
| `solver/Solver.java` | The common "contract" (interface) for every algorithm: `solve(cube, maxDepth, maxStates)`. |
| `solver/SolverResult.java` | What a solver returns: status (solved / not found / limit reached), moves, states explored. |
| `solver/BFSolver.java` | Breadth-First Search. A queue plus a "visited" set. Finishes every depth before the next, so the first answer is shortest. |
| `solver/DFSolver.java` | Depth-limited Depth-First Search. Dives down one branch, backs up, tries the next. Returns the first answer, which may be long. |
| `solver/IDDFSSolver.java` | Iterative Deepening DFS. Runs DFS with limit 1, then 2, then 3... Shortest answer with tiny memory. |
| `solver/IDAStarSolver.java` | IDA\*. Like IDDFS but skips branches where `f = g + h` is too big. The heuristic `h` counts wrong corners and edges (one move fixes at most 4 of each). |
| `solver/CubieCube.java` | A second view of the cube as 8 corner pieces and 12 edge pieces (position + twist). Also checks if a cube is *possible* (twist, flip and swap rules). |
| `solver/TwoPhaseSolver.java` | The powerful solver. Phase 1 reaches a half-solved group, phase 2 finishes inside it. Uses lookup tables and IDA\* for each phase. |
| `Json.java` | A tiny helper that writes JSON text, so we need no JSON library. |
| `CommandHandler.java` | Reads one command (`SOLVE`, `SCRAMBLE`, `APPLY`, `VALIDATE`...) and builds the JSON answer. It checks the solver's moves really solve the cube before saying "success". |
| `SolverServer.java` | The program Node starts. It stays running and answers one line per request (so the Two-Phase tables are built only once). |
| `SelfTest.java` | Simple tests with no libraries. Prints PASS/FAIL. |

### backend/ (Node.js + Express)

| File | What it is |
|------|------------|
| `package.json` | Lists the libraries (express, cors, multer for uploads, pngjs and jpeg-js to read photos) and the `npm start` / `npm test` commands. |
| `server.js` | Starts the web server on port 8080 and starts the Java solver. |
| `src/app.js` | Builds the Express app: CORS (lets the React page on port 5173 call us), JSON reading, routes, error handler. |
| `src/routes/cubeRoutes.js` | The cube endpoints. Each one checks the input and asks Java. |
| `src/routes/uploadRoutes.js` | The upload endpoints for photos and for text/JSON files. Asks Java if the uploaded cube is valid. |
| `src/services/javaBridge.js` | Starts the Java program and talks to it: send one line, wait for one line of JSON. Restarts Java if it stops, and has a timeout. |
| `src/services/photoReader.js` | Detects cube colours from six photos (the steps are explained at the top of the file). |
| `src/utils/validate.js` | Small input checkers (moves, numbers, algorithm names, the cube shape). |
| `src/utils/HttpError.js` | An error that carries an HTTP status such as 400. |
| `src/middleware/errorHandler.js` | Turns every error into a clean JSON answer. |
| `test/helpers.js`, `test/api.test.js`, `test/upload.test.js` | Tests that start the real server and the real Java solver. `helpers.js` draws fake cube photos. |

### frontend/ (React + Vite)

| File | What it is |
|------|------------|
| `index.html`, `vite.config.js`, `package.json` | Standard Vite setup. The dev server uses port 5173. |
| `src/main.jsx` | Starts React and loads the CSS. |
| `src/App.jsx` | The "brain" of the page: holds the cube, settings and results in state, and passes them to the components. |
| `src/api/cubeApi.js` | The only file that talks to the backend. Turns failures into readable messages. |
| `src/hooks/useBackendStatus.js` | Checks `/health` every 5 seconds for the green/red dot. |
| `src/hooks/useSolutionPlayback.js` | Plays the solution step by step. Each step asks the backend for the next cube, then remembers it. |
| `src/data/algorithmInfo.js` | The text for the algorithm list and the explanation section. |
| `src/components/Header.jsx` | Title and connection light. |
| `src/components/CubeControls.jsx` | Left panel: buttons, scramble depth, algorithm, limits. |
| `src/components/CubeNet.jsx`, `CubeFace.jsx` | Draw the six faces as a flat cross. Stickers are clickable (for painting) and can be outlined (uncertain). |
| `src/components/ColorPalette.jsx` | The colour picker for painting stickers. |
| `src/components/UploadPanel.jsx` | Six photo pickers and the file picker. |
| `src/components/SolutionPanel.jsx` | Right panel: result, statistics, move list, Next/Previous/Play. |
| `src/components/AlgorithmInfo.jsx` | The explanation section for the chosen algorithm. |
| `src/styles/global.css`, `App.css` | Colours and layout (dark theme, works on phones). |

## Words you will meet

| Word | Simple meaning |
|------|----------------|
| Cube state | One exact arrangement of all 54 stickers. |
| State transition | Going from one state to the next by making a move. |
| Search tree | Start = scrambled cube. Every state has children, one for each move. |
| Branching factor | How many children a state has: 18 moves (about 15 after skipping same-face repeats). |
| Visited set | A memory of states already seen, so none is searched twice. |
| Depth limit | The longest move list a search is allowed to try. |
| Heuristic h(n) | A guess of how many moves are still needed. |
| Admissible heuristic | A guess that is never too big, so the answer stays shortest. |
| g(n), f(n) | g = moves made so far. f = g + h, the total estimated cost. |
| Pruning | Skipping a branch that cannot give a good answer. |
| Optimal solution | A solution with the fewest possible moves. |
| Time / space complexity | How running time / memory grow. Here about b^d (branching factor to the power of depth). |
| Coordinate | One small number that describes part of the cube (for example all corner twists). |
| Move table / prune table | Pre-computed answers: "coordinate after a move" and "moves needed to fix this coordinate". |
| REST API | Using URLs and GET/POST to ask a server for things. |
| JSON | Plain-text data like `{"moves": 5}`. |
| Component / props / state | A React building block / the inputs it receives / data that redraws it when changed. |

## Common errors

| Problem | Fix |
|---------|-----|
| Header says "Backend offline" | Start the backend (`npm start` in `backend`) and wait for "Java solver is ready". |
| `Cannot start Java` or `The Java solver stopped before it was ready` | Install a JDK 17+, compile the solver (step 1) and run `java -version` and `javac -version` to check. |
| `javac` is not recognized | You installed only a JRE. Install a JDK and open a new PowerShell window. |
| Port 8080 or 5173 in use | Close the other program. To use another backend port: `$env:PORT=8081; npm start` and put `VITE_API_URL=http://localhost:8081/api` in `frontend/.env`. Port 5173 is fixed because the backend allows that origin (CORS). |
| "Missing: ..." on photo upload | Choose a photo for all six faces. |
| Detected colours are wrong | Check the holding guide, use brighter even light, then repaint the outlined stickers. |
| "This cube cannot be solved. One corner is twisted..." | The colours you entered cannot exist on a real cube. Re-check the corners or edges the message mentions. |
| "Search stopped ... states" | Use a smaller scramble, raise the state limit, or choose Two-Phase. |
