import { useCallback, useEffect, useState } from 'react';

import { getSolvedCube, scrambleCube, solveCube, validateCube } from './api/cubeApi.js';
import AlgorithmInfo from './components/AlgorithmInfo.jsx';
import ColorPalette from './components/ColorPalette.jsx';
import CubeControls from './components/CubeControls.jsx';
import CubeNet from './components/CubeNet.jsx';
import Header from './components/Header.jsx';
import SolutionPanel from './components/SolutionPanel.jsx';
import UploadPanel from './components/UploadPanel.jsx';
import { useBackendStatus } from './hooks/useBackendStatus.js';
import { useSolutionPlayback } from './hooks/useSolutionPlayback.js';

const DEFAULT_SETTINGS = {
  scrambleDepth: 4,
  algorithm: 'IDDFS',
  maxDepth: 6,
  maxStates: 5000000,
};

function App() {
  const isOnline = useBackendStatus();

  // The cube currently drawn, and the cube we started from (used by Reset).
  const [cubeState, setCubeState] = useState(null);
  const [startState, setStartState] = useState(null);
  const [scrambleMoves, setScrambleMoves] = useState([]);

  // Extra information about an uploaded or painted cube.
  const [uncertainStickers, setUncertainStickers] = useState([]);
  const [checkInfo, setCheckInfo] = useState(null); // { valid, message, warnings }
  const [paintColour, setPaintColour] = useState(null);

  const [settings, setSettings] = useState(DEFAULT_SETTINGS);
  const [solveResult, setSolveResult] = useState(null);
  const [busyAction, setBusyAction] = useState('');
  const [errorMessage, setErrorMessage] = useState('');

  const isBusy = busyAction !== '';
  const solution = solveResult && solveResult.success ? solveResult.solution : null;
  const solutionStart = solveResult ? solveResult.inputState : null;
  const playback = useSolutionPlayback(solutionStart, solution, setCubeState);

  // Forget everything about the previous cube.
  const clearResults = useCallback(() => {
    setSolveResult(null);
    setErrorMessage('');
    setUncertainStickers([]);
    setCheckInfo(null);
  }, []);

  // Runs a backend request while showing the busy state and catching errors.
  // "actionName" says what is running, for example 'solve'.
  const runRequest = useCallback(async (actionName, request) => {
    setBusyAction(actionName);
    setErrorMessage('');
    try {
      await request();
    } catch (error) {
      setErrorMessage(error.message);
    } finally {
      setBusyAction('');
    }
  }, []);

  const handleNewCube = useCallback(() => {
    return runRequest('newCube', async () => {
      const response = await getSolvedCube();
      setCubeState(response.state);
      setStartState(response.state);
      setScrambleMoves([]);
      clearResults();
    });
  }, [runRequest, clearResults]);

  // Load the first cube as soon as the backend is reachable.
  useEffect(() => {
    if (isOnline && !cubeState) {
      handleNewCube();
    }
  }, [isOnline, cubeState, handleNewCube]);

  function handleSettingChange(name, value) {
    setSettings((current) => ({ ...current, [name]: value }));
  }

  function handleScramble() {
    return runRequest('scramble', async () => {
      const response = await scrambleCube(settings.scrambleDepth);
      setCubeState(response.state);
      setStartState(response.state);
      setScrambleMoves(response.scramble);
      clearResults();

      // Simple algorithms only cope with short scrambles; switch to Two-Phase for long ones.
      if (settings.scrambleDepth > 8 && settings.algorithm !== 'TWOPHASE') {
        setSettings((current) => ({ ...current, algorithm: 'TWOPHASE', maxDepth: 30 }));
      }
    });
  }

  function handleSolve() {
    return runRequest('solve', async () => {
      const response = await solveCube(
        cubeState,
        settings.algorithm,
        settings.maxDepth,
        settings.maxStates
      );
      setSolveResult(response);
    });
  }

  function handleReset() {
    if (!startState) {
      return;
    }
    setCubeState(startState);
    clearResults();
  }

  // The backend read a cube from photos or a file.
  function handleUploaded(result) {
    setCubeState(result.state);
    setStartState(result.state);
    setScrambleMoves([]);
    clearResults();
    setUncertainStickers(result.uncertain);
    setCheckInfo({ valid: result.valid, message: result.message, warnings: result.warnings });
    // An uploaded cube is usually fully mixed, so use the solver that handles any cube.
    setSettings((current) => ({ ...current, algorithm: 'TWOPHASE', maxDepth: 30 }));
  }

  // Paint one sticker with the selected colour (centres never change).
  function handleStickerClick(face, index) {
    if (!paintColour || index === 4 || cubeState[face][index] === paintColour) {
      return;
    }
    const letters = cubeState[face].split('');
    letters[index] = paintColour;
    const newState = { ...cubeState, [face]: letters.join('') };

    setCubeState(newState);
    setStartState(newState);
    setScrambleMoves([]);
    clearResults();
  }

  function handleCheck() {
    return runRequest('check', async () => {
      const response = await validateCube(cubeState);
      setCheckInfo({ valid: response.valid, message: response.message, warnings: [] });
    });
  }

  return (
    <div className="app">
      <Header isOnline={isOnline} />

      {!isOnline && (
        <p className="banner">
          The backend is not reachable. Start it with <code>npm start</code> in the backend
          folder (and compile the Java solver first, see the README).
        </p>
      )}

      <main className="dashboard">
        <CubeControls
          settings={settings}
          onSettingChange={handleSettingChange}
          onNewCube={handleNewCube}
          onScramble={handleScramble}
          onSolve={handleSolve}
          onReset={handleReset}
          isBusy={isBusy}
          isOnline={isOnline}
        />

        <div className="center-column">
          <section className="card cube-card">
            <h2>Cube</h2>
            <CubeNet
              state={cubeState}
              onStickerClick={handleStickerClick}
              uncertainStickers={uncertainStickers}
            />
            <ColorPalette selected={paintColour} onSelect={setPaintColour} />

            <button type="button" onClick={handleCheck} disabled={isBusy || !isOnline || !cubeState}>
              Check This Cube
            </button>
            <CheckMessage info={checkInfo} hasUncertain={uncertainStickers.length > 0} />
          </section>

          <UploadPanel isOnline={isOnline} onLoaded={handleUploaded} onError={setErrorMessage} />
        </div>

        <SolutionPanel
          result={solveResult}
          scrambleMoves={scrambleMoves}
          errorMessage={errorMessage}
          isSolving={busyAction === 'solve'}
          playback={playback}
        />
      </main>

      <AlgorithmInfo algorithm={settings.algorithm} />
    </div>
  );
}

// Shows whether the current cube is valid, plus any photo warnings.
function CheckMessage({ info, hasUncertain }) {
  if (!info) {
    return null;
  }
  const className = info.valid ? 'message message-success' : 'message message-error';

  return (
    <div className="check-message">
      <p className={className}>{info.message}</p>
      {hasUncertain && (
        <p className="message">
          Stickers with a yellow outline were hard to read. Check them against your cube and
          repaint any that are wrong.
        </p>
      )}
      {info.warnings.map((warning) => (
        <p key={warning} className="message message-error">{warning}</p>
      ))}
    </div>
  );
}

export default App;
