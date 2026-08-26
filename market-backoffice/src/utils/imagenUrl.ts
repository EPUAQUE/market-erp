import { environment } from '@/config/environment'

/**
 * `Producto.imagenUrl` guarda una ruta relativa (`/api/v1/productos/imagenes/...`)
 * desde que la subida de archivos reemplazó al campo de texto libre — pero
 * productos creados antes de ese cambio pueden seguir teniendo una URL externa
 * absoluta. Ambos casos deben renderizar, así que solo se antepone la base cuando
 * no es ya absoluta.
 */
export function resolverImagenUrl(imagenUrl: string | null | undefined): string | undefined {
  if (!imagenUrl) return undefined
  if (imagenUrl.startsWith('http://') || imagenUrl.startsWith('https://')) return imagenUrl
  return `${environment.apiBaseUrl}${imagenUrl}`
}
