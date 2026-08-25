<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useUsuarios } from '@/composables/useUsuarios'
import { usePermissionsStore } from '@/stores/permissions.store'
import EstadoBadge from '@/components/common/EstadoBadge.vue'
import type { EstadoUsuario } from '@/types/usuario'

const ESTADO_VARIANT: Record<EstadoUsuario, 'success' | 'neutral' | 'danger'> = {
  ACTIVO: 'success',
  INACTIVO: 'neutral',
  BLOQUEADO: 'danger',
}
const ESTADO_LABEL: Record<EstadoUsuario, string> = {
  ACTIVO: 'Activo',
  INACTIVO: 'Inactivo',
  BLOQUEADO: 'Bloqueado',
}

const { items, listLoading, listError, createLoading, createError, cargar, crear } = useUsuarios()
const permissions = usePermissionsStore()

const search = ref('')
const page = ref(1)
const pageSize = ref(10)

const showCreateForm = ref(false)
const newUsername = ref('')
const newPassword = ref('')

const filtered = computed(() => {
  const term = search.value.trim().toLowerCase()
  if (!term) return items.value
  return items.value.filter((usuario) => usuario.username.toLowerCase().includes(term))
})

const totalPages = computed(() => Math.max(1, Math.ceil(filtered.value.length / pageSize.value)))

const paged = computed(() => {
  const start = (page.value - 1) * pageSize.value
  return filtered.value.slice(start, start + pageSize.value)
})

async function onCrear() {
  const ok = await crear(newUsername.value, newPassword.value)
  if (ok) {
    newUsername.value = ''
    newPassword.value = ''
    showCreateForm.value = false
  }
}

onMounted(cargar)
</script>

<template>
  <div class="mx-auto max-w-4xl space-y-6 p-6 text-mk-text">
    <header class="space-y-1">
      <h1 class="text-xl font-semibold">Usuarios</h1>
      <p class="text-sm text-mk-text/70">Cuentas del personal administrativo y de tienda.</p>
    </header>

    <div class="flex items-center justify-between gap-3">
      <input
        v-model="search"
        type="search"
        placeholder="Buscar por usuario…"
        class="mk-input w-full max-w-xs rounded border border-mk-border bg-transparent px-3 py-2"
      />
      <button
        v-if="permissions.can('USUARIOS_CREAR')"
        type="button"
        class="mk-btn mk-btn-primary rounded bg-mk-primary px-4 py-2 text-sm font-medium text-white"
        @click="showCreateForm = !showCreateForm"
      >
        {{ showCreateForm ? 'Cancelar' : 'Nuevo usuario' }}
      </button>
    </div>

    <form
      v-if="showCreateForm"
      class="space-y-3 rounded border border-mk-border p-4"
      @submit.prevent="onCrear"
    >
      <div class="grid gap-3 sm:grid-cols-2">
        <div class="space-y-1">
          <label class="text-sm font-medium">Usuario</label>
          <input
            v-model="newUsername"
            type="text"
            required
            class="mk-input w-full rounded border border-mk-border bg-transparent px-3 py-2"
          />
        </div>
        <div class="space-y-1">
          <label class="text-sm font-medium">Contraseña</label>
          <input
            v-model="newPassword"
            type="password"
            required
            minlength="12"
            class="mk-input w-full rounded border border-mk-border bg-transparent px-3 py-2"
          />
        </div>
      </div>
      <p v-if="createError" class="text-sm text-mk-danger" role="alert">{{ createError }}</p>
      <button
        type="submit"
        :disabled="createLoading"
        class="mk-btn mk-btn-primary rounded bg-mk-primary px-4 py-2 text-sm font-medium text-white disabled:opacity-50"
      >
        {{ createLoading ? 'Creando…' : 'Crear' }}
      </button>
    </form>

    <div class="mk-scroll-x overflow-x-auto rounded border border-mk-border">
      <table class="w-full text-left text-sm">
        <thead class="border-b border-mk-border bg-mk-surface">
          <tr>
            <th class="px-4 py-2 font-medium">Usuario</th>
            <th class="px-4 py-2 font-medium">Estado</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="listLoading">
            <td colspan="2" class="px-4 py-6 text-center text-mk-text/60">Cargando…</td>
          </tr>
          <tr v-else-if="listError">
            <td colspan="2" class="px-4 py-6 text-center text-mk-danger">{{ listError }}</td>
          </tr>
          <tr v-else-if="paged.length === 0">
            <td colspan="2" class="px-4 py-6 text-center text-mk-text/60">Sin resultados.</td>
          </tr>
          <tr v-for="usuario in paged" :key="usuario.id" class="border-b border-mk-border last:border-0">
            <td class="px-4 py-2">{{ usuario.username }}</td>
            <td class="px-4 py-2">
              <EstadoBadge :variant="ESTADO_VARIANT[usuario.estado]" :label="ESTADO_LABEL[usuario.estado]" />
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="flex items-center justify-between text-sm text-mk-text/70">
      <select v-model.number="pageSize" class="rounded border border-mk-border bg-transparent px-2 py-1">
        <option :value="10">10 / página</option>
        <option :value="25">25 / página</option>
        <option :value="50">50 / página</option>
        <option :value="100">100 / página</option>
      </select>
      <div class="flex items-center gap-2">
        <button type="button" :disabled="page <= 1" class="disabled:opacity-40" @click="page--">Anterior</button>
        <span>Página {{ page }} de {{ totalPages }}</span>
        <button type="button" :disabled="page >= totalPages" class="disabled:opacity-40" @click="page++">
          Siguiente
        </button>
      </div>
    </div>
  </div>
</template>
