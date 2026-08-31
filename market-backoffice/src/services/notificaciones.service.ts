import { apiClient } from '@/services/http/ApiClient'
import { API_ENDPOINTS } from '@/config/endpoints'
import type { Notificacion } from '@/types/notificacion'
import type { Pagina } from '@/types/pagina'

class NotificacionesService {
  listarPorTienda(tiendaId: number, pagina: number, tamano: number, signal?: AbortSignal) {
    return apiClient.get<Pagina<Notificacion>>(API_ENDPOINTS.notificaciones.porTienda(tiendaId), {
      params: { page: pagina, size: tamano },
      signal,
    })
  }

  listarNoLeidas(tiendaId: number, pagina: number, tamano: number, signal?: AbortSignal) {
    return apiClient.get<Pagina<Notificacion>>(API_ENDPOINTS.notificaciones.noLeidas(tiendaId), {
      params: { page: pagina, size: tamano },
      signal,
    })
  }

  generar(tiendaId: number) {
    return apiClient.post<Notificacion[]>(API_ENDPOINTS.notificaciones.generar(tiendaId))
  }

  marcarLeida(tiendaId: number, id: number) {
    return apiClient.post<Notificacion>(API_ENDPOINTS.notificaciones.marcarLeida(tiendaId, id))
  }
}

export const notificacionesService = new NotificacionesService()
