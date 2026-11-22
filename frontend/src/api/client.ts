// A tiny wrapper around fetch() that:
//  - sends/receives JSON
//  - turns the backend's error body { error, fields } into an ApiError we can show in the UI

const BASE_URL = '/api/v1'; // Vite forwards /api to Spring Boot (see vite.config.ts)

// ---- Sign-in (JWT) ----------------------------------------------------------
// After login the backend returns a token; every request sends "Authorization: Bearer <token>".
// The token (never the password) is kept in sessionStorage: it survives a page refresh and is
// removed when the browser tab is closed. It also expires on the server after 8 hours.
const TOKEN_KEY = 'candidate-crm.token';
let token: string | null = readStoredToken();
let onUnauthorized: (() => void) | null = null;

function readStoredToken(): string | null {
  try {
    return sessionStorage.getItem(TOKEN_KEY);
  } catch {
    return null; // storage blocked by the browser: stay signed in until refresh only
  }
}

export function setToken(value: string): void {
  token = value;
  try {
    sessionStorage.setItem(TOKEN_KEY, value);
  } catch {
    // ignore: the token still works for this page
  }
}

export function clearToken(): void {
  token = null;
  try {
    sessionStorage.removeItem(TOKEN_KEY);
  } catch {
    // ignore
  }
}

export function hasToken(): boolean {
  return token !== null;
}

/** Called when the backend answers 401 to a signed-in request, e.g. the token expired. */
export function setUnauthorizedHandler(handler: (() => void) | null): void {
  onUnauthorized = handler;
}

/** An error returned by the backend, e.g. 400 / 404 / 409. */
export class ApiError extends Error {
  readonly status: number;
  /** Field-level validation messages, e.g. { email: "must be a well-formed email address" } */
  readonly fields: Record<string, string>;

  constructor(status: number, message: string, fields: Record<string, string> = {}) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.fields = fields;
  }
}

interface RequestOptions {
  /** false = let the caller handle a 401 itself (the login page does). */
  signOutOn401?: boolean;
}

export async function request<T>(
  method: string,
  path: string,
  body?: unknown,
  { signOutOn401 = true }: RequestOptions = {},
): Promise<T> {
  const headers: Record<string, string> = {};
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  if (token) headers['Authorization'] = `Bearer ${token}`;

  let response: Response;
  try {
    response = await fetch(BASE_URL + path, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  } catch {
    // The server is not running or the network is down.
    throw new ApiError(0, 'Cannot reach the server. Is the backend running?');
  }

  if (response.status === 401 && signOutOn401) {
    clearToken();
    onUnauthorized?.(); // back to the login page
  }

  if (response.status === 204) {
    return undefined as T; // e.g. DELETE: no body
  }

  // Read the body once; it may be JSON or empty.
  const text = await response.text();
  const data = text ? safeJson(text) : null;

  if (!response.ok) {
    const errorBody = (data && typeof data === 'object' ? data : {}) as {
      error?: unknown;
      fields?: Record<string, string>;
    };
    const message = errorBody.error ? String(errorBody.error) : `Request failed (${response.status})`;
    throw new ApiError(response.status, message, errorBody.fields ?? {});
  }

  return data as T;
}

function safeJson(text: string): unknown {
  try {
    return JSON.parse(text);
  } catch {
    return null;
  }
}
