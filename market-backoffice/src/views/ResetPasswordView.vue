<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import { authService } from '@/services/auth/AuthService'
import { ApiClientError } from '@/services/http/ApiClient'

const route = useRoute()
const token = computed(() => (typeof route.query.token === 'string' ? route.query.token : ''))

const nuevaPassword = ref('')
const confirmarPassword = ref('')
const loading = ref(false)
const errorMessage = ref<string | null>(null)
const completado = ref(false)

async function onSubmit() {
  if (loading.value) return

  if (nuevaPassword.value.length < 12) {
    errorMessage.value = 'La contraseña debe tener al menos 12 caracteres.'
    return
  }
  if (nuevaPassword.value !== confirmarPassword.value) {
    errorMessage.value = 'Las contraseñas no coinciden.'
    return
  }

  loading.value = true
  errorMessage.value = null
  try {
    await authService.resetPassword(token.value, nuevaPassword.value)
    completado.value = true
  } catch (error) {
    if (error instanceof ApiClientError && error.status === 400) {
      errorMessage.value = 'El enlace es inválido o ya expiró. Solicita uno nuevo.'
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

    <div v-if="completado" class="relative w-full max-w-sm space-y-5 text-center">
      <div
        class="mx-auto flex h-12 w-12 items-center justify-center rounded-2xl bg-gradient-to-br from-mk-brand to-mk-primary text-sm font-extrabold text-white shadow-lg shadow-mk-brand/30"
      >
        i365
      </div>
      <div class="space-y-1">
        <h1 class="text-xl font-extrabold tracking-tight text-mk-text">Contraseña actualizada</h1>
        <p class="text-sm text-mk-text/60">Ya puedes iniciar sesión con tu nueva contraseña.</p>
      </div>
      <RouterLink
        to="/login"
        class="mk-btn mk-btn-primary inline-block w-full rounded-full bg-mk-primary py-2.5 text-sm font-bold text-white shadow-lg shadow-mk-primary/30"
      >
        Iniciar sesión
      </RouterLink>
    </div>

    <form v-else class="relative w-full max-w-sm space-y-5 text-center" @submit.prevent="onSubmit">
      <div
        class="mx-auto flex h-12 w-12 items-center justify-center rounded-2xl bg-gradient-to-br from-mk-brand to-mk-primary text-sm font-extrabold text-white shadow-lg shadow-mk-brand/30"
      >
        i365
      </div>

      <div class="space-y-1">
        <h1 class="text-xl font-extrabold tracking-tight text-mk-text">Restablecer contraseña</h1>
        <p class="text-sm text-mk-text/60">Ingresa tu nueva contraseña.</p>
      </div>

      <p
        v-if="!token"
        class="rounded-xl bg-mk-danger/10 px-4 py-2 text-left text-sm font-medium text-mk-danger"
        role="alert"
      >
        Este enlace no es válido. Solicita uno nuevo desde "¿Olvidaste tu contraseña?".
      </p>
      <p
        v-else-if="errorMessage"
        class="rounded-xl bg-mk-danger/10 px-4 py-2 text-left text-sm font-medium text-mk-danger"
        role="alert"
      >
        {{ errorMessage }}
      </p>

      <div class="space-y-3 text-left">
        <div class="space-y-1">
          <label for="nueva" class="sr-only">Nueva contraseña</label>
          <input
            id="nueva"
            v-model="nuevaPassword"
            type="password"
            placeholder="Nueva contraseña"
            autocomplete="new-password"
            minlength="12"
            class="mk-input w-full rounded-full border border-mk-border bg-mk-surface px-5 py-2.5 text-sm text-mk-text"
          />
        </div>
        <div class="space-y-1">
          <label for="confirmar" class="sr-only">Confirmar contraseña</label>
          <input
            id="confirmar"
            v-model="confirmarPassword"
            type="password"
            placeholder="Confirmar contraseña"
            autocomplete="new-password"
            minlength="12"
            class="mk-input w-full rounded-full border border-mk-border bg-mk-surface px-5 py-2.5 text-sm text-mk-text"
          />
        </div>
      </div>

      <button
        type="submit"
        :disabled="loading || !token"
        class="mk-btn mk-btn-primary w-full rounded-full bg-mk-primary py-2.5 text-sm font-bold text-white shadow-lg shadow-mk-primary/30 disabled:opacity-50"
      >
        {{ loading ? 'Guardando…' : 'Guardar nueva contraseña' }}
      </button>

      <RouterLink to="/login" class="block text-sm font-medium text-mk-primary hover:underline">
        Volver a iniciar sesión
      </RouterLink>
    </form>
  </div>
</template>
