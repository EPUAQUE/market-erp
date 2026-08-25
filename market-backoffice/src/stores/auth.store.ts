import { defineStore } from 'pinia'
import { authService } from '@/services/auth/AuthService'
import { tokenService } from '@/services/http/token.service'
import { usePermissionsStore } from './permissions.store'
import { useUserStore } from './user.store'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    authorizationLoaded: false,
  }),
  getters: {
    isAuthenticated(): boolean {
      return tokenService.hasToken()
    },
  },
  actions: {
    async login(username: string, password: string) {
      const response = await authService.login(username, password)
      tokenService.set(response.accessToken)
      await this.loadAuthorization()
    },

    async loadAuthorization() {
      const me = await authService.me()
      useUserStore().setUsername(me.username)
      usePermissionsStore().hydrate(me.permisos, me.tiendaIds, me.alcanceGlobal)
      this.authorizationLoaded = true
    },

    async logout() {
      try {
        await authService.logout()
      } finally {
        tokenService.clear()
        usePermissionsStore().clear()
        useUserStore().clear()
        this.authorizationLoaded = false
      }
    },
  },
})
