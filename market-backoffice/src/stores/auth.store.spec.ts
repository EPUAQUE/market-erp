import { createPinia, setActivePinia } from 'pinia'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { tokenService } from '@/services/http/token.service'
import { usePermissionsStore } from './permissions.store'
import { useUserStore } from './user.store'

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
const { useAuthStore } = await import('./auth.store')

function meResponse() {
  return {
    username: 'ana',
    permisos: ['VENTAS_VER'],
    tiendaIds: [1, 2],
    alcanceGlobal: false,
    grupoIds: [],
  }
}

describe('useAuthStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    tokenService.clear()
    vi.mocked(authService.login).mockReset()
    vi.mocked(authService.me).mockReset()
    vi.mocked(authService.logout).mockReset()
    vi.mocked(refreshAccessToken).mockReset()
  })

  it('login guarda el token y carga la autorización', async () => {
    vi.mocked(authService.login).mockResolvedValue({
      accessToken: 'token-123',
      tokenType: 'Bearer',
      expiresIn: 900,
    })
    vi.mocked(authService.me).mockResolvedValue(meResponse())
    const store = useAuthStore()

    await store.login('ana', 'clave-segura')

    expect(tokenService.get()).toBe('token-123')
    expect(store.authorizationLoaded).toBe(true)
    expect(usePermissionsStore().can('VENTAS_VER')).toBe(true)
    expect(useUserStore().username).toBe('ana')
  })

  it('trySilentLogin exitoso restaura la sesión sin pedir credenciales', async () => {
    vi.mocked(refreshAccessToken).mockResolvedValue('token-refrescado')
    vi.mocked(authService.me).mockResolvedValue(meResponse())
    const store = useAuthStore()

    const restaurada = await store.trySilentLogin()

    expect(restaurada).toBe(true)
    expect(store.authorizationLoaded).toBe(true)
    expect(authService.login).not.toHaveBeenCalled()
  })

  it('trySilentLogin fallido limpia el token y no deja autorización cargada', async () => {
    vi.mocked(refreshAccessToken).mockRejectedValue(new Error('sin cookie de refresh'))
    tokenService.set('token-viejo')
    const store = useAuthStore()

    const restaurada = await store.trySilentLogin()

    expect(restaurada).toBe(false)
    expect(tokenService.get()).toBeNull()
    expect(store.authorizationLoaded).toBe(false)
  })

  it('logout limpia token, permisos, usuario y estado de autorización', async () => {
    vi.mocked(authService.login).mockResolvedValue({
      accessToken: 'token-123',
      tokenType: 'Bearer',
      expiresIn: 900,
    })
    vi.mocked(authService.me).mockResolvedValue(meResponse())
    vi.mocked(authService.logout).mockResolvedValue(undefined)
    const store = useAuthStore()
    await store.login('ana', 'clave-segura')

    await store.logout()

    expect(tokenService.get()).toBeNull()
    expect(store.authorizationLoaded).toBe(false)
    expect(usePermissionsStore().can('VENTAS_VER')).toBe(false)
    expect(useUserStore().username).toBeNull()
  })

  it('logout limpia el estado local incluso si la llamada al backend falla', async () => {
    vi.mocked(authService.logout).mockRejectedValue(new Error('red caída'))
    tokenService.set('token-123')
    const store = useAuthStore()

    await expect(store.logout()).rejects.toThrow('red caída')

    expect(tokenService.get()).toBeNull()
    expect(store.authorizationLoaded).toBe(false)
  })
})
