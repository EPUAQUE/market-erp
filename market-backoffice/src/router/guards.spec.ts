import { createPinia, setActivePinia } from 'pinia'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import type { NavigationGuardNext, RouteLocationNormalized, RouteLocationNormalizedLoaded } from 'vue-router'
import { tokenService } from '@/services/http/token.service'
import { usePermissionsStore } from '@/stores/permissions.store'

vi.mock('@/services/auth/AuthService', () => ({
  authService: {
    login: vi.fn(),
    me: vi.fn(),
    logout: vi.fn(),
  },
}))
vi.mock('@/services/http/ApiClient', () => ({
  refreshAccessToken: vi.fn(),
}))

const { authService } = await import('@/services/auth/AuthService')
const { refreshAccessToken } = await import('@/services/http/ApiClient')
const { authGuard } = await import('./guards')

function ruta(meta: Record<string, unknown> = {}, fullPath = '/protegida'): RouteLocationNormalized {
  return { meta, fullPath } as unknown as RouteLocationNormalized
}

const from = {} as RouteLocationNormalizedLoaded
const next: NavigationGuardNext = () => {}

function meResponse(permisos: string[] = []) {
  return { username: 'ana', permisos, tiendaIds: [1], alcanceGlobal: false, grupoIds: [] }
}

describe('authGuard', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    tokenService.clear()
    vi.mocked(authService.me).mockReset()
    vi.mocked(refreshAccessToken).mockReset()
  })

  it('deja pasar sin ninguna verificación cuando requiresAuth es false', async () => {
    const resultado = await authGuard.call(undefined, ruta({ requiresAuth: false }), from, next)

    expect(resultado).toBe(true)
    expect(refreshAccessToken).not.toHaveBeenCalled()
  })

  it('sin token, refresh silencioso exitoso: deja pasar', async () => {
    vi.mocked(refreshAccessToken).mockResolvedValue('token-nuevo')
    vi.mocked(authService.me).mockResolvedValue(meResponse())

    const resultado = await authGuard.call(undefined, ruta({}), from, next)

    expect(resultado).toBe(true)
  })

  it('sin token, refresh silencioso fallido: redirige a login con el destino original', async () => {
    vi.mocked(refreshAccessToken).mockRejectedValue(new Error('sin cookie'))

    const resultado = await authGuard.call(undefined, ruta({}, '/ventas/42'), from, next)

    expect(resultado).toEqual({ name: 'login', query: { redirect: '/ventas/42' } })
  })

  it('con token pero sin autorización cargada, si loadAuthorization falla redirige a login', async () => {
    tokenService.set('token-existente')
    vi.mocked(authService.me).mockRejectedValue(new Error('401'))

    const resultado = await authGuard.call(undefined, ruta({}), from, next)

    expect(resultado).toEqual({ name: 'login' })
  })

  it('con permiso requerido y el usuario no lo tiene, redirige a forbidden', async () => {
    tokenService.set('token-existente')
    vi.mocked(authService.me).mockResolvedValue(meResponse(['PRODUCTOS_VER']))

    const resultado = await authGuard.call(undefined, ruta({ permission: 'CAJA_CERRAR' }), from, next)

    expect(resultado).toEqual({ name: 'forbidden' })
  })

  it('con permiso requerido y el usuario sí lo tiene, deja pasar', async () => {
    tokenService.set('token-existente')
    vi.mocked(authService.me).mockResolvedValue(meResponse(['CAJA_CERRAR']))

    const resultado = await authGuard.call(undefined, ruta({ permission: 'CAJA_CERRAR' }), from, next)

    expect(resultado).toBe(true)
  })

  it('no vuelve a llamar loadAuthorization si ya estaba cargada', async () => {
    tokenService.set('token-existente')
    vi.mocked(authService.me).mockResolvedValue(meResponse(['CAJA_CERRAR']))
    usePermissionsStore().hydrate(['CAJA_CERRAR'], [1], false)
    // Simula que una navegación anterior ya completó loadAuthorization.
    const { useAuthStore } = await import('@/stores/auth.store')
    useAuthStore().authorizationLoaded = true

    await authGuard.call(undefined, ruta({}), from, next)

    expect(authService.me).not.toHaveBeenCalled()
  })
})
