import { createPinia, setActivePinia } from 'pinia'
import { describe, it, expect, beforeEach } from 'vitest'
import { usePermissionsStore } from './permissions.store'

describe('usePermissionsStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('arranca vacía — sin permisos, tiendas ni alcance global', () => {
    const store = usePermissionsStore()
    expect(store.can('VENTAS_VER')).toBe(false)
    expect(store.canAccessTienda(1)).toBe(false)
  })

  it('hydrate carga permisos, tiendas, alcance global y grupos', () => {
    const store = usePermissionsStore()
    store.hydrate(['VENTAS_VER', 'CAJA_CERRAR'], [1, 2], false, [9])

    expect(store.can('VENTAS_VER')).toBe(true)
    expect(store.can('CAJA_CERRAR')).toBe(true)
    expect(store.can('PRODUCTOS_EDITAR')).toBe(false)
    expect(store.canAccessTienda(1)).toBe(true)
    expect(store.canAccessTienda(2)).toBe(true)
    expect(store.canAccessTienda(3)).toBe(false)
  })

  it('canAny es true si tiene al menos uno de los códigos', () => {
    const store = usePermissionsStore()
    store.hydrate(['VENTAS_VER'], [1], false)

    expect(store.canAny(['PRODUCTOS_EDITAR', 'VENTAS_VER'])).toBe(true)
    expect(store.canAny(['PRODUCTOS_EDITAR', 'CAJA_CERRAR'])).toBe(false)
  })

  it('alcanceGlobal permite acceder a cualquier tienda aunque no esté en tiendaIds', () => {
    const store = usePermissionsStore()
    store.hydrate(['VENTAS_VER'], [], true)

    expect(store.canAccessTienda(999)).toBe(true)
  })

  it('clear resetea todo a su estado inicial', () => {
    const store = usePermissionsStore()
    store.hydrate(['VENTAS_VER'], [1], true, [9])

    store.clear()

    expect(store.can('VENTAS_VER')).toBe(false)
    expect(store.canAccessTienda(1)).toBe(false)
  })
})
