import { apiClient } from '@/services/http/ApiClient';
import { API_ENDPOINTS } from '@/config/endpoints';
class AuthService {
    login(username, password) {
        return apiClient.post(API_ENDPOINTS.auth.login, { username, password }, { requiresAuth: false });
    }
    me() {
        return apiClient.get(API_ENDPOINTS.auth.me);
    }
    logout() {
        return apiClient.post(API_ENDPOINTS.auth.logout, undefined, { requiresAuth: false });
    }
}
export const authService = new AuthService();
