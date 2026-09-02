/**
 * Formatea un monto (string/number del backend, con la precisión que traiga —
 * `precioUnitario`/`costoUnitario` llegan con 4 decimales) como dinero físico:
 * Quetzales con exactamente 2 decimales, ni más ni menos. Nunca interpolar un
 * monto crudo del backend directo en la plantilla — siempre pasar por acá.
 */
export function formatCurrency(valor: string | number | null | undefined): string {
  if (valor === null || valor === undefined || valor === '') return '—'
  const numero = Number(valor)
  if (Number.isNaN(numero)) return '—'
  return `Q ${numero.toFixed(2)}`
}

/** cantidad × precioUnitario — pasar el resultado por `formatCurrency` para mostrarlo. */
export function calcularSubtotal(
  cantidad: string | number | null | undefined,
  precioUnitario: string | number | null | undefined,
): number {
  return Number(cantidad ?? 0) * Number(precioUnitario ?? 0)
}
