<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth.store'
import { ApiClientError } from '@/services/http/ApiClient'

const USUARIO_RECORDADO_KEY = 'inven365-usuario-recordado'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const username = ref('')
const password = ref('')
const recordarme = ref(false)
const loading = ref(false)
const errorMessage = ref<string | null>(null)
const rateLimitUntil = ref<number | null>(null)

onMounted(() => {
  const recordado = localStorage.getItem(USUARIO_RECORDADO_KEY)
  if (recordado) {
    username.value = recordado
    recordarme.value = true
  }
})

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
    if (recordarme.value) {
      localStorage.setItem(USUARIO_RECORDADO_KEY, username.value.trim())
    } else {
      localStorage.removeItem(USUARIO_RECORDADO_KEY)
    }
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
  <div class="mk-bg relative flex min-h-screen items-center justify-center overflow-hidden px-4">
    <div
      class="pointer-events-none absolute -top-40 left-1/2 h-[420px] w-[420px] -translate-x-1/2 rounded-full bg-mk-primary/15 blur-3xl"
      aria-hidden="true"
    ></div>

    <form class="relative w-full max-w-sm space-y-5 text-center" @submit.prevent="onSubmit">
      <div
        class="mx-auto flex h-12 w-12 items-center justify-center rounded-2xl bg-gradient-to-br from-mk-brand to-mk-primary text-sm font-extrabold text-white shadow-lg shadow-mk-brand/30"
      >
        i365
      </div>

      <div class="space-y-1">
        <h1 class="text-xl font-extrabold tracking-tight text-mk-text">Bienvenido de nuevo</h1>
        <p class="text-sm text-mk-text/60">Ingresa tus datos para continuar</p>
      </div>

      <p
        v-if="errorMessage"
        class="rounded-xl bg-mk-danger/10 px-4 py-2 text-left text-sm font-medium text-mk-danger"
        role="alert"
      >
        {{ errorMessage }}
      </p>

      <div class="space-y-3 text-left">
        <div class="space-y-1">
          <label for="username" class="sr-only">Usuario</label>
          <input
            id="username"
            v-model="username"
            type="text"
            placeholder="Usuario"
            autocomplete="username"
            class="mk-input w-full rounded-full border border-mk-border bg-mk-surface px-5 py-2.5 text-sm text-mk-text"
          />
        </div>

        <div class="space-y-1">
          <label for="password" class="sr-only">Contraseña</label>
          <input
            id="password"
            v-model="password"
            type="password"
            placeholder="Contraseña"
            autocomplete="current-password"
            class="mk-input w-full rounded-full border border-mk-border bg-mk-surface px-5 py-2.5 text-sm text-mk-text"
          />
        </div>
      </div>

      <div class="flex items-center justify-between text-sm">
        <label class="flex items-center gap-2 text-mk-text/70">
          <input v-model="recordarme" type="checkbox" class="rounded border-mk-border" />
          Recordarme
        </label>
        <RouterLink to="/olvide-password" class="font-medium text-mk-primary hover:underline">
          ¿Olvidaste tu contraseña?
        </RouterLink>
      </div>

      <button
        type="submit"
        :disabled="loading || isRateLimited()"
        class="mk-btn mk-btn-primary w-full rounded-full bg-mk-primary py-2.5 text-sm font-bold text-white shadow-lg shadow-mk-primary/30 disabled:opacity-50"
      >
        {{ loading ? 'Ingresando…' : 'Ingresar' }}
      </button>

      <p class="text-xs text-mk-text/50">© 2026 Inven365 · Backoffice administrativo</p>
    </form>
  </div>
</template>
