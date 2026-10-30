import { request } from './request';

export type Role = 'CANDIDATE' | 'INTERVIEWER' | 'ADMIN';

export interface AuthUser {
  userId: number;
  username: string;
  role: Role;
}

export interface AuthToken {
  token: string;
  user: AuthUser;
}

export const authApi = {
  async login(username: string, password: string, expectedRole?: Role): Promise<AuthToken> {
    return request.post<AuthToken>('/api/v1/auth/login', { username, password, expectedRole });
  },

  async register(username: string, password: string, displayName?: string): Promise<AuthUser> {
    return request.post<AuthUser>('/api/v1/auth/register', { username, password, displayName });
  },

  async me(): Promise<AuthUser> {
    return request.get<AuthUser>('/api/v1/auth/me');
  },
};