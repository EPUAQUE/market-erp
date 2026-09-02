<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useProductoTiendas } from '@/composables/useProductoTiendas'
import { usePermissionsStore } from '@/stores/permissions.store'
import { tiendasService } from '@/services/tiendas.service'
import { formatCurrency } from '@/utils/money'
import EstadoBadge from '@/components/common/EstadoBadge.vue'
import type { ProductoTienda } from '@/types/producto'
import type { Tienda } from '@/types/tienda'

const route = useRoute()
const productoId = Number(route.params.productoId)

const { items, listLoading, listError, saveLoading, saveError, cargar, asignar, actualizar, alternarEstado } =
  useProductoTiendas(productoId)
const permissions = usePermissionsStore()

const tiendas = ref<Tienda[]>([])

const showForm = ref(false)
const editingId = ref<number | null>(null)
const form = ref({
  tiendaId: '' as number | '',
  precioVenta: '',
  stockMinimo: '0',
  stockMaximo: '',
  permitirVenta: true,
  permitirIngreso: true,
})

const tiendasDisponibles = computed(() => {
  const asignadas = new Set(items.value.map((pt) => pt.tiendaId))
  return tiendas.value.filter((t) => !asignadas.has(t.id))
})

function nombreTienda(id: number) {
  return tiendas.value.find((t) => t.id === id)?.nombre ?? id
}

function abrirAsignar() {
  editingId.value = null
  form.value = {
    tiendaId: '',
    precioVenta: '',
    stockMinimo: '0',
    stockMaximo: '',
    permitirVenta: true,
    permitirIngreso: true,
  }
  showForm.value = true
}

function abrirEditar(pt: ProductoTienda) {
  editingId.value = pt.id
  form.value = {
    tiendaId: pt.tiendaId,
    precioVenta: pt.precioVenta,
    stockMinimo: pt.stockMinimo,
    stockMaximo: pt.stockMaximo,
    permitirVenta: pt.permitirVenta,
    permitirIngreso: pt.permitirIngreso,
  }
  showForm.value = true
}

async function onSubmit() {
  const datos = {
    precioVenta: form.value.precioVenta,
    stockMinimo: form.value.stockMinimo,
    stockMaximo: form.value.stockMaximo,
    permitirVenta: form.value.permitirVenta,
    permitirIngreso: form.value.permitirIngreso,
  }
  const ok = editingId.value
    ? await actualizar(editingId.value, datos)
    : await asignar(Number(form.value.tiendaId), datos)
  if (ok) showForm.value = false
}

onMounted(async () => {
  await Promise.all([cargar(), tiendasService.listar().then((r) => (tiendas.value = r))])
})
</script>

