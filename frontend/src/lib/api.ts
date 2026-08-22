import axios, { AxiosInstance, InternalAxiosRequestConfig } from 'axios';
import type { User, DashboardData, SyncStatus, Email, JobApplication, TravelTrip } from '@/types';

// Use relative URLs for combined deployment (same origin)
class ApiClient {
  private client: AxiosInstance;
  private accessToken: string | null = null;

  constructor() {
    this.client = axios.create({
      baseURL: '', // Relative URLs
      headers: {
        'Content-Type': 'application/json',
      },
      withCredentials: false, // We use Authorization header
    });

    this.client.interceptors.request.use(
      (config: InternalAxiosRequestConfig) => {
        if (this.accessToken) {
          config.headers.Authorization = `Bearer ${this.accessToken}`;
        }
        // Ensure URL is relative
        if (config.url && !config.url.startsWith('/') && !config.url.startsWith('http')) {
          config.url = '/' + config.url;
        }
        return config;
      },
      (error) => Promise.reject(error)
    );

    this.client.interceptors.response.use(
      (response) => response,
      async (error) => {
        if (error.response?.status === 401) {
          this.accessToken = null;
          if (typeof window !== 'undefined') {
            window.location.href = '/auth/login';
          }
        }
        return Promise.reject(error);
      }
    );
  }

  setAccessToken(token: string | null) {
    this.accessToken = token;
  }

  // Auth
  async getAuthUrl(): Promise<{ authUrl: string }> {
    const response = await this.client.get('/api/auth/google/url');
    return response.data;
  }

  async handleCallback(code: string): Promise<{ user: User; accessToken: string }> {
    const response = await this.client.post('/api/auth/google/callback', { code });
    this.accessToken = response.data.accessToken;
    return response.data;
  }

  async getCurrentUser(): Promise<User> {
    const response = await this.client.get('/api/auth/me');
    return response.data;
  }

  async logout(): Promise<void> {
    await this.client.post('/api/auth/logout');
    this.accessToken = null;
  }

  // Dashboard
  async getDashboard(): Promise<DashboardData> {
    const response = await this.client.get('/api/dashboard');
    return response.data;
  }

  // Emails
  async getEmails(params?: { category?: string; limit?: number; offset?: number }): Promise<{ emails: Email[]; total: number }> {
    const response = await this.client.get('/api/emails', { params });
    return response.data;
  }

  async getEmail(id: string): Promise<Email> {
    const response = await this.client.get(`/api/emails/${id}`);
    return response.data;
  }

  // Sync
  async getSyncStatus(): Promise<SyncStatus> {
    const response = await this.client.get('/api/dashboard/sync/status');
    return response.data;
  }

  async triggerSync(): Promise<{ success: boolean }> {
    const response = await this.client.post('/api/dashboard/sync/trigger');
    return response.data;
  }

  // Jobs
  async getJobApplications(): Promise<JobApplication[]> {
    const response = await this.client.get('/api/jobs');
    return response.data;
  }

  // Travel
  async getTrips(): Promise<TravelTrip[]> {
    const response = await this.client.get('/api/travel/trips');
    return response.data;
  }

  // Search
  async searchEmails(query: string): Promise<Email[]> {
    const response = await this.client.get('/api/search', { params: { q: query } });
    return response.data;
  }

  // AI Assistant
  async askAssistant(question: string): Promise<{ answer: string; sources: string[] }> {
    const response = await this.client.post('/api/assistant/ask', { question });
    return response.data;
  }
}

export const api = new ApiClient();