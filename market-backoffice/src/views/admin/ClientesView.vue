<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useClientes } from '@/composables/useClientes'
import { usePermissionsStore } from '@/stores/permissions.store'
import EstadoBadge from '@/components/common/EstadoBadge.vue'
import type { Cliente } from '@/types/cliente'

const { items, listLoading, listError, saveLoading, saveError, cargar, crear, actualizar, alternarEstado } =
  useClientes()
const permissions = usePermissionsStore()

const search = ref('')
const page = ref(1)
const pageSize = ref(10)

const showForm = ref(false)
const editingId = ref<number | null>(null)
const form = ref({ nit: '', nombre: '', direccion: '', telefono: '', correo: '' })

const filtered = computed(() => {
  const term = search.value.trim().toLowerCase()
  if (!term) return items.value
  return items.value.filter(
    (c) => c.nombre.toLowerCase().includes(term) || (c.nit ?? '').toLowerCase().includes(term),
  )
})

const totalPages = computed(() => Math.max(1, Math.ceil(filtered.value.length / pageSize.value)))
const paged = computed(() => {
  const start = (page.value - 1) * pageSize.value
  return filtered.value.slice(start, start + pageSize.value)
})

function abrirCrear() {
  editingId.value = null
  form.value = { nit: '', nombre: '', direccion: '', telefono: '', correo: '' }
  showForm.value = true
}

function abrirEditar(cliente: Cliente) {
  editingId.value = cliente.id
  form.value = {
    nit: cliente.nit ?? '',
    nombre: cliente.nombre,
    direccion: cliente.direccion ?? '',
    telefono: cliente.telefono ?? '',
    correo: cliente.correo ?? '',
  }
  showForm.value = true
}

async function onSubmit() {
  const datos = {
    nombre: form.value.nombre,
    direccion: form.value.direccion || undefined,
    telefono: form.value.telefono || undefined,
    correo: form.value.correo || undefined,
  }
  const ok = editingId.value
    ? await actualizar(editingId.value, datos)
    : await crear(form.value.nit || undefined, datos)
  if (ok) {
    showForm.value = false
  }
}

onMounted(cargar)
</script>

<template>
  <div class="mx-auto max-w-4xl space-y-6 p-6">
    <header class="space-y-1">
      <h1 class="text-xl font-semibold">Clientes</h1>
      <p class="text-sm text-mk-text/70">Catálogo de clientes de Market.</p>
    </header>

    <div class="flex items-center justify-between gap-3">
      <input
        v-model="search"
        type="search"
        placeholder="Buscar por NIT o nombre…"
        class="mk-input w-full max-w-xs rounded border border-mk-border bg-transparent px-3 py-2"
      />
      <button
        v-if="permissions.can('CLIENTES_CREAR')"
        type="button"
        class="mk-btn mk-btn-primary rounded bg-mk-primary px-4 py-2 text-sm font-medium text-white"
        @click="showForm ? (showForm = false) : abrirCrear()"
      >
        {{ showForm ? 'Cancelar' : 'Nuevo cliente' }}
      </button>
    </div>

    <form v-if="showForm" class="space-y-3 rounded border border-mk-border p-4" @submit.prevent="onSubmit">
      <div class="grid gap-3 sm:grid-cols-2">
        <div class="space-y-1">
          <label class="text-sm font-medium">NIT</label>
          <input
            v-model="form.nit"
            type="text"
            placeholder="Opcional — vacío para Consumidor Final"
            :disabled="editingId !== null"
            class="mk-input w-full rounded border border-mk-border bg-transparent px-3 py-2 disabled:opacity-50"
          />
        </div>
        <div class="space-y-1">
          <label class="text-sm font-medium">Nombre</label>
          <input
            v-model="form.nombre"
            type="text"
            required
            class="mk-input w-full rounded border border-mk-border bg-transparent px-3 py-2"
          />
        </div>
        <div class="space-y-1">
          <label class="text-sm font-medium">Dirección</label>
          <input
            v-model="form.direccion"
            type="text"
            class="mk-input w-full rounded border border-mk-border bg-transparent px-3 py-2"
          />
        </div>
        <div class="space-y-1">
          <label class="text-sm font-medium">Teléfono</label>
          <input
            v-model="form.telefono"
            type="text"
            class="mk-input w-full rounded border border-mk-border bg-transparent px-3 py-2"
          />
        </div>
        <div class="space-y-1 sm:col-span-2">
          <label class="text-sm font-medium">Correo</label>
          <input
            v-model="form.correo"
            type="email"
            class="mk-input w-full rounded border border-mk-border bg-transparent px-3 py-2"
          />
        </div>
      </div>
      <p v-if="saveError" class="text-sm text-mk-danger" role="alert">{{ saveError }}</p>
      <button
        type="submit"
        :disabled="saveLoading"
        class="mk-btn mk-btn-primary rounded bg-mk-primary px-4 py-2 text-sm font-medium text-white disabled:opacity-50"
      >
        {{ saveLoading ? 'Guardando…' : editingId ? 'Guardar cambios' : 'Crear' }}
      </button>
    </form>

    <div class="mk-scroll-x overflow-x-auto rounded border border-mk-border">
      <table class="w-full text-left text-sm">
        <thead class="border-b border-mk-border bg-mk-surface">
          <tr>
            <th class="px-4 py-2 font-medium">NIT</th>
            <th class="px-4 py-2 font-medium">Nombre</th>
            <th class="px-4 py-2 font-medium">Estado</th>
            <th class="px-4 py-2 font-medium">Acciones</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="listLoading">
            <td colspan="4" class="px-4 py-6 text-center text-mk-text/60">Cargando…</td>
          </tr>
          <tr v-else-if="listError">
            <td colspan="4" class="px-4 py-6 text-center text-mk-danger">{{ listError }}</td>
          </tr>
          <tr v-else-if="paged.length === 0">
            <td colspan="4" class="px-4 py-6 text-center text-mk-text/60">Sin resultados.</td>
          </tr>
          <tr v-for="cliente in paged" :key="cliente.id" class="border-b border-mk-border last:border-0">
            <td class="px-4 py-2">{{ cliente.nit ?? '—' }}</td>
            <td class="px-4 py-2">{{ cliente.nombre }}</td>
            <td class="px-4 py-2">
              <EstadoBadge
                :variant="cliente.estado === 'ACTIVO' ? 'success' : 'neutral'"
                :label="cliente.estado === 'ACTIVO' ? 'Activo' : 'Inactivo'"
              />
            </td>
            <td class="px-4 py-2">
              <button
                v-if="permissions.can('CLIENTES_EDITAR')"
                type="button"
                class="mr-3 text-mk-primary hover:underline"
                @click="abrirEditar(cliente)"
              >
                Editar
              </button>
              <button
                v-if="permissions.can('CLIENTES_EDITAR')"
                type="button"
                class="text-mk-primary hover:underline"
                @click="alternarEstado(cliente)"
              >
                {{ cliente.estado === 'ACTIVO' ? 'Desactivar' : 'Activar' }}
              </button>
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
