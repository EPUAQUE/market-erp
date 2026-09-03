import { apiClient } from '@/services/http/ApiClient'
import { API_ENDPOINTS } from '@/config/endpoints'
import type { DocumentoFel } from '@/types/fel'
import type { Pagina } from '@/types/pagina'

class FelService {
  listarPorTienda(tiendaId: number, pagina: number, tamano: number, signal?: AbortSignal) {
    return apiClient.get<Pagina<DocumentoFel>>(API_ENDPOINTS.fel.porTienda(tiendaId), {
      params: { page: pagina, size: tamano },
      signal,
    })
  }

  obtener(tiendaId: number, id: number, signal?: AbortSignal) {
    return apiClient.get<DocumentoFel>(API_ENDPOINTS.fel.porId(tiendaId, id), { signal })
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
