import { describe, it, expect } from 'vitest'
import { formatCurrency, calcularSubtotal } from './money'

describe('formatCurrency', () => {
  it('redondea a 2 decimales un monto con más precisión (precioUnitario/costoUnitario)', () => {
    expect(formatCurrency('8.5000')).toBe('Q 8.50')
    expect(formatCurrency('10.1234')).toBe('Q 10.12')
  })

  it('completa a 2 decimales un entero', () => {
    expect(formatCurrency('16')).toBe('Q 16.00')
  })

  it('devuelve — para valores nulos/vacíos/inválidos', () => {
    expect(formatCurrency(null)).toBe('—')
    expect(formatCurrency(undefined)).toBe('—')
    expect(formatCurrency('')).toBe('—')
    expect(formatCurrency('no-es-numero')).toBe('—')
  })
})

describe('calcularSubtotal', () => {
  it('multiplica cantidad por precioUnitario', () => {
    expect(calcularSubtotal('3', '8.50')).toBeCloseTo(25.5)
  })

  it('trata cantidad/precio vacíos como cero', () => {
    expect(calcularSubtotal('', '8.50')).toBe(0)
    expect(calcularSubtotal('3', '')).toBe(0)
  })
})
