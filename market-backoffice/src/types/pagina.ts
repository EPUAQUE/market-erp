/** Envelope de paginación que devuelven los listados paginados del backend. */
export interface Pagina<T> {
  contenido: T[]
  pagina: number
  tamano: number
  totalElementos: number
  totalPaginas: number
}
