import axios from 'axios';

const API_BASE_URL = process.env.REACT_APP_BACKEND_URL || 'http://localhost:8001';

// Add token to all requests
axios.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Handle token expiration
axios.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.location.reload();
    }
    return Promise.reject(error);
  }
);

export const authAPI = {
  login: (credentials) => axios.post(`${API_BASE_URL}/api/auth/login`, credentials),
  register: (userData) => axios.post(`${API_BASE_URL}/api/auth/register`, userData),
};