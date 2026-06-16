export type AuthUser = {
  id: string;
  email: string;
  displayName: string;
};

export type AuthState = {
  status: 'anonymous' | 'loading' | 'authenticated';
  accessToken: string | null;
  user: AuthUser | null;
};

export const initialAuthState: AuthState = {
  status: 'anonymous',
  accessToken: null,
  user: null,
};
