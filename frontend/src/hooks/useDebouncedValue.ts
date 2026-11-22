import { useEffect, useState } from 'react';

/**
 * Returns `value`, but only after it has stopped changing for `delayMs`.
 * Used for the search box: we call the API once the user pauses typing,
 * not on every key press.
 */
export function useDebouncedValue<T>(value: T, delayMs = 400): T {
  const [debounced, setDebounced] = useState(value);

  useEffect(() => {
    const timer = setTimeout(() => setDebounced(value), delayMs);
    return () => clearTimeout(timer); // a new key press cancels the previous timer
  }, [value, delayMs]);

  return debounced;
}
