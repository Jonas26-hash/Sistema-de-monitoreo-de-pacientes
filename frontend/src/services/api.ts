import axios from 'axios';
import { message } from 'antd';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api',
});

let last503Msg = 0;

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('auth_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 403 && error.response?.data?.error === "cuenta desactivada") {
      localStorage.removeItem('auth_token');
      localStorage.removeItem('auth_user');
      message.error('Tu cuenta ha sido desactivada. Contacta al administrador.', 6);
      window.location.href = '/login';
      return Promise.reject(error);
    }
    if (error.response?.status === 401) {
      const path = window.location.pathname;
      if (path !== '/login' && path !== '/register') {
        localStorage.removeItem('auth_token');
        localStorage.removeItem('auth_user');
        window.location.href = '/login';
      }
    }
    if (error.response?.status === 503) {
      const now = Date.now();
      if (now - last503Msg > 8000) {
        last503Msg = now;
        const data = error.response.data;
        const url = error.config?.url || '';
        message.warning(`${url} — Servicio no disponible`, 4);
      }
    }
    if (!error.response && !navigator.onLine) {
      const path = window.location.pathname;
      if (path !== '/offline' && path !== '/login' && path !== '/register') {
        window.location.href = '/offline';
      }
    }
    return Promise.reject(error);
  }
);

export default api;

