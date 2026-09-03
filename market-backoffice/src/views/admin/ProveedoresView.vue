<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useProveedores } from '@/composables/useProveedores'
import { useFiltrosTabla, type FiltroColumna } from '@/composables/useFiltrosTabla'
import { usePermissionsStore } from '@/stores/permissions.store'
import EstadoBadge from '@/components/common/EstadoBadge.vue'
import ModalDialog from '@/components/common/ModalDialog.vue'
import ActionIcon from '@/components/common/ActionIcon.vue'
import type { Proveedor } from '@/types/proveedor'

const { items, listLoading, listError, saveLoading, saveError, cargar, crear, actualizar, alternarEstado } =
  useProveedores()
const permissions = usePermissionsStore()

const page = ref(1)
const pageSize = ref(10)

const showForm = ref(false)
const editingId = ref<number | null>(null)
const form = ref({ nit: '', nombre: '', direccion: '', telefono: '', correo: '' })

const COLUMNAS_FILTRO: FiltroColumna<Proveedor>[] = [
  { clave: 'nit', tipo: 'texto', valor: (p) => p.nit },
  { clave: 'nombre', tipo: 'texto', valor: (p) => p.nombre },
  {
    clave: 'estado',
    tipo: 'opciones',
    valor: (p) => p.estado,
    opciones: [
      { valor: 'ACTIVO', etiqueta: 'Activo' },
      { valor: 'INACTIVO', etiqueta: 'Inactivo' },
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

const totalPages = computed(() => Math.max(1, Math.ceil(filtered.value.length / pageSize.value)))
const paged = computed(() => {
  const start = (page.value - 1) * pageSize.value
  return filtered.value.slice(start, start + pageSize.value)
})

const modalTitle = computed(() => (editingId.value !== null ? 'Editar proveedor' : 'Nuevo proveedor'))

function abrirCrear() {
  editingId.value = null
  form.value = { nit: '', nombre: '', direccion: '', telefono: '', correo: '' }
  showForm.value = true
}

function abrirEditar(proveedor: Proveedor) {
  editingId.value = proveedor.id
  form.value = {
    nit: proveedor.nit,
    nombre: proveedor.nombre,
    direccion: proveedor.direccion ?? '',
    telefono: proveedor.telefono ?? '',
    correo: proveedor.correo ?? '',
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
  const ok = editingId.value ? await actualizar(editingId.value, datos) : await crear(form.value.nit, datos)
  if (ok) {
    showForm.value = false
  }
}

onMounted(cargar)
</script>

<template>
  <div class="mx-auto max-w-4xl space-y-6 p-6">
    <header class="space-y-1">
      <h1 class="text-xl font-semibold">Proveedores</h1>
      <p class="text-sm text-mk-text/70">Catálogo de proveedores de Inven365.</p>
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
        v-if="permissions.can('PROVEEDORES_CREAR')"
        type="button"
        class="mk-btn mk-btn-primary rounded bg-mk-primary px-4 py-2 text-sm font-medium text-white"
        @click="abrirCrear()"
      >
        Nuevo proveedor
      </button>
    </div>

    <ModalDialog v-model="showForm" :title="modalTitle">
      <form class="space-y-3" @submit.prevent="onSubmit">
        <div class="grid gap-3 sm:grid-cols-2">
          <div class="space-y-1">
            <label class="text-sm font-medium">NIT</label>
            <input
              v-model="form.nit"
              type="text"
              required
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
            <th class="px-4 py-2 font-medium">NIT</th>
            <th class="px-4 py-2 font-medium">Nombre</th>
            <th class="px-4 py-2 font-medium">Estado</th>
            <th class="px-4 py-2 font-medium">Acciones</th>
          </tr>
          <tr class="border-b border-mk-border bg-mk-surface/50">
            <th class="px-4 py-1.5 font-normal">
              <input
                v-model="filtrosColumna.nit"
                type="text"
                placeholder="Filtrar…"
                class="mk-input w-full rounded border border-mk-border bg-transparent px-2 py-1 text-xs"
              />
            </th>
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
                <option value="ACTIVO">Activo</option>
                <option value="INACTIVO">Inactivo</option>
              </select>
            </th>
            <th class="px-4 py-1.5"></th>
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
          <tr v-for="proveedor in paged" :key="proveedor.id" class="border-b border-mk-border last:border-0">
            <td class="px-4 py-2">{{ proveedor.nit }}</td>
            <td class="px-4 py-2">{{ proveedor.nombre }}</td>
            <td class="px-4 py-2">
              <EstadoBadge
                :variant="proveedor.estado === 'ACTIVO' ? 'success' : 'neutral'"
                :label="proveedor.estado === 'ACTIVO' ? 'Activo' : 'Inactivo'"
              />
            </td>
            <td class="px-4 py-2">
              <div class="mk-row-actions">
                <button
                  v-if="permissions.can('PROVEEDORES_EDITAR')"
                  type="button"
                  class="mk-row-btn"
                  title="Editar"
                  @click="abrirEditar(proveedor)"
                >
                  <ActionIcon name="edit" />
                </button>
                <button
                  v-if="permissions.can('PROVEEDORES_EDITAR')"
                  type="button"
                  class="mk-row-btn"
                  :class="proveedor.estado === 'ACTIVO' ? 'mk-row-btn-danger' : 'mk-row-btn-success'"
                  :title="proveedor.estado === 'ACTIVO' ? 'Desactivar' : 'Activar'"
                  @click="alternarEstado(proveedor)"
                >
                  <ActionIcon name="power" />
                </button>
              </div>
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
        <button type="button" :disabled="page <= 1" class="disabled:opacity-40" @click="page--">
          Anterior
        </button>
        <span>Página {{ page }} de {{ totalPages }}</span>
        <button type="button" :disabled="page >= totalPages" class="disabled:opacity-40" @click="page++">
          Siguiente
        </button>
      </div>
    </div>
  </div>
</template>
