function pad(n: number): string {
  return String(n).padStart(2, '0')
}

/**
 * Guatemala usa dd/MM/yyyy — `toLocaleDateString()` sin locale explícito
 * depende del navegador del usuario (en Chrome con locale en-US da
 * MM/DD/YYYY). Nunca interpolar `new Date(...).toLocaleDateString()`
 * directo en la plantilla — siempre pasar por acá.
 */
export function formatFecha(iso: string | null | undefined): string {
  if (!iso) return '—'
  const fecha = new Date(iso)
  if (Number.isNaN(fecha.getTime())) return '—'
  return `${pad(fecha.getDate())}/${pad(fecha.getMonth() + 1)}/${fecha.getFullYear()}`
}

/** Igual que `formatFecha`, agregando hora:minuto en formato 24h. */
export function formatFechaHora(iso: string | null | undefined): string {
  if (!iso) return '—'
  const fecha = new Date(iso)
  if (Number.isNaN(fecha.getTime())) return '—'
  return `${formatFecha(iso)} ${pad(fecha.getHours())}:${pad(fecha.getMinutes())}`
}
