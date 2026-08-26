<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useUsuarios } from '@/composables/useUsuarios'
import { useTiendas } from '@/composables/useTiendas'
import { usePermissionsStore } from '@/stores/permissions.store'
import { rolesService } from '@/services/roles.service'
import EstadoBadge from '@/components/common/EstadoBadge.vue'
import type { EstadoUsuario, Usuario } from '@/types/usuario'
import type { Rol } from '@/types/rol'

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

const {
  items,
  listLoading,
  listError,
  createLoading,
  createError,
  tiendasPorUsuario,
  tiendasLoading,
  tiendasError,
  asignarLoading,
  asignarError,
  cargar,
  crear,
  cargarTiendas,
  asignarTienda,
} = useUsuarios()
const { items: tiendas, cargar: cargarTiendasCatalogo } = useTiendas()
const permissions = usePermissionsStore()

const search = ref('')
const page = ref(1)
const pageSize = ref(10)

const showCreateForm = ref(false)
const newUsername = ref('')
const newPassword = ref('')
const newNombre = ref('')
const newTelefono = ref('')
const newCorreo = ref('')
const newTiendaId = ref<number | ''>('')
const newRolId = ref<number | ''>('')

const roles = ref<Rol[]>([])
const expandedUsuarioId = ref<number | null>(null)
const nuevaAsignacionTiendaId = ref<number | ''>('')
const nuevaAsignacionRolId = ref<number | ''>('')

function nombreTienda(tiendaId: number): string {
  return tiendas.value.find((t) => t.id === tiendaId)?.nombre ?? `#${tiendaId}`
}

async function toggleTiendas(usuario: Usuario) {
  if (expandedUsuarioId.value === usuario.id) {
    expandedUsuarioId.value = null
    return
  }
  expandedUsuarioId.value = usuario.id
  nuevaAsignacionTiendaId.value = ''
  nuevaAsignacionRolId.value = ''
  if (!tiendasPorUsuario.value[usuario.id]) await cargarTiendas(usuario.id)
}

async function onAsignarTienda(usuarioId: number) {
  if (!nuevaAsignacionTiendaId.value || !nuevaAsignacionRolId.value) return
  const ok = await asignarTienda(usuarioId, Number(nuevaAsignacionTiendaId.value), Number(nuevaAsignacionRolId.value))
  if (ok) {
    nuevaAsignacionTiendaId.value = ''
    nuevaAsignacionRolId.value = ''
  }
}

const filtered = computed(() => {
  const term = search.value.trim().toLowerCase()
  if (!term) return items.value
  return items.value.filter(
    (usuario) =>
      usuario.username.toLowerCase().includes(term) || (usuario.nombre ?? '').toLowerCase().includes(term),
  )
})

const totalPages = computed(() => Math.max(1, Math.ceil(filtered.value.length / pageSize.value)))

const paged = computed(() => {
  const start = (page.value - 1) * pageSize.value
  return filtered.value.slice(start, start + pageSize.value)
})

async function onCrear() {
  const id = await crear(newUsername.value, newPassword.value, newNombre.value, newTelefono.value, newCorreo.value)
  if (!id) return

  // Tienda/rol son opcionales acá (ADMIN no necesita) — si se eligieron, se asignan
  // en el mismo envío en vez de obligar a un segundo paso en "Tiendas" por fila.
  if (newTiendaId.value && newRolId.value) {
    const asignado = await asignarTienda(id, Number(newTiendaId.value), Number(newRolId.value))
    if (!asignado) return
  }

  newUsername.value = ''
  newPassword.value = ''
  newNombre.value = ''
  newTelefono.value = ''
  newCorreo.value = ''
  newTiendaId.value = ''
  newRolId.value = ''
  showCreateForm.value = false
}

