import { useEffect, useState } from 'react';

import { getHealth } from '../api/cubeApi.js';

const CHECK_INTERVAL_MS = 5000;

// Asks the backend "are you alive?" now and then, and returns true or false.
export function useBackendStatus() {
  const [isOnline, setIsOnline] = useState(false);

  useEffect(() => {
    let isCancelled = false;

    async function checkHealth() {
      try {
        await getHealth();
        if (!isCancelled) {
          setIsOnline(true);
        }
      } catch (error) {
        if (!isCancelled) {
          setIsOnline(false);
        }
      }
    }

    checkHealth();
    const timerId = setInterval(checkHealth, CHECK_INTERVAL_MS);

    return () => {
      isCancelled = true;
      clearInterval(timerId);
    };
  }, []);

  return isOnline;
}
