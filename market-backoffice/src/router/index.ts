import { createRouter, createWebHistory } from 'vue-router'
import { routes } from './routes'
import { authGuard } from './guards'
import { setOnUnauthorized } from '@/services/http/ApiClient'
import { tokenService } from '@/services/http/token.service'
import { useAuthStore } from '@/stores/auth.store'
import { usePermissionsStore } from '@/stores/permissions.store'
import { useUserStore } from '@/stores/user.store'

export const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach(authGuard)

setOnUnauthorized(() => {
  tokenService.clear()
  usePermissionsStore().clear()
  useUserStore().clear()
  useAuthStore().authorizationLoaded = false
  router.push({ name: 'login', query: { sessionExpired: '1' } })
})