onMounted(async () => {
  await Promise.all([cargar(), cargarTiendasCatalogo(), rolesService.listar().then((r) => (roles.value = r))])
})
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
          <label class="text-sm font-medium">Nombre</label>
          <input
            v-model="newNombre"
            type="text"
            required
            class="mk-input w-full rounded border border-mk-border bg-transparent px-3 py-2"
          />
        </div>
        <div class="space-y-1">
          <label class="text-sm font-medium">Teléfono</label>
          <input
            v-model="newTelefono"
            type="tel"
            required
            class="mk-input w-full rounded border border-mk-border bg-transparent px-3 py-2"
          />
        </div>
        <div class="space-y-1 sm:col-span-2">
          <label class="text-sm font-medium">Correo electrónico</label>
          <input
            v-model="newCorreo"
            type="email"
            required
            class="mk-input w-full rounded border border-mk-border bg-transparent px-3 py-2"
          />
        </div>
        <div class="space-y-1">
          <label class="text-sm font-medium">Usuario</label>
          <input
            v-model="newUsername"
            type="text"
            required
            class="mk-input w-full rounded border border-mk-border bg-transparent px-3 py-2"
          />
          <p class="text-xs text-mk-text/60">Código con el que ingresa a la aplicación.</p>
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
        <div class="space-y-1">
          <label class="text-sm font-medium">Tienda</label>
          <select
            v-model="newTiendaId"
            class="mk-input w-full rounded border border-mk-border bg-transparent px-3 py-2"
          >
            <option value="">— Sin asignar por ahora —</option>
            <option v-for="tienda in tiendas" :key="tienda.id" :value="tienda.id">{{ tienda.nombre }}</option>
          </select>
        </div>
        <div class="space-y-1">
          <label class="text-sm font-medium">Rol</label>
          <select
            v-model="newRolId"
            class="mk-input w-full rounded border border-mk-border bg-transparent px-3 py-2"
          >
            <option value="">— Sin asignar por ahora —</option>
            <option v-for="rol in roles" :key="rol.id" :value="rol.id">{{ rol.nombre }}</option>
          </select>
          <p class="text-xs text-mk-text/60">
            ADMIN no necesita tienda. Encargado y Cajero (vendedor) sí — sin una no pueden ingresar.
          </p>
        </div>
      </div>
      <p v-if="createError" class="text-sm text-mk-danger" role="alert">{{ createError }}</p>
      <p v-if="asignarError" class="text-sm text-mk-danger" role="alert">{{ asignarError }}</p>
      <button
        type="submit"
        :disabled="createLoading || asignarLoading"
        class="mk-btn mk-btn-primary rounded bg-mk-primary px-4 py-2 text-sm font-medium text-white disabled:opacity-50"
      >
        {{ createLoading ? 'Creando…' : asignarLoading ? 'Asignando tienda…' : 'Crear' }}
      </button>
    </form>

    <div class="mk-scroll-x overflow-x-auto rounded border border-mk-border">
      <table class="w-full text-left text-sm">
        <thead class="border-b border-mk-border bg-mk-surface">
          <tr>
            <th class="px-4 py-2 font-medium">Nombre</th>
            <th class="px-4 py-2 font-medium">Usuario</th>
            <th class="px-4 py-2 font-medium">Teléfono</th>
            <th class="px-4 py-2 font-medium">Correo</th>
            <th class="px-4 py-2 font-medium">Estado</th>
            <th class="px-4 py-2 font-medium">Acciones</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="listLoading">
            <td colspan="6" class="px-4 py-6 text-center text-mk-text/60">Cargando…</td>
          </tr>
          <tr v-else-if="listError">
            <td colspan="6" class="px-4 py-6 text-center text-mk-danger">{{ listError }}</td>
          </tr>
          <tr v-else-if="paged.length === 0">
            <td colspan="6" class="px-4 py-6 text-center text-mk-text/60">Sin resultados.</td>
          </tr>
          <template v-for="usuario in paged" :key="usuario.id">
            <tr class="border-b border-mk-border last:border-0">
              <td class="px-4 py-2">{{ usuario.nombre ?? '—' }}</td>
              <td class="px-4 py-2">{{ usuario.username }}</td>
              <td class="px-4 py-2">{{ usuario.telefono ?? '—' }}</td>
              <td class="px-4 py-2">{{ usuario.correo ?? '—' }}</td>
              <td class="px-4 py-2">
                <EstadoBadge :variant="ESTADO_VARIANT[usuario.estado]" :label="ESTADO_LABEL[usuario.estado]" />
              </td>
              <td class="px-4 py-2">
                <button
                  v-if="permissions.can('USUARIOS_ASIGNAR_TIENDA')"
                  type="button"
                  class="text-mk-primary hover:underline"
                  @click="toggleTiendas(usuario)"
                >
                  {{ expandedUsuarioId === usuario.id ? 'Ocultar tiendas' : 'Tiendas' }}
                </button>
              </td>
            </tr>
            <tr v-if="expandedUsuarioId === usuario.id" class="border-b border-mk-border bg-mk-surface">
              <td colspan="6" class="px-4 py-4">
                <div class="space-y-3">
                  <div v-if="tiendasLoading" class="text-sm text-mk-text/60">Cargando…</div>
                  <ul v-else-if="(tiendasPorUsuario[usuario.id] ?? []).length > 0" class="space-y-1 text-sm">
                    <li v-for="asignacion in tiendasPorUsuario[usuario.id]" :key="asignacion.id">
                      {{ nombreTienda(asignacion.tiendaId) }} — {{ asignacion.rolNombre }}
                    </li>
                  </ul>
                  <p v-else class="text-sm text-mk-text/60">Sin tiendas asignadas todavía.</p>

                  <form
                    class="flex flex-wrap items-end gap-2"
                    @submit.prevent="onAsignarTienda(usuario.id)"
                  >
                    <div class="space-y-1">
                      <label class="text-xs font-medium">Tienda</label>
                      <select
                        v-model="nuevaAsignacionTiendaId"
                        required
                        class="mk-input rounded border border-mk-border bg-transparent px-3 py-2 text-sm"
                      >
                        <option value="" disabled>Seleccione…</option>
                        <option v-for="tienda in tiendas" :key="tienda.id" :value="tienda.id">
                          {{ tienda.nombre }}
                        </option>
                      </select>
                    </div>
                    <div class="space-y-1">
                      <label class="text-xs font-medium">Rol</label>
                      <select
                        v-model="nuevaAsignacionRolId"
                        required
                        class="mk-input rounded border border-mk-border bg-transparent px-3 py-2 text-sm"
                      >
                        <option value="" disabled>Seleccione…</option>
                        <option v-for="rol in roles" :key="rol.id" :value="rol.id">{{ rol.nombre }}</option>
                      </select>
                    </div>
                    <button
                      type="submit"
                      :disabled="asignarLoading"
                      class="mk-btn mk-btn-primary rounded bg-mk-primary px-3 py-2 text-sm font-medium text-white disabled:opacity-50"
                    >
                      {{ asignarLoading ? 'Asignando…' : 'Asignar' }}
                    </button>
                  </form>
                  <p v-if="tiendasError" class="text-sm text-mk-danger" role="alert">{{ tiendasError }}</p>
                  <p v-if="asignarError" class="text-sm text-mk-danger" role="alert">{{ asignarError }}</p>
                </div>
              </td>
            </tr>
          </template>
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
