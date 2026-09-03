<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useMarcas } from '@/composables/useMarcas'
import { useFiltrosTabla, type FiltroColumna } from '@/composables/useFiltrosTabla'
import { usePermissionsStore } from '@/stores/permissions.store'
import type { Marca } from '@/types/marca'

const { items, listLoading, listError, saveLoading, saveError, cargar, crear, actualizar } = useMarcas()
const permissions = usePermissionsStore()

const showForm = ref(false)
const editingId = ref<number | null>(null)
const nombre = ref('')

const COLUMNAS_FILTRO: FiltroColumna<Marca>[] = [{ clave: 'nombre', tipo: 'texto', valor: (m) => m.nombre }]
const {
  busquedaGlobal,
  filtrosColumna,
  itemsFiltrados: filtered,
  limpiarFiltros,
  hayFiltrosActivos,
} = useFiltrosTabla(items, COLUMNAS_FILTRO)

function abrirCrear() {
  editingId.value = null
  nombre.value = ''
  showForm.value = true
}

function abrirEditar(marca: Marca) {
  editingId.value = marca.id
  nombre.value = marca.nombre
  showForm.value = true
}

async function onSubmit() {
  const ok = editingId.value ? await actualizar(editingId.value, nombre.value) : await crear(nombre.value)
  if (ok) showForm.value = false
}

onMounted(cargar)
</script>

<template>
  <div class="mx-auto max-w-3xl space-y-6 p-6">
    <header class="space-y-1">
      <h1 class="text-xl font-semibold">Marcas</h1>
      <p class="text-sm text-mk-text/70">Catálogo de marcas de producto.</p>
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
        v-if="permissions.can('MARCAS_CREAR')"
        type="button"
        class="mk-btn mk-btn-primary rounded bg-mk-primary px-4 py-2 text-sm font-medium text-white"
        @click="showForm ? (showForm = false) : abrirCrear()"
      >
        {{ showForm ? 'Cancelar' : 'Nueva marca' }}
      </button>
    </div>

    <form v-if="showForm" class="space-y-3 rounded border border-mk-border p-4" @submit.prevent="onSubmit">
      <div class="space-y-1">
        <label class="text-sm font-medium">Nombre</label>
        <input
          v-model="nombre"
          type="text"
          required
          class="mk-input w-full max-w-sm rounded border border-mk-border bg-transparent px-3 py-2"
        />
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
            <th class="px-4 py-1.5"></th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="listLoading">
            <td colspan="2" class="px-4 py-6 text-center text-mk-text/60">Cargando…</td>
          </tr>
          <tr v-else-if="listError">
            <td colspan="2" class="px-4 py-6 text-center text-mk-danger">{{ listError }}</td>
          </tr>
          <tr v-else-if="filtered.length === 0">
            <td colspan="2" class="px-4 py-6 text-center text-mk-text/60">Sin resultados.</td>
          </tr>
          <tr v-for="marca in filtered" :key="marca.id" class="border-b border-mk-border last:border-0">
            <td class="px-4 py-2">{{ marca.nombre }}</td>
            <td class="px-4 py-2">
              <button
                v-if="permissions.can('MARCAS_EDITAR')"
                type="button"
                class="text-mk-primary hover:underline"
                @click="abrirEditar(marca)"
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
