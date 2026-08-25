/**
 * Códigos de permiso planos tal como los devuelve el backend (RBAC + alcance por
 * tienda, ver market-backend/seguridad-desarrolladores.md §1). No es una unión
 * literal cerrada: el backend puede agregar códigos de otros módulos antes de
 * que el frontend los conozca — por eso `can()` trabaja sobre `string`.
 */
export type PermissionCode = string

export interface MeResponse {
  username: string
  permisos: PermissionCode[]
  tiendaIds: number[]
  alcanceGlobal: boolean
}

export interface LoginResponse {
  accessToken: string
  tokenType: string
  expiresIn: number
}
