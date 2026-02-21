import api from './api';

export const authService = {
  async register(userData) {
    const response = await api.post('/auth/register', userData);
    if (response.data.token) {
      localStorage.setItem('token', response.data.token);
      localStorage.setItem('user', JSON.stringify(response.data));
    }
    return response.data;
  },

  async login(email, password) {
    const response = await api.post('/auth/login', { email, password });
    if (response.data.token) {
      localStorage.setItem('token', response.data.token);
      localStorage.setItem('user', JSON.stringify(response.data));
    }
    return response.data;
  },

  logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
  },

  async getCurrentUser() {
    const response = await api.get('/auth/me');
    return response.data;
  },

  getStoredUser() {
    const user = localStorage.getItem('user');
    return user ? JSON.parse(user) : null;
  },

  isAuthenticated() {
    return !!localStorage.getItem('token');
  },

  async updateProfile(data) {
    const response = await api.put('/users/me', data);
    return response.data;
  },

  async updateStatus(userStatus) {
    const response = await api.put('/users/me/status', { userStatus });
    return response.data;
  },

  async updateVisibility(isPublic) {
    const response = await api.put('/users/me/visibility', { isPublic });
    return response.data;
  },

  async getAdminUsers(page = 0, size = 20) {
    const response = await api.get('/admin/users', { params: { page, size } });
    return response.data;
  },

  async getAdminOrganizations(page = 0, size = 20) {
    const response = await api.get('/admin/organizations', { params: { page, size } });
    return response.data;
  },

  async verifyOrganization(id) {
    const response = await api.put(`/admin/organizations/${id}/verify`);
    return response.data;
  },

  async getOpportunities(params = {}) {
    const response = await api.get('/opportunities', { params });
    return response.data;
  },

  async getOpportunity(id) {
    const response = await api.get(`/opportunities/${id}`);
    return response.data;
  },

  async getCategories() {
    const response = await api.get('/categories');
    return response.data;
  },

  async getOrganizations() {
    const response = await api.get('/organizations');
    return response.data;
  },
};
