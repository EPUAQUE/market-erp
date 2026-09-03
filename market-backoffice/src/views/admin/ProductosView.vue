<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useProductos } from '@/composables/useProductos'
import { useFiltrosTabla, type FiltroColumna } from '@/composables/useFiltrosTabla'
import { usePermissionsStore } from '@/stores/permissions.store'
import { categoriasService } from '@/services/categorias.service'
import { marcasService } from '@/services/marcas.service'
import { unidadesMedidaService } from '@/services/unidadesMedida.service'
import { resolverImagenUrl } from '@/utils/imagenUrl'
import EstadoBadge from '@/components/common/EstadoBadge.vue'
import ModalDialog from '@/components/common/ModalDialog.vue'
import ActionIcon from '@/components/common/ActionIcon.vue'
import PaginacionTabla from '@/components/common/PaginacionTabla.vue'
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
  subirImagen,
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

const showForm = ref(false)
const editingId = ref<number | null>(null)
const form = ref({
  codigoInterno: '',
  codigoBarras: '',
  nombre: '',
  descripcion: '',
  descripcionCorta: '',
  categoriaId: '' as number | '',
  marcaId: '' as number | '',
  unidadMedidaId: '' as number | '',
})
const imagenActualUrl = ref<string | null>(null)
const archivoImagen = ref<File | null>(null)
const previewImagen = ref<string | null>(null)

function onArchivoImagenSeleccionado(event: Event) {
  const input = event.target as HTMLInputElement
  const archivo = input.files?.[0] ?? null
  archivoImagen.value = archivo
  if (previewImagen.value) URL.revokeObjectURL(previewImagen.value)
  previewImagen.value = archivo ? URL.createObjectURL(archivo) : null
}

function nombreCategoria(id: number) {
  return categorias.value.find((c) => c.id === id)?.nombre ?? id
}
function nombreMarca(id: number) {
  return marcas.value.find((m) => m.id === id)?.nombre ?? id
}
function nombreUnidad(id: number) {
  return unidades.value.find((u) => u.id === id)?.abreviacion ?? id
}

// Nota: con paginación del servidor, este filtro solo busca dentro de la
// página cargada, no en todo el catálogo (ver CLAUDE.md, "Server-side
// pagination").
const COLUMNAS_FILTRO_PRODUCTOS: FiltroColumna<Producto>[] = [
  { clave: 'codigo', tipo: 'texto', valor: (p) => p.codigoInterno },
  { clave: 'nombre', tipo: 'texto', valor: (p) => p.nombre },
  { clave: 'categoria', tipo: 'texto', valor: (p) => String(nombreCategoria(p.categoriaId)) },
  { clave: 'marca', tipo: 'texto', valor: (p) => String(nombreMarca(p.marcaId)) },
  { clave: 'unidad', tipo: 'texto', valor: (p) => String(nombreUnidad(p.unidadMedidaId)) },
  {
    clave: 'estado',
    tipo: 'opciones',
    valor: (p) => (p.activo ? 'true' : 'false'),
    opciones: [
      { valor: 'true', etiqueta: 'Activo' },
      { valor: 'false', etiqueta: 'Inactivo' },
    ],
  },
]
const {
  busquedaGlobal: busquedaProductos,
  filtrosColumna: filtrosProductos,
  itemsFiltrados: filtered,
  limpiarFiltros: limpiarFiltrosProductos,
  hayFiltrosActivos: hayFiltrosProductosActivos,
} = useFiltrosTabla(items, COLUMNAS_FILTRO_PRODUCTOS)

function resetImagenForm() {
  if (previewImagen.value) URL.revokeObjectURL(previewImagen.value)
  archivoImagen.value = null
  previewImagen.value = null
}

const modalTitle = computed(() => (editingId.value !== null ? 'Editar producto' : 'Nuevo producto'))

function abrirCrear() {
  editingId.value = null
  form.value = {
    codigoInterno: '',
    codigoBarras: '',
    nombre: '',
    descripcion: '',
    descripcionCorta: '',
    categoriaId: '',
    marcaId: '',
    unidadMedidaId: '',
  }
  imagenActualUrl.value = null
  resetImagenForm()
  showForm.value = true
}

function abrirEditar(producto: Producto) {
  editingId.value = producto.id
  form.value = {
    codigoInterno: producto.codigoInterno,
    codigoBarras: producto.codigoBarras ?? '',
    nombre: producto.nombre,
    descripcion: producto.descripcion ?? '',
    descripcionCorta: producto.descripcionCorta ?? '',
    categoriaId: producto.categoriaId,
    marcaId: producto.marcaId,
    unidadMedidaId: producto.unidadMedidaId,
  }
  imagenActualUrl.value = producto.imagenUrl
  resetImagenForm()
  showForm.value = true
}

