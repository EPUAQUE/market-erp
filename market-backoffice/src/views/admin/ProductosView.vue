<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useProductos } from '@/composables/useProductos'
import { usePermissionsStore } from '@/stores/permissions.store'
import { categoriasService } from '@/services/categorias.service'
import { marcasService } from '@/services/marcas.service'
import { unidadesMedidaService } from '@/services/unidadesMedida.service'
import EstadoBadge from '@/components/common/EstadoBadge.vue'
import type { Producto } from '@/types/producto'
import type { Categoria } from '@/types/categoria'
import type { Marca } from '@/types/marca'
import type { UnidadMedida } from '@/types/unidadMedida'

const {
  items,
  listLoading,
  listError,
  saveLoading,
  saveError,
  pagina,
  tamano,
  totalElementos,
  totalPaginas,
  cargar,
  crear,
  actualizar,
  alternarEstado,
} = useProductos()

// El composable arranca con un tamano grande (pensado para consumidores tipo
// dropdown, ver useProductos.ts) — esta es la vista de administración real,
// así que empieza con paginación normal 10/página.
tamano.value = 10
const permissions = usePermissionsStore()

const categorias = ref<Categoria[]>([])
const marcas = ref<Marca[]>([])
const unidades = ref<UnidadMedida[]>([])

const search = ref('')
const showForm = ref(false)
const editingId = ref<number | null>(null)
const form = ref({
  codigoInterno: '',
  codigoBarras: '',
  nombre: '',
  descripcion: '',
  categoriaId: '' as number | '',
  marcaId: '' as number | '',
  unidadMedidaId: '' as number | '',
  imagenUrl: '',
})

const filtered = computed(() => {
  const term = search.value.trim().toLowerCase()
  if (!term) return items.value
  return items.value.filter(
    (p) => p.nombre.toLowerCase().includes(term) || p.codigoInterno.toLowerCase().includes(term),
  )
})

function nombreCategoria(id: number) {
  return categorias.value.find((c) => c.id === id)?.nombre ?? id
}
function nombreMarca(id: number) {
  return marcas.value.find((m) => m.id === id)?.nombre ?? id
}
function nombreUnidad(id: number) {
  return unidades.value.find((u) => u.id === id)?.abreviacion ?? id
}

function abrirCrear() {
  editingId.value = null
  form.value = {
    codigoInterno: '',
    codigoBarras: '',
    nombre: '',
    descripcion: '',
    categoriaId: '',
    marcaId: '',
    unidadMedidaId: '',
    imagenUrl: '',
  }
  showForm.value = true
}

function abrirEditar(producto: Producto) {
  editingId.value = producto.id
  form.value = {
    codigoInterno: producto.codigoInterno,
    codigoBarras: producto.codigoBarras ?? '',
    nombre: producto.nombre,
    descripcion: producto.descripcion ?? '',
    categoriaId: producto.categoriaId,
    marcaId: producto.marcaId,
    unidadMedidaId: producto.unidadMedidaId,
    imagenUrl: producto.imagenUrl ?? '',
  }
  showForm.value = true
}

async function onSubmit() {
  if (!form.value.categoriaId || !form.value.marcaId || !form.value.unidadMedidaId) return
  const datos = {
    codigoBarras: form.value.codigoBarras || undefined,
    nombre: form.value.nombre,
    descripcion: form.value.descripcion || undefined,
    categoriaId: Number(form.value.categoriaId),
    marcaId: Number(form.value.marcaId),
    unidadMedidaId: Number(form.value.unidadMedidaId),
    imagenUrl: form.value.imagenUrl || undefined,
  }
  const ok = editingId.value
    ? await actualizar(editingId.value, datos)
    : await crear(form.value.codigoInterno, datos)
  if (ok) showForm.value = false
}

watch(tamano, () => {
  pagina.value = 1
})
watch([pagina, tamano], () => {
  cargar()
})

onMounted(async () => {
  await Promise.all([
    cargar(),
    categoriasService.listar().then((r) => (categorias.value = r)),
    marcasService.listar().then((r) => (marcas.value = r)),
    unidadesMedidaService.listar().then((r) => (unidades.value = r)),
  ])
})
</script>

