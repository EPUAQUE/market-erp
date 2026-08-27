export const API_ENDPOINTS = {
  auth: {
    login: '/api/v1/auth/login',
    refresh: '/api/v1/auth/refresh',
    logout: '/api/v1/auth/logout',
    me: '/api/v1/auth/me',
  },
  usuarios: {
    base: '/api/v1/usuarios',
    // Mismo endpoint para GET (listar asignaciones) y POST (asignar) — ver usuarios.service.ts.
    tiendas: (usuarioId: number) => `/api/v1/usuarios/${usuarioId}/tiendas`,
    grupos: (usuarioId: number) => `/api/v1/usuarios/${usuarioId}/grupos`,
  },
  roles: {
    base: '/api/v1/roles',
  },
  tiendas: {
    base: '/api/v1/tiendas',
    porId: (id: number) => `/api/v1/tiendas/${id}`,
    activar: (id: number) => `/api/v1/tiendas/${id}/activar`,
    desactivar: (id: number) => `/api/v1/tiendas/${id}/desactivar`,
  },
  gruposTienda: {
    base: '/api/v1/grupos-tienda',
    porId: (id: number) => `/api/v1/grupos-tienda/${id}`,
    activar: (id: number) => `/api/v1/grupos-tienda/${id}/activar`,
    desactivar: (id: number) => `/api/v1/grupos-tienda/${id}/desactivar`,
  },
  unidadesMedida: {
    base: '/api/v1/unidades-medida',
    porId: (id: number) => `/api/v1/unidades-medida/${id}`,
  },
  categorias: {
    base: '/api/v1/categorias',
    porId: (id: number) => `/api/v1/categorias/${id}`,
    activar: (id: number) => `/api/v1/categorias/${id}/activar`,
    desactivar: (id: number) => `/api/v1/categorias/${id}/desactivar`,
  },
  marcas: {
    base: '/api/v1/marcas',
    porId: (id: number) => `/api/v1/marcas/${id}`,
  },
  productos: {
    base: '/api/v1/productos',
    porId: (id: number) => `/api/v1/productos/${id}`,
    activar: (id: number) => `/api/v1/productos/${id}/activar`,
    desactivar: (id: number) => `/api/v1/productos/${id}/desactivar`,
    imagen: (id: number) => `/api/v1/productos/${id}/imagen`,
    tiendas: (productoId: number) => `/api/v1/productos/${productoId}/tiendas`,
    tiendaPorId: (productoId: number, id: number) => `/api/v1/productos/${productoId}/tiendas/${id}`,
    tiendaActivar: (productoId: number, id: number) => `/api/v1/productos/${productoId}/tiendas/${id}/activar`,
    tiendaDesactivar: (productoId: number, id: number) =>
      `/api/v1/productos/${productoId}/tiendas/${id}/desactivar`,
  },
  inventario: {
    porTienda: (tiendaId: number) => `/api/v1/inventario/tiendas/${tiendaId}`,
    movimientos: (tiendaId: number, productoId: number) =>
      `/api/v1/inventario/tiendas/${tiendaId}/productos/${productoId}/movimientos`,
    registrarMovimiento: (tiendaId: number) => `/api/v1/inventario/tiendas/${tiendaId}/movimientos`,
  },
  proveedores: {
    base: '/api/v1/proveedores',
    porId: (id: number) => `/api/v1/proveedores/${id}`,
    activar: (id: number) => `/api/v1/proveedores/${id}/activar`,
    desactivar: (id: number) => `/api/v1/proveedores/${id}/desactivar`,
  },
  compras: {
    porTienda: (tiendaId: number) => `/api/v1/compras/tiendas/${tiendaId}`,
    recibir: (tiendaId: number, id: number) => `/api/v1/compras/tiendas/${tiendaId}/${id}/recibir`,
    anular: (tiendaId: number, id: number) => `/api/v1/compras/tiendas/${tiendaId}/${id}/anular`,
  },
  cuentasPorPagar: {
    porTienda: (tiendaId: number) => `/api/v1/cuentas-por-pagar/tiendas/${tiendaId}`,
    pagos: (tiendaId: number, id: number) => `/api/v1/cuentas-por-pagar/tiendas/${tiendaId}/${id}/pagos`,
    anular: (tiendaId: number, id: number) => `/api/v1/cuentas-por-pagar/tiendas/${tiendaId}/${id}/anular`,
  },
  clientes: {
    base: '/api/v1/clientes',
    porId: (id: number) => `/api/v1/clientes/${id}`,
    activar: (id: number) => `/api/v1/clientes/${id}/activar`,
    desactivar: (id: number) => `/api/v1/clientes/${id}/desactivar`,
  },
  ventas: {
    porTienda: (tiendaId: number) => `/api/v1/ventas/tiendas/${tiendaId}`,
    completar: (tiendaId: number, id: number) => `/api/v1/ventas/tiendas/${tiendaId}/${id}/completar`,
    anular: (tiendaId: number, id: number) => `/api/v1/ventas/tiendas/${tiendaId}/${id}/anular`,
  },
  cuentasPorCobrar: {
    porTienda: (tiendaId: number) => `/api/v1/cuentas-por-cobrar/tiendas/${tiendaId}`,
    cobros: (tiendaId: number, id: number) => `/api/v1/cuentas-por-cobrar/tiendas/${tiendaId}/${id}/cobros`,
    anular: (tiendaId: number, id: number) => `/api/v1/cuentas-por-cobrar/tiendas/${tiendaId}/${id}/anular`,
  },
  caja: {
    porTienda: (tiendaId: number) => `/api/v1/caja/tiendas/${tiendaId}`,
    abierta: (tiendaId: number) => `/api/v1/caja/tiendas/${tiendaId}/abierta`,
    abrir: (tiendaId: number) => `/api/v1/caja/tiendas/${tiendaId}/abrir`,
    movimientos: (tiendaId: number) => `/api/v1/caja/tiendas/${tiendaId}/movimientos`,
    cerrar: (tiendaId: number) => `/api/v1/caja/tiendas/${tiendaId}/cerrar`,
  },
  traslados: {
    base: '/api/v1/traslados',
    completar: (id: number) => `/api/v1/traslados/${id}/completar`,
    anular: (id: number) => `/api/v1/traslados/${id}/anular`,
  },
  gastosProgramados: {
    porTienda: (tiendaId: number) => `/api/v1/gastos-programados/tiendas/${tiendaId}`,
    porId: (tiendaId: number, id: number) => `/api/v1/gastos-programados/tiendas/${tiendaId}/${id}`,
    activar: (tiendaId: number, id: number) => `/api/v1/gastos-programados/tiendas/${tiendaId}/${id}/activar`,
    desactivar: (tiendaId: number, id: number) => `/api/v1/gastos-programados/tiendas/${tiendaId}/${id}/desactivar`,
    generarPago: (tiendaId: number, id: number) =>
      `/api/v1/gastos-programados/tiendas/${tiendaId}/${id}/generar-pago`,
  },
  notificaciones: {
    porTienda: (tiendaId: number) => `/api/v1/notificaciones/tiendas/${tiendaId}`,
    noLeidas: (tiendaId: number) => `/api/v1/notificaciones/tiendas/${tiendaId}/no-leidas`,
    generar: (tiendaId: number) => `/api/v1/notificaciones/tiendas/${tiendaId}/generar`,
    marcarLeida: (tiendaId: number, id: number) =>
      `/api/v1/notificaciones/tiendas/${tiendaId}/${id}/marcar-leida`,
  },
  dashboard: {
    porTienda: (tiendaId: number) => `/api/v1/dashboard/tiendas/${tiendaId}`,
    porGrupo: (grupoId: number) => `/api/v1/dashboard/grupos/${grupoId}`,
  },
  reportes: {
    ventas: (tiendaId: number) => `/api/v1/reportes/tiendas/${tiendaId}/ventas`,
    compras: (tiendaId: number) => `/api/v1/reportes/tiendas/${tiendaId}/compras`,
  },
  fel: {
    porTienda: (tiendaId: number) => `/api/v1/fel/tiendas/${tiendaId}`,
    emitir: (tiendaId: number, ventaId: number) => `/api/v1/fel/tiendas/${tiendaId}/ventas/${ventaId}/emitir`,
    reintentar: (tiendaId: number, id: number) => `/api/v1/fel/tiendas/${tiendaId}/${id}/reintentar`,
    anular: (tiendaId: number, id: number) => `/api/v1/fel/tiendas/${tiendaId}/${id}/anular`,
  },
}
