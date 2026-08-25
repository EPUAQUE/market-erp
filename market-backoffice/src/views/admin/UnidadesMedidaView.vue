<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useUnidadesMedida } from '@/composables/useUnidadesMedida'
import { usePermissionsStore } from '@/stores/permissions.store'
import type { UnidadMedida } from '@/types/unidadMedida'

const { items, listLoading, listError, saveLoading, saveError, cargar, crear, actualizar } = useUnidadesMedida()
const permissions = usePermissionsStore()

const search = ref('')
const showForm = ref(false)
const editingId = ref<number | null>(null)
const form = ref({ nombre: '', abreviacion: '' })

const filtered = computed(() => {
  const term = search.value.trim().toLowerCase()
  if (!term) return items.value
  return items.value.filter(
    (u) => u.nombre.toLowerCase().includes(term) || u.abreviacion.toLowerCase().includes(term),
  )
})

function abrirCrear() {
  editingId.value = null
  form.value = { nombre: '', abreviacion: '' }
  showForm.value = true
}

function abrirEditar(unidad: UnidadMedida) {
  editingId.value = unidad.id
  form.value = { nombre: unidad.nombre, abreviacion: unidad.abreviacion }
  showForm.value = true
}

async function onSubmit() {
  const ok = editingId.value
    ? await actualizar(editingId.value, form.value.nombre, form.value.abreviacion)
    : await crear(form.value.nombre, form.value.abreviacion)
  if (ok) showForm.value = false
}

onMounted(cargar)
</script>

<template>
  <div class="mx-auto max-w-3xl space-y-6 p-6">
    <header class="space-y-1">
      <h1 class="text-xl font-semibold">Unidades de Medida</h1>
      <p class="text-sm text-mk-text/70">Catálogo de unidades para productos (kg, litro, unidad…).</p>
    </header>

    <div class="flex items-center justify-between gap-3">
      <input
        v-model="search"
        type="search"
        placeholder="Buscar por nombre o abreviación…"
        class="mk-input w-full max-w-xs rounded border border-mk-border bg-transparent px-3 py-2"
      />
      <button
        v-if="permissions.can('UNIDADES_MEDIDA_CREAR')"
        type="button"
        class="mk-btn mk-btn-primary rounded bg-mk-primary px-4 py-2 text-sm font-medium text-white"
        @click="showForm ? (showForm = false) : abrirCrear()"
      >
        {{ showForm ? 'Cancelar' : 'Nueva unidad' }}
      </button>
    </div>

    <form v-if="showForm" class="space-y-3 rounded border border-mk-border p-4" @submit.prevent="onSubmit">
      <div class="grid gap-3 sm:grid-cols-2">
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
          <label class="text-sm font-medium">Abreviación</label>
          <input
            v-model="form.abreviacion"
            type="text"
            required
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
            <th class="px-4 py-2 font-medium">Nombre</th>
            <th class="px-4 py-2 font-medium">Abreviación</th>
            <th class="px-4 py-2 font-medium">Acciones</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="listLoading">
            <td colspan="3" class="px-4 py-6 text-center text-mk-text/60">Cargando…</td>
          </tr>
          <tr v-else-if="listError">
            <td colspan="3" class="px-4 py-6 text-center text-mk-danger">{{ listError }}</td>
          </tr>
          <tr v-else-if="filtered.length === 0">
            <td colspan="3" class="px-4 py-6 text-center text-mk-text/60">Sin resultados.</td>
          </tr>
          <tr v-for="unidad in filtered" :key="unidad.id" class="border-b border-mk-border last:border-0">
            <td class="px-4 py-2">{{ unidad.nombre }}</td>
            <td class="px-4 py-2">{{ unidad.abreviacion }}</td>
            <td class="px-4 py-2">
              <button
                v-if="permissions.can('UNIDADES_MEDIDA_EDITAR')"
                type="button"
                class="text-mk-primary hover:underline"
                @click="abrirEditar(unidad)"
              >
                Editar
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>
