import { useCallback, useEffect, useRef, useState } from 'react';

import { applyMoves } from '../api/cubeApi.js';

const STEP_DELAY_MS = 800;

// Steps through a solution one move at a time.
// Every cube state shown comes from the Java backend (/apply). Each state is
// kept in "frames" so going back and replaying never needs another request.
export function useSolutionPlayback(startState, moves, onStateChange) {
  const [stepIndex, setStepIndex] = useState(0);
  const [isPlaying, setIsPlaying] = useState(false);
  const [isLoadingStep, setIsLoadingStep] = useState(false);
  const [error, setError] = useState('');
  const framesRef = useRef([]);

  const totalSteps = moves ? moves.length : 0;

  // A new solution arrived (or it was cleared): start again from step 0.
  useEffect(() => {
    framesRef.current = startState ? [startState] : [];
    setStepIndex(0);
    setIsPlaying(false);
    setError('');
  }, [startState, moves]);

  const goNext = useCallback(async () => {
    if (isLoadingStep || stepIndex >= totalSteps) {
      return;
    }

    const targetIndex = stepIndex + 1;
    let frame = framesRef.current[targetIndex];

    if (!frame) {
      setIsLoadingStep(true);
      try {
        const currentFrame = framesRef.current[stepIndex];
        const response = await applyMoves(currentFrame, [moves[stepIndex]]);
        frame = response.state;
        framesRef.current[targetIndex] = frame;
      } catch (requestError) {
        setError(requestError.message);
        setIsPlaying(false);
        return;
      } finally {
        setIsLoadingStep(false);
      }
    }

    setStepIndex(targetIndex);
    onStateChange(frame);
  }, [isLoadingStep, stepIndex, totalSteps, moves, onStateChange]);

  const goPrevious = useCallback(() => {
    if (stepIndex <= 0) {
      return;
    }
    const targetIndex = stepIndex - 1;
    setIsPlaying(false);
    setStepIndex(targetIndex);
    onStateChange(framesRef.current[targetIndex]);
  }, [stepIndex, onStateChange]);

  const reset = useCallback(() => {
    if (framesRef.current.length === 0) {
      return;
    }
    setIsPlaying(false);
    setStepIndex(0);
    onStateChange(framesRef.current[0]);
  }, [onStateChange]);

  const play = useCallback(() => {
    if (totalSteps === 0) {
      return;
    }
    if (stepIndex >= totalSteps) {
      // Finished already: rewind, then play again from the beginning.
      setStepIndex(0);
      onStateChange(framesRef.current[0]);
    }
    setIsPlaying(true);
  }, [stepIndex, totalSteps, onStateChange]);

  const pause = useCallback(() => {
    setIsPlaying(false);
  }, []);

  // While playing, wait a moment and then take the next step.
  useEffect(() => {
    if (!isPlaying) {
      return undefined;
    }
    if (stepIndex >= totalSteps) {
      setIsPlaying(false);
      return undefined;
    }

    const timerId = setTimeout(goNext, STEP_DELAY_MS);
    return () => clearTimeout(timerId);
  }, [isPlaying, stepIndex, totalSteps, goNext]);

  return {
    stepIndex,
    totalSteps,
    isPlaying,
    isLoadingStep,
    error,
    goNext,
    goPrevious,
    reset,
    play,
    pause,
  };
}
