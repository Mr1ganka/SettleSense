import { apiRequest } from './client';

export type AuthResponse = {
  token: string;
  userId: number;
  email: string;
  displayName: string;
};

export type LoginRequestPayload = {
  email: string;
  password: string;
};

export type RegisterRequestPayload = {
  displayName: string;
  email: string;
  password: string;
};

export function loginApi(payload: LoginRequestPayload): Promise<AuthResponse> {
  return apiRequest<AuthResponse>('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export function registerApi(payload: RegisterRequestPayload): Promise<AuthResponse> {
  return apiRequest<AuthResponse>('/api/users', {
    method: 'POST',
    body: JSON.stringify(payload),
  }).then(async () => {
    // Automatically log in after registration to get auth token
    return loginApi({ email: payload.email, password: payload.password });
  });
}