async function onSubmit() {
  if (!form.value.categoriaId || !form.value.marcaId || !form.value.unidadMedidaId) return
  const datos = {
    codigoBarras: form.value.codigoBarras || undefined,
    nombre: form.value.nombre,
    descripcion: form.value.descripcion || undefined,
    descripcionCorta: form.value.descripcionCorta || undefined,
    categoriaId: Number(form.value.categoriaId),
    marcaId: Number(form.value.marcaId),
    unidadMedidaId: Number(form.value.unidadMedidaId),
    // El backend sobrescribe imagenUrl con lo que venga acá (ver
    // ActualizarProductoRequest) — hay que reenviar el valor actual o una
    // edición de cualquier otro campo borraría la imagen ya subida. La imagen
    // en sí se sube aparte (subirImagen abajo), esto solo evita perderla.
    imagenUrl: imagenActualUrl.value ?? undefined,
  }
  // La imagen se sube en un segundo paso porque necesita el id del producto
  // (endpoint dedicado `POST /productos/{id}/imagen`, ver productos.service.ts)
  // — al crear, ese id todavía no existe hasta que el primer paso responde.
  let id: number | null
  if (editingId.value) {
    id = (await actualizar(editingId.value, datos)) ? editingId.value : null
  } else {
    id = await crear(form.value.codigoInterno, datos)
  }
  if (!id) return
  if (archivoImagen.value && !(await subirImagen(id, archivoImagen.value))) return
  showForm.value = false
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
      <div class="flex items-center gap-2">
        <input
          v-model="busquedaProductos"
          type="search"
          placeholder="Buscar en todas las columnas…"
          class="mk-input w-full max-w-xs rounded border border-mk-border bg-transparent px-3 py-2"
        />
        <button
          v-if="hayFiltrosProductosActivos"
          type="button"
          class="text-sm text-mk-text/60 hover:underline"
          @click="limpiarFiltrosProductos"
        >
          Limpiar filtros
        </button>
      </div>
      <button
        v-if="permissions.can('PRODUCTOS_CREAR')"
        type="button"
        class="mk-btn mk-btn-primary rounded bg-mk-primary px-4 py-2 text-sm font-medium text-white"
        @click="abrirCrear()"
      >
        Nuevo producto
      </button>
    </div>

    <ModalDialog v-model="showForm" :title="modalTitle" max-width="max-w-2xl">
      <form class="space-y-3" @submit.prevent="onSubmit">
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
          <div class="space-y-1 sm:col-span-2">
            <label class="text-sm font-medium">Descripción corta</label>
            <input
              v-model="form.descripcionCorta"
              type="text"
              maxlength="100"
              placeholder="Para imprimir en factura/recibo y mostrar en el POS"
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
              <option v-for="u in unidades" :key="u.id" :value="u.id">
                {{ u.nombre }} ({{ u.abreviacion }})
              </option>
            </select>
          </div>
          <div class="space-y-1">
            <label class="text-sm font-medium">Imagen</label>
            <div class="flex items-center gap-3">
              <img
                v-if="previewImagen || imagenActualUrl"
                :src="previewImagen ?? resolverImagenUrl(imagenActualUrl)"
                alt=""
                class="h-14 w-14 rounded border border-mk-border object-cover"
              />
              <input
                type="file"
                accept="image/jpeg,image/png,image/webp"
                class="mk-input w-full rounded border border-mk-border bg-transparent px-3 py-2 text-sm"
                @change="onArchivoImagenSeleccionado"
              />
            </div>
            <p class="text-xs text-mk-text/60">JPG, PNG o WEBP — máximo 5MB.</p>
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
            <th class="px-4 py-2 font-medium">Imagen</th>
            <th class="px-4 py-2 font-medium">Código</th>
            <th class="px-4 py-2 font-medium">Nombre</th>
            <th class="px-4 py-2 font-medium">Categoría</th>
            <th class="px-4 py-2 font-medium">Marca</th>
            <th class="px-4 py-2 font-medium">Unidad</th>
            <th class="px-4 py-2 font-medium">Estado</th>
            <th class="px-4 py-2 font-medium">Acciones</th>
          </tr>
          <tr class="border-b border-mk-border bg-mk-surface/50">
            <th class="px-4 py-1.5"></th>
            <th class="px-4 py-1.5 font-normal">
              <input
                v-model="filtrosProductos.codigo"
                type="text"
                placeholder="Filtrar…"
                class="mk-input w-full rounded border border-mk-border bg-transparent px-2 py-1 text-xs"
              />
            </th>
            <th class="px-4 py-1.5 font-normal">
              <input
                v-model="filtrosProductos.nombre"
                type="text"
                placeholder="Filtrar…"
                class="mk-input w-full rounded border border-mk-border bg-transparent px-2 py-1 text-xs"
              />
            </th>
            <th class="px-4 py-1.5 font-normal">
              <select
                v-model="filtrosProductos.categoria"
                class="mk-input w-full rounded border border-mk-border bg-transparent px-2 py-1 text-xs"
              >
                <option value="">Todas</option>
                <option v-for="c in categorias" :key="c.id" :value="c.nombre">{{ c.nombre }}</option>
              </select>
            </th>
            <th class="px-4 py-1.5 font-normal">
              <select
                v-model="filtrosProductos.marca"
                class="mk-input w-full rounded border border-mk-border bg-transparent px-2 py-1 text-xs"
              >
                <option value="">Todas</option>
                <option v-for="m in marcas" :key="m.id" :value="m.nombre">{{ m.nombre }}</option>
              </select>
            </th>
            <th class="px-4 py-1.5 font-normal">
              <select
                v-model="filtrosProductos.unidad"
                class="mk-input w-full rounded border border-mk-border bg-transparent px-2 py-1 text-xs"
              >
                <option value="">Todas</option>
                <option v-for="u in unidades" :key="u.id" :value="u.abreviacion">{{ u.abreviacion }}</option>
              </select>
            </th>
            <th class="px-4 py-1.5 font-normal">
              <select
                v-model="filtrosProductos.estado"
                class="mk-input w-full rounded border border-mk-border bg-transparent px-2 py-1 text-xs"
              >
                <option value="">Todos</option>
                <option value="true">Activo</option>
                <option value="false">Inactivo</option>
              </select>
            </th>
            <th class="px-4 py-1.5"></th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="listLoading">
            <td colspan="8" class="px-4 py-6 text-center text-mk-text/60">Cargando…</td>
          </tr>
          <tr v-else-if="listError">
            <td colspan="8" class="px-4 py-6 text-center text-mk-danger">{{ listError }}</td>
          </tr>
          <tr v-else-if="filtered.length === 0">
            <td colspan="8" class="px-4 py-6 text-center text-mk-text/60">Sin resultados.</td>
          </tr>
          <tr v-for="producto in filtered" :key="producto.id" class="border-b border-mk-border last:border-0">
            <td class="px-4 py-2">
              <img
                v-if="producto.imagenUrl"
                :src="resolverImagenUrl(producto.imagenUrl)"
                alt=""
                class="h-10 w-10 rounded border border-mk-border object-cover"
              />
              <span v-else class="text-mk-text/40">—</span>
            </td>
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
              <div class="mk-row-actions">
                <RouterLink
                  v-if="permissions.can('PRODUCTOS_VER')"
                  :to="`/productos/${producto.id}/tiendas`"
                  class="mk-row-btn"
                  title="Ver tiendas"
                >
                  <ActionIcon name="store" />
                </RouterLink>
                <button
                  v-if="permissions.can('PRODUCTOS_EDITAR')"
                  type="button"
                  class="mk-row-btn"
                  title="Editar"
                  @click="abrirEditar(producto)"
                >
                  <ActionIcon name="edit" />
                </button>
                <button
                  v-if="permissions.can('PRODUCTOS_EDITAR')"
                  type="button"
                  class="mk-row-btn"
                  :class="producto.activo ? 'mk-row-btn-danger' : 'mk-row-btn-success'"
                  :title="producto.activo ? 'Desactivar' : 'Activar'"
                  @click="alternarEstado(producto)"
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
      <select v-model.number="tamano" class="rounded border border-mk-border bg-transparent px-2 py-1">
        <option :value="10">10 / página</option>
        <option :value="25">25 / página</option>
        <option :value="50">50 / página</option>
        <option :value="100">100 / página</option>
      </select>
      <div class="flex items-center gap-3">
        <span>{{ totalElementos }} en total</span>
        <PaginacionTabla v-model:pagina="pagina" :total-paginas="totalPaginas" />
      </div>
    </div>
  </div>
</template>
