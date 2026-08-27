import { ref } from 'vue'
import { usuariosService } from '@/services/usuarios.service'
import { ApiClientError } from '@/services/http/ApiClient'
import type { Usuario, UsuarioGrupo, UsuarioTienda } from '@/types/usuario'

export function useUsuarios() {
  const items = ref<Usuario[]>([])
  const listLoading = ref(false)
  const listError = ref<string | null>(null)
  const createLoading = ref(false)
  const createError = ref<string | null>(null)

  const tiendasPorUsuario = ref<Record<number, UsuarioTienda[]>>({})
  const tiendasLoading = ref(false)
  const tiendasError = ref<string | null>(null)
  const asignarLoading = ref(false)
  const asignarError = ref<string | null>(null)

  const gruposPorUsuario = ref<Record<number, UsuarioGrupo[]>>({})
  const gruposLoading = ref(false)
  const gruposError = ref<string | null>(null)
  const asignarGrupoLoading = ref(false)
  const asignarGrupoError = ref<string | null>(null)

  async function cargar() {
    listLoading.value = true
    listError.value = null
    try {
      items.value = await usuariosService.listar()
    } catch (error) {
      listError.value = error instanceof ApiClientError ? error.message : 'No se pudo cargar la lista.'
    } finally {
      listLoading.value = false
    }
  }

  /** Devuelve el id del usuario creado (o null si falló) — lo necesita UsuariosView para asignar tienda justo después. */
  async function crear(
    username: string,
    password: string,
    nombre: string,
    telefono: string,
    correo: string,
  ): Promise<number | null> {
    createLoading.value = true
    createError.value = null
    try {
      const creado = await usuariosService.crear(username, password, nombre, telefono, correo)
      items.value = [...items.value, creado]
      return creado.id
    } catch (error) {
      createError.value = error instanceof ApiClientError ? error.message : 'No se pudo crear el usuario.'
      return null
    } finally {
      createLoading.value = false
    }
  }

  async function cargarTiendas(usuarioId: number) {
    tiendasLoading.value = true
    tiendasError.value = null
    try {
      tiendasPorUsuario.value = { ...tiendasPorUsuario.value, [usuarioId]: await usuariosService.listarTiendas(usuarioId) }
    } catch (error) {
      tiendasError.value = error instanceof ApiClientError ? error.message : 'No se pudieron cargar las tiendas asignadas.'
    } finally {
      tiendasLoading.value = false
    }
  }

  async function asignarTienda(usuarioId: number, tiendaId: number, rolId: number): Promise<boolean> {
    asignarLoading.value = true
    asignarError.value = null
    try {
      await usuariosService.asignarTienda(usuarioId, tiendaId, rolId)
      await cargarTiendas(usuarioId)
      return true
    } catch (error) {
      asignarError.value = error instanceof ApiClientError ? error.message : 'No se pudo asignar la tienda.'
      return false
    } finally {
      asignarLoading.value = false
    }
  }

  async function cargarGrupos(usuarioId: number) {
    gruposLoading.value = true
    gruposError.value = null
    try {
      gruposPorUsuario.value = { ...gruposPorUsuario.value, [usuarioId]: await usuariosService.listarGrupos(usuarioId) }
    } catch (error) {
      gruposError.value = error instanceof ApiClientError ? error.message : 'No se pudieron cargar los grupos asignados.'
    } finally {
      gruposLoading.value = false
    }
  }

  async function asignarGrupo(usuarioId: number, grupoTiendaId: number, rolId: number): Promise<boolean> {
    asignarGrupoLoading.value = true
    asignarGrupoError.value = null
    try {
      await usuariosService.asignarGrupo(usuarioId, grupoTiendaId, rolId)
      await cargarGrupos(usuarioId)
      return true
    } catch (error) {
      asignarGrupoError.value = error instanceof ApiClientError ? error.message : 'No se pudo asignar el grupo.'
      return false
    } finally {
      asignarGrupoLoading.value = false
    }
  }

  return {
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
    gruposPorUsuario,
    gruposLoading,
    gruposError,
    asignarGrupoLoading,
    asignarGrupoError,
    cargar,
    crear,
    cargarTiendas,
    asignarTienda,
    cargarGrupos,
    asignarGrupo,
  }
}
