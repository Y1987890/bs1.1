import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import type { ReactNode } from 'react';
import { authApi, type AuthUser } from '../api/auth';
import { authTokenStore } from '../api/request';

interface AuthContextValue {
  user: AuthUser | null;
  loading: boolean;
  login: (username: string, password: string, expectedRole?: AuthUser['role']) => Promise<void>;
  register: (username: string, password: string, displayName?: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    const token = authTokenStore.get();
    if (!token) {
      setLoading(false);
      return;
    }

    authApi.me()
      .then((u) => {
        if (!cancelled) setUser(u);
      })
      .catch(() => {
        authTokenStore.clear();
        if (!cancelled) setUser(null);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, []);

  const login = useCallback(async (username: string, password: string, expectedRole?: AuthUser['role']) => {
    const { token, user: loginUser } = await authApi.login(username, password, expectedRole);
    authTokenStore.set(token);
    setUser(loginUser);
  }, []);

  const register = useCallback(async (username: string, password: string, displayName?: string) => {
    await authApi.register(username, password, displayName);
    await login(username, password);
  }, [login]);

  const logout = useCallback(() => {
    authTokenStore.clear();
    setUser(null);
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({ user, loading, login, register, logout }),
    [user, loading, login, register, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return ctx;
}