<template>
  <div class="mx-auto max-w-4xl space-y-6 p-6">
    <header class="space-y-1">
      <RouterLink to="/productos" class="text-sm text-mk-primary hover:underline">← Productos</RouterLink>
      <h1 class="text-xl font-semibold">Configuración por tienda</h1>
      <p class="text-sm text-mk-text/70">
        Precio, stock y si permite venta/ingreso de inventario en cada tienda.
      </p>
    </header>

    <div class="flex items-center justify-end">
      <button
        v-if="permissions.can('PRODUCTOS_EDITAR')"
        type="button"
        class="mk-btn mk-btn-primary rounded bg-mk-primary px-4 py-2 text-sm font-medium text-white"
        @click="showForm ? (showForm = false) : abrirAsignar()"
      >
        {{ showForm ? 'Cancelar' : 'Asignar a tienda' }}
      </button>
    </div>

    <form v-if="showForm" class="space-y-3 rounded border border-mk-border p-4" @submit.prevent="onSubmit">
      <div class="grid gap-3 sm:grid-cols-2">
        <div class="space-y-1">
          <label class="text-sm font-medium">Tienda</label>
          <select
            v-model="form.tiendaId"
            required
            :disabled="editingId !== null"
            class="mk-input w-full rounded border border-mk-border bg-transparent px-3 py-2 disabled:opacity-50"
          >
            <option value="" disabled>Seleccione…</option>
            <option v-for="t in tiendasDisponibles" :key="t.id" :value="t.id">{{ t.nombre }}</option>
          </select>
        </div>
        <div class="space-y-1">
          <label class="text-sm font-medium">Precio de venta</label>
          <input
            v-model="form.precioVenta"
            type="text"
            inputmode="decimal"
            required
            class="mk-input w-full rounded border border-mk-border bg-transparent px-3 py-2"
          />
        </div>
        <div class="space-y-1">
          <label class="text-sm font-medium">Stock mínimo</label>
          <input
            v-model="form.stockMinimo"
            type="text"
            inputmode="decimal"
            required
            class="mk-input w-full rounded border border-mk-border bg-transparent px-3 py-2"
          />
        </div>
        <div class="space-y-1">
          <label class="text-sm font-medium">Stock máximo</label>
          <input
            v-model="form.stockMaximo"
            type="text"
            inputmode="decimal"
            required
            class="mk-input w-full rounded border border-mk-border bg-transparent px-3 py-2"
          />
        </div>
        <div class="flex items-center gap-2">
          <input id="permitirVenta" v-model="form.permitirVenta" type="checkbox" />
          <label for="permitirVenta" class="text-sm">Permite venta</label>
        </div>
        <div class="flex items-center gap-2">
          <input id="permitirIngreso" v-model="form.permitirIngreso" type="checkbox" />
          <label for="permitirIngreso" class="text-sm">Permite ingreso de inventario</label>
        </div>
      </div>
      <p v-if="saveError" class="text-sm text-mk-danger" role="alert">{{ saveError }}</p>
      <button
        type="submit"
        :disabled="saveLoading"
        class="mk-btn mk-btn-primary rounded bg-mk-primary px-4 py-2 text-sm font-medium text-white disabled:opacity-50"
      >
        {{ saveLoading ? 'Guardando…' : editingId ? 'Guardar cambios' : 'Asignar' }}
      </button>
    </form>

    <div class="mk-scroll-x overflow-x-auto rounded border border-mk-border">
      <table class="w-full text-left text-sm">
        <thead class="border-b border-mk-border bg-mk-surface">
          <tr>
            <th class="px-4 py-2 font-medium">Tienda</th>
            <th class="px-4 py-2 font-medium">Precio</th>
            <th class="px-4 py-2 font-medium">Stock (min/max)</th>
            <th class="px-4 py-2 font-medium">Venta</th>
            <th class="px-4 py-2 font-medium">Ingreso</th>
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
          <tr v-else-if="items.length === 0">
            <td colspan="7" class="px-4 py-6 text-center text-mk-text/60">Sin configuraciones aún.</td>
          </tr>
          <tr v-for="pt in items" :key="pt.id" class="border-b border-mk-border last:border-0">
            <td class="px-4 py-2">{{ nombreTienda(pt.tiendaId) }}</td>
            <td class="px-4 py-2 fc-num">{{ formatCurrency(pt.precioVenta) }}</td>
            <td class="px-4 py-2 fc-num">{{ pt.stockMinimo }} / {{ pt.stockMaximo }}</td>
            <td class="px-4 py-2">{{ pt.permitirVenta ? 'Sí' : 'No' }}</td>
            <td class="px-4 py-2">{{ pt.permitirIngreso ? 'Sí' : 'No' }}</td>
            <td class="px-4 py-2">
              <EstadoBadge
                :variant="pt.activo ? 'success' : 'neutral'"
                :label="pt.activo ? 'Activo' : 'Inactivo'"
              />
            </td>
            <td class="px-4 py-2 whitespace-nowrap">
              <button
                v-if="permissions.can('PRODUCTOS_EDITAR')"
                type="button"
                class="mr-3 text-mk-primary hover:underline"
                @click="abrirEditar(pt)"
              >
                Editar
              </button>
              <button
                v-if="permissions.can('PRODUCTOS_EDITAR')"
                type="button"
                class="text-mk-primary hover:underline"
                @click="alternarEstado(pt)"
              >
                {{ pt.activo ? 'Desactivar' : 'Activar' }}
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>