<template>
  <div class="mx-auto max-w-5xl space-y-6 p-6">
    <header class="space-y-1">
      <h1 class="text-xl font-semibold">Productos</h1>
      <p class="text-sm text-mk-text/70">Catálogo global de productos.</p>
    </header>

    <div class="flex items-center justify-between gap-3">
      <input
        v-model="search"
        type="search"
        placeholder="Buscar por código o nombre…"
        class="mk-input w-full max-w-xs rounded border border-mk-border bg-transparent px-3 py-2"
      />
      <button
        v-if="permissions.can('PRODUCTOS_CREAR')"
        type="button"
        class="mk-btn mk-btn-primary rounded bg-mk-primary px-4 py-2 text-sm font-medium text-white"
        @click="showForm ? (showForm = false) : abrirCrear()"
      >
        {{ showForm ? 'Cancelar' : 'Nuevo producto' }}
      </button>
    </div>

    <form v-if="showForm" class="space-y-3 rounded border border-mk-border p-4" @submit.prevent="onSubmit">
      <div class="grid gap-3 sm:grid-cols-2">
        <div class="space-y-1">
          <label class="text-sm font-medium">Código interno</label>
          <input
            v-model="form.codigoInterno"
            type="text"
            required
            :disabled="editingId !== null"
            class="mk-input w-full rounded border border-mk-border bg-transparent px-3 py-2 disabled:opacity-50"
          />
        </div>
        <div class="space-y-1">
          <label class="text-sm font-medium">Código de barras</label>
          <input
            v-model="form.codigoBarras"
            type="text"
            class="mk-input w-full rounded border border-mk-border bg-transparent px-3 py-2"
          />
        </div>
        <div class="space-y-1 sm:col-span-2">
          <label class="text-sm font-medium">Nombre</label>
          <input
            v-model="form.nombre"
            type="text"
            required
            class="mk-input w-full rounded border border-mk-border bg-transparent px-3 py-2"
          />
        </div>
        <div class="space-y-1 sm:col-span-2">
          <label class="text-sm font-medium">Descripción</label>
          <input
            v-model="form.descripcion"
            type="text"
            class="mk-input w-full rounded border border-mk-border bg-transparent px-3 py-2"
          />
        </div>
        <div class="space-y-1">
          <label class="text-sm font-medium">Categoría</label>
          <select
            v-model="form.categoriaId"
            required
            class="mk-input w-full rounded border border-mk-border bg-transparent px-3 py-2"
          >
            <option value="" disabled>Seleccione…</option>
            <option v-for="c in categorias" :key="c.id" :value="c.id">{{ c.nombre }}</option>
          </select>
        </div>
        <div class="space-y-1">
          <label class="text-sm font-medium">Marca</label>
          <select
            v-model="form.marcaId"
            required
            class="mk-input w-full rounded border border-mk-border bg-transparent px-3 py-2"
          >
            <option value="" disabled>Seleccione…</option>
            <option v-for="m in marcas" :key="m.id" :value="m.id">{{ m.nombre }}</option>
          </select>
        </div>
        <div class="space-y-1">
          <label class="text-sm font-medium">Unidad de medida</label>
          <select
            v-model="form.unidadMedidaId"
            required
            class="mk-input w-full rounded border border-mk-border bg-transparent px-3 py-2"
          >
            <option value="" disabled>Seleccione…</option>
            <option v-for="u in unidades" :key="u.id" :value="u.id">{{ u.nombre }} ({{ u.abreviacion }})</option>
          </select>
        </div>
        <div class="space-y-1">
          <label class="text-sm font-medium">Imagen (URL)</label>
          <input
            v-model="form.imagenUrl"
            type="text"
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
            <th class="px-4 py-2 font-medium">Código</th>
            <th class="px-4 py-2 font-medium">Nombre</th>
            <th class="px-4 py-2 font-medium">Categoría</th>
            <th class="px-4 py-2 font-medium">Marca</th>
            <th class="px-4 py-2 font-medium">Unidad</th>
            <th class="px-4 py-2 font-medium">Estado</th>
            <th class="px-4 py-2 font-medium">Acciones</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="listLoading">
            <td colspan="7" class="px-4 py-6 text-center text-mk-text/60">Cargando…</td>
          </tr>
          <tr v-else-if="listError">
            <td colspan="7" class="px-4 py-6 text-center text-mk-danger">{{ listError }}</td>
          </tr>
          <tr v-else-if="filtered.length === 0">
            <td colspan="7" class="px-4 py-6 text-center text-mk-text/60">Sin resultados.</td>
          </tr>
          <tr v-for="producto in filtered" :key="producto.id" class="border-b border-mk-border last:border-0">
            <td class="px-4 py-2">{{ producto.codigoInterno }}</td>
            <td class="px-4 py-2">{{ producto.nombre }}</td>
            <td class="px-4 py-2">{{ nombreCategoria(producto.categoriaId) }}</td>
            <td class="px-4 py-2">{{ nombreMarca(producto.marcaId) }}</td>
            <td class="px-4 py-2">{{ nombreUnidad(producto.unidadMedidaId) }}</td>
            <td class="px-4 py-2">
              <EstadoBadge
                :variant="producto.activo ? 'success' : 'neutral'"
                :label="producto.activo ? 'Activo' : 'Inactivo'"
              />
            </td>
            <td class="px-4 py-2 whitespace-nowrap">
              <RouterLink
                v-if="permissions.can('PRODUCTOS_VER')"
                :to="`/productos/${producto.id}/tiendas`"
                class="mr-3 text-mk-primary hover:underline"
              >
                Tiendas
              </RouterLink>
              <button
                v-if="permissions.can('PRODUCTOS_EDITAR')"
                type="button"
                class="mr-3 text-mk-primary hover:underline"
                @click="abrirEditar(producto)"
              >
                Editar
              </button>
              <button
                v-if="permissions.can('PRODUCTOS_EDITAR')"
                type="button"
                class="text-mk-primary hover:underline"
                @click="alternarEstado(producto)"
              >
                {{ producto.activo ? 'Desactivar' : 'Activar' }}
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="flex items-center justify-between text-sm text-mk-text/70">
      <select v-model.number="tamano" class="rounded border border-mk-border bg-transparent px-2 py-1">
        <option :value="10">10 / página</option>
        <option :value="25">25 / página</option>
        <option :value="50">50 / página</option>
        <option :value="100">100 / página</option>
      </select>
      <div class="flex items-center gap-2">
        <button type="button" :disabled="pagina <= 1" class="disabled:opacity-40" @click="pagina--">Anterior</button>
        <span>Página {{ pagina }} de {{ totalPaginas }} ({{ totalElementos }} en total)</span>
        <button type="button" :disabled="pagina >= totalPaginas" class="disabled:opacity-40" @click="pagina++">
          Siguiente
        </button>
      </div>
    </div>
  </div>
</template>
