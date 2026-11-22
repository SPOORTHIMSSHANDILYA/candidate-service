// Safe wrappers around the browser's localStorage.
// localStorage can be missing or full (private mode, quotas), so never let it crash the app.

export function loadJson<T>(key: string, fallback: T): T {
  try {
    const raw = window.localStorage.getItem(key);
    return raw ? (JSON.parse(raw) as T) : fallback;
  } catch {
    return fallback;
  }
}

export function saveJson(key: string, value: unknown): void {
  try {
    window.localStorage.setItem(key, JSON.stringify(value));
  } catch {
    // ignore: the feature just won't be remembered in this browser
  }
}
