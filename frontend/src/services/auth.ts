import api from './api';
import type { LoginRequest, LoginResponse } from '../types';

export async function login(data: LoginRequest): Promise<LoginResponse> {
  const res = await api.post<LoginResponse>('/auth/login', data);
  return res.data;
}

export async function register(data: {
  username: string;
  email: string;
  password: string;
  nombres: string;
  apellidos: string;
  roles: string[];
}) {
  const res = await api.post('/auth/register', data);
  return res.data;
}

export function decodeToken(token: string): { sub: string; roles: string[]; email?: string; nombres?: string; apellidos?: string } | null {
  try {
    const payload = token.split('.')[1];
    const decoded = JSON.parse(atob(payload));
    const rawRoles = decoded.groups ?? decoded.roles ?? [];
    const roles = Array.isArray(rawRoles)
      ? rawRoles
      : String(rawRoles)
          .split(/[,\s]+/)
          .map((role) => role.trim())
          .filter(Boolean);

    return {
      sub: decoded.sub || decoded.username || '',
      roles,
      email: decoded.email,
      nombres: decoded.nombres,
      apellidos: decoded.apellidos,
    };
  } catch {
    return null;
  }
}

