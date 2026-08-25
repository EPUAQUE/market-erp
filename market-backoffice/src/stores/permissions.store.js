import { defineStore } from 'pinia';
export const usePermissionsStore = defineStore('permissions', {
    state: () => ({
        permisos: new Set(),
        tiendaIds: new Set(),
        alcanceGlobal: false,
    }),
    actions: {
        hydrate(permisos, tiendaIds, alcanceGlobal) {
            this.permisos = new Set(permisos);
            this.tiendaIds = new Set(tiendaIds);
            this.alcanceGlobal = alcanceGlobal;
        },
        clear() {
            this.permisos = new Set();
            this.tiendaIds = new Set();
            this.alcanceGlobal = false;
        },
        can(codigo) {
            return this.permisos.has(codigo);
        },
        canAny(codigos) {
            return codigos.some((codigo) => this.permisos.has(codigo));
        },
        canAccessTienda(tiendaId) {
            return this.alcanceGlobal || this.tiendaIds.has(tiendaId);
        },
    },
});
