// Right panel: the result of the last solve, plus playback controls.
function SolutionPanel({ result, scrambleMoves, errorMessage, isSolving, playback }) {
  return (
    <section className="card">
      <h2>Solution</h2>

      {scrambleMoves.length > 0 && (
        <p className="scramble-line">
          <strong>Scramble:</strong> {scrambleMoves.join(' ')}
        </p>
      )}

      {errorMessage && <p className="message message-error">{errorMessage}</p>}
      {isSolving && <p className="message">Searching... this can take a few seconds.</p>}
      {!result && !isSolving && !errorMessage && (
        <p className="placeholder-text">Generate a scramble, then press Solve Cube.</p>
      )}

      {result && <ResultDetails result={result} playback={playback} />}
    </section>
  );
}

function ResultDetails({ result, playback }) {
  const messageClass = result.success ? 'message message-success' : 'message message-error';

  return (
    <>
      <p className={messageClass}>{result.message}</p>

      <dl className="stats">
        <Stat label="Algorithm" value={result.algorithm} />
        <Stat label="Moves" value={result.moveCount} />
        <Stat label="Time" value={`${result.executionTimeMs} ms`} />
        <Stat label="States explored" value={result.statesExplored.toLocaleString()} />
        <Stat label="Depth limit" value={result.maxDepth} />
        <Stat label="State limit" value={result.maxStates.toLocaleString()} />
      </dl>

      {result.success && result.solution.length > 0 && (
        <>
          <MoveList moves={result.solution} currentStep={playback.stepIndex} />
          <PlaybackControls playback={playback} />
        </>
      )}
    </>
  );
}

function Stat({ label, value }) {
  return (
    <div className="stat">
      <dt>{label}</dt>
      <dd>{value}</dd>
    </div>
  );
}

// Moves already played are dimmed; the move that comes next is highlighted.
function MoveList({ moves, currentStep }) {
  return (
    <ol className="move-list">
      {moves.map((move, index) => {
        let className = 'move-chip';
        if (index < currentStep) {
          className += ' move-done';
        } else if (index === currentStep) {
          className += ' move-next';
        }
        return (
          <li key={index} className={className}>
            {move}
          </li>
        );
      })}
    </ol>
  );
}

function PlaybackControls({ playback }) {
  const { stepIndex, totalSteps, isPlaying, isLoadingStep } = playback;

  return (
    <div className="playback">
      <p className="step-counter">
        Step {stepIndex} of {totalSteps}
      </p>

      <div className="button-row">
        <button
          type="button"
          onClick={playback.goPrevious}
          disabled={stepIndex === 0 || isLoadingStep}
        >
          Previous
        </button>
        <button
          type="button"
          onClick={playback.goNext}
          disabled={stepIndex >= totalSteps || isLoadingStep}
        >
          Next
        </button>
      </div>

      <div className="button-row">
        {isPlaying ? (
          <button type="button" onClick={playback.pause}>
            Pause
          </button>
        ) : (
          <button type="button" className="primary-button" onClick={playback.play}>
            Play Solution
          </button>
        )}
        <button type="button" className="secondary-button" onClick={playback.reset}>
          Reset Playback
        </button>
      </div>

      {playback.error && <p className="message message-error">{playback.error}</p>}
    </div>
  );
}

export default SolutionPanel;
