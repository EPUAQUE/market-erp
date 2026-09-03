<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useCategorias } from '@/composables/useCategorias'
import { useFiltrosTabla, type FiltroColumna } from '@/composables/useFiltrosTabla'
import { usePermissionsStore } from '@/stores/permissions.store'
import EstadoBadge from '@/components/common/EstadoBadge.vue'
import ModalDialog from '@/components/common/ModalDialog.vue'
import type { Categoria } from '@/types/categoria'

const { items, listLoading, listError, saveLoading, saveError, cargar, crear, actualizar, alternarEstado } =
  useCategorias()
const permissions = usePermissionsStore()

const showForm = ref(false)
const editingId = ref<number | null>(null)
const form = ref({ nombre: '' })

const COLUMNAS_FILTRO: FiltroColumna<Categoria>[] = [
  { clave: 'nombre', tipo: 'texto', valor: (c) => c.nombre },
  {
    clave: 'estado',
    tipo: 'opciones',
    valor: (c) => c.estado,
    opciones: [
      { valor: 'ACTIVA', etiqueta: 'Activa' },
      { valor: 'INACTIVA', etiqueta: 'Inactiva' },
    ],
  },
]
const {
  busquedaGlobal,
  filtrosColumna,
  itemsFiltrados: filtered,
  limpiarFiltros,
  hayFiltrosActivos,
} = useFiltrosTabla(items, COLUMNAS_FILTRO)

const modalTitle = computed(() => (editingId.value !== null ? 'Editar categoría' : 'Nueva categoría'))

function abrirCrear() {
  editingId.value = null
  form.value = { nombre: '' }
  showForm.value = true
}

function abrirEditar(categoria: Categoria) {
  editingId.value = categoria.id
  form.value = { nombre: categoria.nombre }
  showForm.value = true
}

async function onSubmit() {
  const ok = editingId.value
    ? await actualizar(editingId.value, form.value.nombre)
    : await crear(form.value.nombre)
  if (ok) showForm.value = false
}

onMounted(cargar)
</script>

<template>
  <div class="mx-auto max-w-3xl space-y-6 p-6">
    <header class="space-y-1">
      <h1 class="text-xl font-semibold">Categorías</h1>
      <p class="text-sm text-mk-text/70">Catálogo de categorías de producto.</p>
    </header>

    <div class="flex items-center justify-between gap-3">
      <div class="flex items-center gap-2">
        <input
          v-model="busquedaGlobal"
          type="search"
          placeholder="Buscar en todas las columnas…"
          class="mk-input w-full max-w-xs rounded border border-mk-border bg-transparent px-3 py-2"
        />
        <button
          v-if="hayFiltrosActivos"
          type="button"
          class="text-sm text-mk-text/60 hover:underline"
          @click="limpiarFiltros"
        >
          Limpiar filtros
        </button>
      </div>
      <button
        v-if="permissions.can('CATEGORIAS_CREAR')"
        type="button"
        class="mk-btn mk-btn-primary rounded bg-mk-primary px-4 py-2 text-sm font-medium text-white"
        @click="abrirCrear()"
      >
        Nueva categoría
      </button>
    </div>

    <ModalDialog v-model="showForm" :title="modalTitle">
      <form class="space-y-3" @submit.prevent="onSubmit">
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
        </div>
        <p v-if="saveError" class="text-sm text-mk-danger" role="alert">{{ saveError }}</p>
        <div class="flex justify-end gap-2">
          <button
            type="button"
            class="mk-btn mk-btn-ghost rounded px-4 py-2 text-sm"
            @click="showForm = false"
          >
            Cancelar
          </button>
          <button
            type="submit"
            :disabled="saveLoading"
            class="mk-btn mk-btn-primary rounded bg-mk-primary px-4 py-2 text-sm font-medium text-white disabled:opacity-50"
          >
            {{ saveLoading ? 'Guardando…' : editingId ? 'Guardar cambios' : 'Crear' }}
          </button>
        </div>
      </form>
    </ModalDialog>

    <div class="mk-scroll-x overflow-x-auto rounded border border-mk-border">
      <table class="w-full text-left text-sm">
        <thead class="border-b border-mk-border bg-mk-surface">
          <tr>
            <th class="px-4 py-2 font-medium">Nombre</th>
            <th class="px-4 py-2 font-medium">Estado</th>
            <th class="px-4 py-2 font-medium">Acciones</th>
          </tr>
          <tr class="border-b border-mk-border bg-mk-surface/50">
            <th class="px-4 py-1.5 font-normal">
              <input
                v-model="filtrosColumna.nombre"
                type="text"
                placeholder="Filtrar…"
                class="mk-input w-full rounded border border-mk-border bg-transparent px-2 py-1 text-xs"
              />
            </th>
            <th class="px-4 py-1.5 font-normal">
              <select
                v-model="filtrosColumna.estado"
                class="mk-input w-full rounded border border-mk-border bg-transparent px-2 py-1 text-xs"
              >
                <option value="">Todos</option>
                <option value="ACTIVA">Activa</option>
                <option value="INACTIVA">Inactiva</option>
              </select>
            </th>
            <th class="px-4 py-1.5"></th>
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
          <tr
            v-for="categoria in filtered"
            :key="categoria.id"
            class="border-b border-mk-border last:border-0"
          >
            <td class="px-4 py-2">{{ categoria.nombre }}</td>
            <td class="px-4 py-2">
              <EstadoBadge
                :variant="categoria.estado === 'ACTIVA' ? 'success' : 'neutral'"
                :label="categoria.estado === 'ACTIVA' ? 'Activa' : 'Inactiva'"
              />
            </td>
            <td class="px-4 py-2">
              <button
                v-if="permissions.can('CATEGORIAS_EDITAR')"
                type="button"
                class="mr-3 text-mk-primary hover:underline"
                @click="abrirEditar(categoria)"
              >
                Editar
              </button>
              <button
                v-if="permissions.can('CATEGORIAS_EDITAR')"
                type="button"
                class="text-mk-primary hover:underline"
                @click="alternarEstado(categoria)"
              >
                {{ categoria.estado === 'ACTIVA' ? 'Desactivar' : 'Activar' }}
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>
