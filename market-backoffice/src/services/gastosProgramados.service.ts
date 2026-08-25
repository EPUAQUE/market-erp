import { apiClient } from '@/services/http/ApiClient'
import { API_ENDPOINTS } from '@/config/endpoints'
import type { FrecuenciaGasto, GastoProgramado } from '@/types/gastoProgramado'

export interface DatosGastoProgramado {
  concepto: string
  monto: string
  frecuencia: FrecuenciaGasto
}

class GastosProgramadosService {
  listarPorTienda(tiendaId: number) {
    return apiClient.get<GastoProgramado[]>(API_ENDPOINTS.gastosProgramados.porTienda(tiendaId))
  }

  crear(tiendaId: number, datos: DatosGastoProgramado & { fechaInicio: string }) {
    return apiClient.post<GastoProgramado>(API_ENDPOINTS.gastosProgramados.porTienda(tiendaId), datos)
  }

  actualizar(tiendaId: number, id: number, datos: DatosGastoProgramado) {
    return apiClient.put<GastoProgramado>(API_ENDPOINTS.gastosProgramados.porId(tiendaId, id), datos)
  }

  activar(tiendaId: number, id: number) {
    return apiClient.post<GastoProgramado>(API_ENDPOINTS.gastosProgramados.activar(tiendaId, id))
  }

  desactivar(tiendaId: number, id: number) {
    return apiClient.post<GastoProgramado>(API_ENDPOINTS.gastosProgramados.desactivar(tiendaId, id))
  }

  generarPago(tiendaId: number, id: number) {
    return apiClient.post<GastoProgramado>(API_ENDPOINTS.gastosProgramados.generarPago(tiendaId, id))
  }
}

export const gastosProgramadosService = new GastosProgramadosService()
