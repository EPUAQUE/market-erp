import { apiClient } from '@/services/http/ApiClient'
import { API_ENDPOINTS } from '@/config/endpoints'
import type { Notificacion } from '@/types/notificacion'

class NotificacionesService {
  listarPorTienda(tiendaId: number) {
    return apiClient.get<Notificacion[]>(API_ENDPOINTS.notificaciones.porTienda(tiendaId))
  }

  listarNoLeidas(tiendaId: number) {
    return apiClient.get<Notificacion[]>(API_ENDPOINTS.notificaciones.noLeidas(tiendaId))
  }

  generar(tiendaId: number) {
    return apiClient.post<Notificacion[]>(API_ENDPOINTS.notificaciones.generar(tiendaId))
  }

  marcarLeida(tiendaId: number, id: number) {
    return apiClient.post<Notificacion>(API_ENDPOINTS.notificaciones.marcarLeida(tiendaId, id))
  }
}

export const notificacionesService = new NotificacionesService()
