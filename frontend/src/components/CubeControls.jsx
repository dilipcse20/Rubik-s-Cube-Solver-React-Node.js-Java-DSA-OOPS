import { ALGORITHMS } from '../data/algorithmInfo.js';

const SCRAMBLE_DEPTHS = [1, 2, 3, 4, 5, 6, 7, 8, 12, 20, 25];

const STATE_LIMITS = [
  { value: 100000, label: '100,000' },
  { value: 500000, label: '500,000' },
  { value: 1000000, label: '1,000,000' },
  { value: 2000000, label: '2,000,000' },
  { value: 5000000, label: '5,000,000' },
  { value: 10000000, label: '10,000,000' },
];

// Left panel: every button and setting. It owns no data; App passes values and handlers.
function CubeControls({
  settings,
  onSettingChange,
  onNewCube,
  onScramble,
  onSolve,
  onReset,
  isBusy,
  isOnline,
}) {
  const isDisabled = isBusy || !isOnline;

  function handleNumberChange(name) {
    return (event) => onSettingChange(name, Number(event.target.value));
  }

  function handleTextChange(name) {
    return (event) => onSettingChange(name, event.target.value);
  }

  return (
    <section className="card">
      <h2>Cube Controls</h2>

      <div className="button-column">
        <button type="button" onClick={onNewCube} disabled={isDisabled}>
          New Solved Cube
        </button>
        <button type="button" onClick={onScramble} disabled={isDisabled}>
          Generate Scramble
        </button>
      </div>

      <label className="field">
        <span>Scramble depth (moves)</span>
        <select
          value={settings.scrambleDepth}
          onChange={handleNumberChange('scrambleDepth')}
        >
          {SCRAMBLE_DEPTHS.map((depth) => (
            <option key={depth} value={depth}>
              {depth}
            </option>
          ))}
        </select>
      </label>

      <label className="field">
        <span>Algorithm</span>
        <select value={settings.algorithm} onChange={handleTextChange('algorithm')}>
          {ALGORITHMS.map((algorithm) => (
            <option key={algorithm.value} value={algorithm.value}>
              {algorithm.label}
            </option>
          ))}
        </select>
      </label>

      <label className="field">
        <span>Maximum search depth</span>
        <input
          type="number"
          min="1"
          max="40"
          value={settings.maxDepth}
          onChange={handleNumberChange('maxDepth')}
        />
      </label>

      <label className="field">
        <span>Maximum states to explore</span>
        <select value={settings.maxStates} onChange={handleNumberChange('maxStates')}>
          {STATE_LIMITS.map((limit) => (
            <option key={limit.value} value={limit.value}>
              {limit.label}
            </option>
          ))}
        </select>
      </label>

      <div className="button-column">
        <button
          type="button"
          className="primary-button"
          onClick={onSolve}
          disabled={isDisabled}
        >
          {isBusy ? 'Working...' : 'Solve Cube'}
        </button>
        <button type="button" className="secondary-button" onClick={onReset} disabled={isDisabled}>
          Reset
        </button>
      </div>
    </section>
  );
}

export default CubeControls;
