<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth.store'
import { ApiClientError } from '@/services/http/ApiClient'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const username = ref('')
const password = ref('')
const loading = ref(false)
const errorMessage = ref<string | null>(null)
const rateLimitUntil = ref<number | null>(null)

function isRateLimited(): boolean {
  return rateLimitUntil.value !== null && Date.now() < rateLimitUntil.value
}

async function onSubmit() {
  if (isRateLimited() || loading.value) return

  if (!username.value.trim() || !password.value) {
    errorMessage.value = 'Ingrese usuario y contraseña.'
    return
  }

  loading.value = true
  errorMessage.value = null
  try {
    await authStore.login(username.value, password.value)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    router.push(redirect)
  } catch (error) {
    if (error instanceof ApiClientError) {
      if (error.status === 429 && error.retryAfterMs) {
        rateLimitUntil.value = Date.now() + error.retryAfterMs
        errorMessage.value = 'Demasiados intentos. Intente de nuevo en unos momentos.'
      } else {
        errorMessage.value = 'Usuario o contraseña incorrectos.'
      }
    } else {
      errorMessage.value = 'No se pudo conectar con el servidor.'
    }
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="mk-bg flex min-h-screen items-center justify-center px-4">
    <form
      class="mk-card w-full max-w-sm space-y-4 rounded-lg border border-mk-border bg-mk-surface p-8 shadow-sm"
      @submit.prevent="onSubmit"
    >
      <div class="space-y-1 text-center">
        <h1 class="text-xl font-semibold text-mk-text">Market</h1>
        <p class="text-sm text-mk-text/70">Backoffice administrativo</p>
      </div>

      <div class="space-y-1">
        <label for="username" class="text-sm font-medium text-mk-text">Usuario</label>
        <input
          id="username"
          v-model="username"
          type="text"
          autocomplete="username"
          class="mk-input w-full rounded border border-mk-border bg-transparent px-3 py-2 text-mk-text"
        />
      </div>

      <div class="space-y-1">
        <label for="password" class="text-sm font-medium text-mk-text">Contraseña</label>
        <input
          id="password"
          v-model="password"
          type="password"
          autocomplete="current-password"
          class="mk-input w-full rounded border border-mk-border bg-transparent px-3 py-2 text-mk-text"
        />
      </div>

      <p v-if="errorMessage" class="text-sm text-mk-danger" role="alert">
        {{ errorMessage }}
      </p>

      <button
        type="submit"
        :disabled="loading || isRateLimited()"
        class="mk-btn mk-btn-primary w-full rounded bg-mk-primary py-2 font-medium text-white disabled:opacity-50"
      >
        {{ loading ? 'Ingresando…' : 'Ingresar' }}
      </button>
    </form>
  </div>
</template>
