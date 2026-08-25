import { apiClient } from '@/services/http/ApiClient'
import { API_ENDPOINTS } from '@/config/endpoints'
import type { DocumentoFel } from '@/types/fel'

class FelService {
  listarPorTienda(tiendaId: number) {
    return apiClient.get<DocumentoFel[]>(API_ENDPOINTS.fel.porTienda(tiendaId))
  }

  emitir(tiendaId: number, ventaId: number) {
    return apiClient.post<DocumentoFel>(API_ENDPOINTS.fel.emitir(tiendaId, ventaId))
  }

  reintentar(tiendaId: number, id: number) {
    return apiClient.post<DocumentoFel>(API_ENDPOINTS.fel.reintentar(tiendaId, id))
  }

  anular(tiendaId: number, id: number, motivo: string) {
    return apiClient.post<DocumentoFel>(API_ENDPOINTS.fel.anular(tiendaId, id), { motivo })
  }
}

export const felService = new FelService()
