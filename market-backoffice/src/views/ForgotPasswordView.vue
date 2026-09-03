<script setup lang="ts">
import { ref } from 'vue'
import { authService } from '@/services/auth/AuthService'
import { ApiClientError } from '@/services/http/ApiClient'

const username = ref('')
const loading = ref(false)
const errorMessage = ref<string | null>(null)
const enviado = ref(false)

async function onSubmit() {
  if (loading.value || !username.value.trim()) return

  loading.value = true
  errorMessage.value = null
  try {
    await authService.forgotPassword(username.value.trim())
    enviado.value = true
  } catch (error) {
    if (error instanceof ApiClientError && error.status === 429 && error.retryAfterMs) {
      errorMessage.value = 'Demasiados intentos. Intente de nuevo en unos momentos.'
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

    <div v-if="enviado" class="relative w-full max-w-sm space-y-5 text-center">
      <div
        class="mx-auto flex h-12 w-12 items-center justify-center rounded-2xl bg-gradient-to-br from-mk-brand to-mk-primary text-sm font-extrabold text-white shadow-lg shadow-mk-brand/30"
      >
        i365
      </div>
      <div class="space-y-1">
        <h1 class="text-xl font-extrabold tracking-tight text-mk-text">Revisa tu correo</h1>
        <p class="text-sm text-mk-text/60">
          Si el usuario existe y tiene un correo registrado, te enviamos un enlace para restablecer tu
          contraseña. Expira en 30 minutos.
        </p>
      </div>
      <RouterLink
        to="/login"
        class="mk-btn mk-btn-primary inline-block w-full rounded-full bg-mk-primary py-2.5 text-sm font-bold text-white shadow-lg shadow-mk-primary/30"
      >
        Volver a iniciar sesión
      </RouterLink>
    </div>

    <form v-else class="relative w-full max-w-sm space-y-5 text-center" @submit.prevent="onSubmit">
      <div
        class="mx-auto flex h-12 w-12 items-center justify-center rounded-2xl bg-gradient-to-br from-mk-brand to-mk-primary text-sm font-extrabold text-white shadow-lg shadow-mk-brand/30"
      >
        i365
      </div>

      <div class="space-y-1">
        <h1 class="text-xl font-extrabold tracking-tight text-mk-text">¿Olvidaste tu contraseña?</h1>
        <p class="text-sm text-mk-text/60">
          Ingresa tu usuario y te enviaremos un enlace para restablecerla.
        </p>
      </div>

      <p
        v-if="errorMessage"
        class="rounded-xl bg-mk-danger/10 px-4 py-2 text-left text-sm font-medium text-mk-danger"
        role="alert"
      >
        {{ errorMessage }}
      </p>

      <div class="space-y-1 text-left">
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

      <button
        type="submit"
        :disabled="loading"
        class="mk-btn mk-btn-primary w-full rounded-full bg-mk-primary py-2.5 text-sm font-bold text-white shadow-lg shadow-mk-primary/30 disabled:opacity-50"
      >
        {{ loading ? 'Enviando…' : 'Enviar enlace' }}
      </button>

      <RouterLink to="/login" class="block text-sm font-medium text-mk-primary hover:underline">
        Volver a iniciar sesión
      </RouterLink>
    </form>
  </div>
</template>
