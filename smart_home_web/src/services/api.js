import axios from 'axios';

// Base API URL - uses proxy in development, direct in production
const API_BASE_URL = process.env.REACT_APP_API_URL || '/api';

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Add auth token to requests if available
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

export const deviceApi = {
  // Get all devices
  getDevices: () => apiClient.get('/devices'),
  
  // Get device by ID
  getDevice: (id) => apiClient.get(`/devices/${id}`),
  
  // Control device (send raw string "0" or "1")
  controlDevice: (id, command) => 
    apiClient.post(`/devices/${id}/control`, command, {
      headers: { 'Content-Type': 'text/plain' }
    }),
  
  // Get device telemetry
  getTelemetry: (deviceId, limit = 100) => 
    apiClient.get(`/telemetry/${deviceId}`, { params: { limit } }),
};

export const automationApi = {
  // Get all automations
  getAutomations: () => apiClient.get('/automations'),
  
  // Get automation by ID
  getAutomation: (id) => apiClient.get(`/automations/${id}`),
  
  // Create automation
  createAutomation: (data) => apiClient.post('/automations', data),
  
  // Update automation
  updateAutomation: (id, data) => apiClient.put(`/automations/${id}`, data),
  
  // Delete automation
  deleteAutomation: (id) => apiClient.delete(`/automations/${id}`),
  
  // Toggle automation active status
  toggleAutomation: (id, isActive) => 
    apiClient.patch(`/automations/${id}/toggle`, { isActive }),
};

export const authApi = {
  login: (username, password) => 
    apiClient.post('/auth/login', { username, password }),
  
  register: (userData) => 
    apiClient.post('/auth/register', userData),
};

export default apiClient;
