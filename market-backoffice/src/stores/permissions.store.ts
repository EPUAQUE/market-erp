import { defineStore } from 'pinia'
import type { PermissionCode } from '@/types/auth'

interface PermissionsState {
  permisos: Set<PermissionCode>
  tiendaIds: Set<number>
  alcanceGlobal: boolean
}

export const usePermissionsStore = defineStore('permissions', {
  state: (): PermissionsState => ({
    permisos: new Set(),
    tiendaIds: new Set(),
    alcanceGlobal: false,
  }),
  actions: {
    hydrate(permisos: PermissionCode[], tiendaIds: number[], alcanceGlobal: boolean) {
      this.permisos = new Set(permisos)
      this.tiendaIds = new Set(tiendaIds)
      this.alcanceGlobal = alcanceGlobal
    },
    clear() {
      this.permisos = new Set()
      this.tiendaIds = new Set()
      this.alcanceGlobal = false
    },
    can(codigo: PermissionCode): boolean {
      return this.permisos.has(codigo)
    },
    canAny(codigos: PermissionCode[]): boolean {
      return codigos.some((codigo) => this.permisos.has(codigo))
    },
    canAccessTienda(tiendaId: number): boolean {
      return this.alcanceGlobal || this.tiendaIds.has(tiendaId)
    },
  },
})
