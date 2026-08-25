import axios, {} from 'axios';
import { environment } from '@/config/environment';
import { API_ENDPOINTS } from '@/config/endpoints';
import { tokenService } from './token.service';
import { ApiClientError, mapAxiosError } from './error.mapper';
/** Callback wired from the router (see router/index.ts) — kept decoupled to avoid circular imports. */
let onUnauthorized = null;
export function setOnUnauthorized(callback) {
    onUnauthorized = callback;
}
const axiosInstance = axios.create({
    baseURL: environment.apiBaseUrl,
    timeout: environment.apiTimeout,
    withCredentials: true,
});
axiosInstance.interceptors.request.use((config) => {
    const requiresAuth = config.requiresAuth ?? true;
    if (requiresAuth) {
        const token = tokenService.get();
        if (token) {
            config.headers.set('Authorization', `Bearer ${token}`);
        }
    }
    return config;
});
let refreshInFlight = null;
async function refreshAccessToken() {
    if (!refreshInFlight) {
        refreshInFlight = axiosInstance
            .post(API_ENDPOINTS.auth.refresh, undefined, { requiresAuth: false })
            .then((response) => {
            const token = response.data.accessToken;
            tokenService.set(token);
            return token;
        })
            .finally(() => {
            refreshInFlight = null;
        });
    }
    return refreshInFlight;
}
axiosInstance.interceptors.response.use((response) => response, async (error) => {
    const config = error.config;
    const requiresAuth = config?.requiresAuth ?? true;
    const status = error.response?.status;
    if (status === 401 && requiresAuth && !config?._retried) {
        try {
            const newToken = await refreshAccessToken();
            config._retried = true;
            config.headers.set('Authorization', `Bearer ${newToken}`);
            return axiosInstance.request(config);
        }
        catch {
            tokenService.clear();
            onUnauthorized?.();
        }
    }
    return Promise.reject(mapAxiosError(error));
});
export const apiClient = {
    get(url, options) {
        return axiosInstance
            .get(url, { requiresAuth: options?.requiresAuth, params: options?.params })
            .then((r) => r.data);
    },
    post(url, body, options) {
        return axiosInstance.post(url, body, { requiresAuth: options?.requiresAuth }).then((r) => r.data);
    },
    put(url, body, options) {
        return axiosInstance.put(url, body, { requiresAuth: options?.requiresAuth }).then((r) => r.data);
    },
    delete(url, options) {
        return axiosInstance.delete(url, { requiresAuth: options?.requiresAuth }).then((r) => r.data);
    },
};
export { ApiClientError };
