import { ApiError, clearToken, hasToken, request, setToken } from './client';

/** The signed-in recruiter. */
export interface CurrentUser {
  username: string;
  displayName: string;
}

interface LoginResponse extends CurrentUser {
  accessToken: string;
  tokenType: string;
  expiresAt: string;
}

/** POST /auth/login: on success the token is stored and sent with every later request. */
export async function login(username: string, password: string): Promise<CurrentUser> {
  const response = await request<LoginResponse>(
    'POST',
    '/auth/login',
    { username, password },
    { signOutOn401: false }, // a wrong password is shown on the login page
  );
  setToken(response.accessToken);
  return { username: response.username, displayName: response.displayName };
}

/** After a page refresh: if a token is stored and still valid, returns its user; otherwise null. */
export async function restoreSession(): Promise<CurrentUser | null> {
  if (!hasToken()) return null;
  try {
    return await request<CurrentUser>('GET', '/auth/me', undefined, { signOutOn401: false });
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) clearToken(); // expired or invalid
    return null;
  }
}

/** JWTs are stateless: signing out just forgets the token in this browser. */
export function logout(): void {
  clearToken();
}
