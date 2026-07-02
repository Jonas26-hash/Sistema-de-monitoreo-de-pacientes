import { createContext, useContext, useState, useCallback, useEffect, type ReactNode } from 'react';
import type { LoginRequest } from '../types';
import { login as loginApi, decodeToken } from '../services/auth';
import api from '../services/api';

export interface AuthUser {
  username: string;
  email?: string;
  roles: string[];
  nombres?: string;
  apellidos?: string;
  avatar?: string;
}

interface AuthContextType {
  user: AuthUser | null;
  token: string | null;
  loading: boolean;
  login: (data: LoginRequest) => Promise<void>;
  logout: () => void;
  updateUser: (data: Partial<AuthUser>) => void;
  setAuthFromToken: (token: string) => void;
  isAuthenticated: boolean;
  hasRole: (role: string) => boolean;
}

const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(() => {
    const stored = localStorage.getItem('auth_user');
    return stored ? JSON.parse(stored) : null;
  });
  const [token, setToken] = useState<string | null>(() => localStorage.getItem('auth_token'));
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (token) {
      const decoded = decodeToken(token);
      if (decoded) {
        const u: AuthUser = { username: decoded.sub, email: decoded.email, roles: decoded.roles, nombres: decoded.nombres, apellidos: decoded.apellidos };
        setUser(u);
        if (u.username) {
          api.get('/auth/profile').then((res) => {
            const me = res.data;
            if (me.avatar) {
              u.avatar = me.avatar;
              setUser({ ...u });
              localStorage.setItem('auth_user', JSON.stringify(u));
            }
          }).catch(() => {});
        }
        localStorage.setItem('auth_user', JSON.stringify(u));
        localStorage.removeItem('user_display_name');
      }
    }
  }, [token]);

  const login = useCallback(async (data: LoginRequest) => {
    setLoading(true);
    try {
      const res = await loginApi(data);
      localStorage.setItem('auth_token', res.token);
      localStorage.removeItem('user_display_name');
      setToken(res.token);
      const decoded = decodeToken(res.token);
      const u: AuthUser = { username: decoded?.sub || res.username, email: decoded?.email || res.email, roles: decoded?.roles || res.roles, nombres: decoded?.nombres, apellidos: decoded?.apellidos, avatar: res.avatar };
      setUser(u);
      localStorage.setItem('auth_user', JSON.stringify(u));
    } finally {
      setLoading(false);
    }
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem('auth_token');
    localStorage.removeItem('auth_user');
    localStorage.removeItem('user_display_name');
    setUser(null);
    setToken(null);
  }, []);

  const hasRole = useCallback((role: string) => user?.roles?.includes(role) ?? false, [user]);

  const updateUser = useCallback((data: Partial<AuthUser>) => {
    setUser((prev) => {
      if (!prev) return prev;
      const updated = { ...prev, ...data };
      localStorage.setItem('auth_user', JSON.stringify(updated));
      return updated;
    });
  }, []);

  const setAuthFromToken = useCallback((newToken: string) => {
    localStorage.setItem('auth_token', newToken);
    setToken(newToken);
    const decoded = decodeToken(newToken);
    if (decoded) {
      const u: AuthUser = { username: decoded.sub, email: decoded.email, roles: decoded.roles, nombres: decoded.nombres, apellidos: decoded.apellidos };
      setUser(u);
      api.get('/auth/profile').then((res) => {
        const me = res.data;
        if (me.avatar) {
          u.avatar = me.avatar;
          setUser({ ...u });
          localStorage.setItem('auth_user', JSON.stringify(u));
        }
      }).catch(() => {});
      localStorage.setItem('auth_user', JSON.stringify(u));
    }
  }, []);

  return (
    <AuthContext.Provider value={{ user, token, loading, login, logout, updateUser, setAuthFromToken, isAuthenticated: !!token, hasRole }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth debe usarse dentro de AuthProvider');
  return ctx;
}

