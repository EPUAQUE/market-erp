import { apiClient } from '@/services/http/ApiClient'
import { API_ENDPOINTS } from '@/config/endpoints'
import type { LoginResponse, MeResponse } from '@/types/auth'

class AuthService {
  login(username: string, password: string) {
    return apiClient.post<LoginResponse>(
      API_ENDPOINTS.auth.login,
      { username, password },
      { requiresAuth: false },
    )
  }

  me() {
    return apiClient.get<MeResponse>(API_ENDPOINTS.auth.me)
  }

  logout() {
    return apiClient.post<void>(API_ENDPOINTS.auth.logout, undefined, { requiresAuth: false })
  }
}

export const authService = new AuthService()
