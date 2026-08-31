import axios, { type AxiosInstance, type InternalAxiosRequestConfig } from 'axios'
import { environment } from '@/config/environment'
import { API_ENDPOINTS } from '@/config/endpoints'
import { tokenService } from './token.service'
import { ApiClientError, mapAxiosError } from './error.mapper'

export interface ApiRequestOptions {
  requiresAuth?: boolean
  signal?: AbortSignal
}

declare module 'axios' {
  interface AxiosRequestConfig {
    requiresAuth?: boolean
    _retried?: boolean
  }
  interface InternalAxiosRequestConfig {
    requiresAuth?: boolean
    _retried?: boolean
  }
}

/** Callback wired from the router (see router/index.ts) — kept decoupled to avoid circular imports. */
let onUnauthorized: (() => void) | null = null
export function setOnUnauthorized(callback: () => void): void {
  onUnauthorized = callback
}

const axiosInstance: AxiosInstance = axios.create({
  baseURL: environment.apiBaseUrl,
  timeout: environment.apiTimeout,
  withCredentials: true,
})

axiosInstance.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const requiresAuth = config.requiresAuth ?? true
  if (requiresAuth) {
    const token = tokenService.get()
    if (token) {
      config.headers.set('Authorization', `Bearer ${token}`)
    }
  }
  return config
})

let refreshInFlight: Promise<string> | null = null

async function refreshAccessToken(): Promise<string> {
  if (!refreshInFlight) {
    refreshInFlight = axiosInstance
      .post(API_ENDPOINTS.auth.refresh, undefined, { requiresAuth: false })
      .then((response) => {
        const token = response.data.accessToken as string
        tokenService.set(token)
        return token
      })
      .finally(() => {
        refreshInFlight = null
      })
  }
  return refreshInFlight
}

axiosInstance.interceptors.response.use(
  (response) => response,
  async (error) => {
    const config = error.config as InternalAxiosRequestConfig | undefined
    const requiresAuth = config?.requiresAuth ?? true
    const status = error.response?.status

    if (status === 401 && requiresAuth && !config?._retried) {
      try {
        const newToken = await refreshAccessToken()
        config!._retried = true
        config!.headers.set('Authorization', `Bearer ${newToken}`)
        return axiosInstance.request(config!)
      } catch {
        tokenService.clear()
        onUnauthorized?.()
      }
    }

    return Promise.reject(mapAxiosError(error))
  },
)

/** Exportado para el intento de refresh silencioso al montar la app (ver auth.store.ts) — reusa el mismo dedupe de refresh-en-vuelo que el interceptor de 401. */
export { refreshAccessToken }

export const apiClient = {
  get<T>(url: string, options?: ApiRequestOptions & { params?: Record<string, unknown> }) {
    return axiosInstance
      .get<T>(url, {
        requiresAuth: options?.requiresAuth,
        params: options?.params,
        signal: options?.signal,
      })
      .then((r) => r.data)
  },
  post<T>(url: string, body?: unknown, options?: ApiRequestOptions) {
    return axiosInstance
      .post<T>(url, body, { requiresAuth: options?.requiresAuth, signal: options?.signal })
      .then((r) => r.data)
  },
  put<T>(url: string, body?: unknown, options?: ApiRequestOptions) {
    return axiosInstance
      .put<T>(url, body, { requiresAuth: options?.requiresAuth, signal: options?.signal })
      .then((r) => r.data)
  },
  delete<T>(url: string, options?: ApiRequestOptions) {
    return axiosInstance
      .delete<T>(url, { requiresAuth: options?.requiresAuth, signal: options?.signal })
      .then((r) => r.data)
  },
}

export { ApiClientError }
