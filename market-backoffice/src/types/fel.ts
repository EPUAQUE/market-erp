export type EstadoDocumentoFel = 'PENDIENTE' | 'CERTIFICADO' | 'ANULADO' | 'ERROR'

export interface DocumentoFel {
  id: number
  ventaId: number
  tiendaId: number
  serie: string
  numero: number
  uuid: string | null
  estado: EstadoDocumentoFel
  fechaEmision: string
  fechaCertificacion: string | null
  motivoAnulacion: string | null
  mensajeError: string | null
}
