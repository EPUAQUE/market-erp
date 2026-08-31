import type { NavigationGuardWithThis } from 'vue-router'
import { useAuthStore } from '@/stores/auth.store'
import { usePermissionsStore } from '@/stores/permissions.store'
import { tokenService } from '@/services/http/token.service'

export const authGuard: NavigationGuardWithThis<undefined> = async (to) => {
  const authStore = useAuthStore()

  if (to.meta.requiresAuth === false) {
    return true
  }

  // tokenService.hasToken() consulta directo, nunca vía un getter de Pinia:
  // el access token vive en una variable de módulo plana (no reactiva, a
  // propósito — nunca localStorage, ver token.service.ts), así que un
  // `computed`/getter que lo lea queda cacheado en su primer valor para
  // siempre (Vue no tiene forma de saber que cambió) — causaba que el login
  // nunca redirigiera al dashboard, porque el getter había cacheado `false`
  // desde la primera navegación (sin sesión) del arranque de la app.
  if (!tokenService.hasToken()) {
    // Recarga de página (o primera visita): el access token en memoria ya se
    // perdió, pero la cookie HttpOnly de refresh token puede seguir viva —
    // se intenta restaurar la sesión antes de mandar a login.
    const restaurada = await authStore.trySilentLogin()
    if (!restaurada) {
      return { name: 'login', query: { redirect: to.fullPath } }
    }
  }

  if (!authStore.authorizationLoaded) {
    try {
      await authStore.loadAuthorization()
    } catch {
      return { name: 'login' }
    }
  }

  if (to.meta.permission && !usePermissionsStore().can(to.meta.permission)) {
    return { name: 'forbidden' }
  }

  return true
}
