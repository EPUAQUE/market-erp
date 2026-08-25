export const API_ENDPOINTS = {
    auth: {
        login: '/api/v1/auth/login',
        refresh: '/api/v1/auth/refresh',
        logout: '/api/v1/auth/logout',
        me: '/api/v1/auth/me',
    },
    usuarios: {
        base: '/api/v1/usuarios',
        asignarTienda: (usuarioId) => `/api/v1/usuarios/${usuarioId}/tiendas`,
    },
    tiendas: {
        base: '/api/v1/tiendas',
        porId: (id) => `/api/v1/tiendas/${id}`,
        activar: (id) => `/api/v1/tiendas/${id}/activar`,
        desactivar: (id) => `/api/v1/tiendas/${id}/desactivar`,
    },
    unidadesMedida: {
        base: '/api/v1/unidades-medida',
        porId: (id) => `/api/v1/unidades-medida/${id}`,
    },
    categorias: {
        base: '/api/v1/categorias',
        porId: (id) => `/api/v1/categorias/${id}`,
        activar: (id) => `/api/v1/categorias/${id}/activar`,
        desactivar: (id) => `/api/v1/categorias/${id}/desactivar`,
    },
    marcas: {
        base: '/api/v1/marcas',
        porId: (id) => `/api/v1/marcas/${id}`,
    },
    productos: {
        base: '/api/v1/productos',
        porId: (id) => `/api/v1/productos/${id}`,
        activar: (id) => `/api/v1/productos/${id}/activar`,
        desactivar: (id) => `/api/v1/productos/${id}/desactivar`,
        tiendas: (productoId) => `/api/v1/productos/${productoId}/tiendas`,
        tiendaPorId: (productoId, id) => `/api/v1/productos/${productoId}/tiendas/${id}`,
        tiendaActivar: (productoId, id) => `/api/v1/productos/${productoId}/tiendas/${id}/activar`,
        tiendaDesactivar: (productoId, id) => `/api/v1/productos/${productoId}/tiendas/${id}/desactivar`,
    },
    inventario: {
        porTienda: (tiendaId) => `/api/v1/inventario/tiendas/${tiendaId}`,
        movimientos: (tiendaId, productoId) => `/api/v1/inventario/tiendas/${tiendaId}/productos/${productoId}/movimientos`,
        registrarMovimiento: (tiendaId) => `/api/v1/inventario/tiendas/${tiendaId}/movimientos`,
    },
    proveedores: {
        base: '/api/v1/proveedores',
        porId: (id) => `/api/v1/proveedores/${id}`,
        activar: (id) => `/api/v1/proveedores/${id}/activar`,
        desactivar: (id) => `/api/v1/proveedores/${id}/desactivar`,
    },
    compras: {
        porTienda: (tiendaId) => `/api/v1/compras/tiendas/${tiendaId}`,
        recibir: (tiendaId, id) => `/api/v1/compras/tiendas/${tiendaId}/${id}/recibir`,
        anular: (tiendaId, id) => `/api/v1/compras/tiendas/${tiendaId}/${id}/anular`,
    },
    cuentasPorPagar: {
        porTienda: (tiendaId) => `/api/v1/cuentas-por-pagar/tiendas/${tiendaId}`,
        pagos: (tiendaId, id) => `/api/v1/cuentas-por-pagar/tiendas/${tiendaId}/${id}/pagos`,
        anular: (tiendaId, id) => `/api/v1/cuentas-por-pagar/tiendas/${tiendaId}/${id}/anular`,
    },
    clientes: {
        base: '/api/v1/clientes',
        porId: (id) => `/api/v1/clientes/${id}`,
        activar: (id) => `/api/v1/clientes/${id}/activar`,
        desactivar: (id) => `/api/v1/clientes/${id}/desactivar`,
    },
    ventas: {
        porTienda: (tiendaId) => `/api/v1/ventas/tiendas/${tiendaId}`,
        completar: (tiendaId, id) => `/api/v1/ventas/tiendas/${tiendaId}/${id}/completar`,
        anular: (tiendaId, id) => `/api/v1/ventas/tiendas/${tiendaId}/${id}/anular`,
    },
    cuentasPorCobrar: {
        porTienda: (tiendaId) => `/api/v1/cuentas-por-cobrar/tiendas/${tiendaId}`,
        cobros: (tiendaId, id) => `/api/v1/cuentas-por-cobrar/tiendas/${tiendaId}/${id}/cobros`,
        anular: (tiendaId, id) => `/api/v1/cuentas-por-cobrar/tiendas/${tiendaId}/${id}/anular`,
    },
    caja: {
        porTienda: (tiendaId) => `/api/v1/caja/tiendas/${tiendaId}`,
        abierta: (tiendaId) => `/api/v1/caja/tiendas/${tiendaId}/abierta`,
        abrir: (tiendaId) => `/api/v1/caja/tiendas/${tiendaId}/abrir`,
        movimientos: (tiendaId) => `/api/v1/caja/tiendas/${tiendaId}/movimientos`,
        cerrar: (tiendaId) => `/api/v1/caja/tiendas/${tiendaId}/cerrar`,
    },
    traslados: {
        base: '/api/v1/traslados',
        completar: (id) => `/api/v1/traslados/${id}/completar`,
        anular: (id) => `/api/v1/traslados/${id}/anular`,
    },
    gastosProgramados: {
        porTienda: (tiendaId) => `/api/v1/gastos-programados/tiendas/${tiendaId}`,
        porId: (tiendaId, id) => `/api/v1/gastos-programados/tiendas/${tiendaId}/${id}`,
        activar: (tiendaId, id) => `/api/v1/gastos-programados/tiendas/${tiendaId}/${id}/activar`,
        desactivar: (tiendaId, id) => `/api/v1/gastos-programados/tiendas/${tiendaId}/${id}/desactivar`,
        generarPago: (tiendaId, id) => `/api/v1/gastos-programados/tiendas/${tiendaId}/${id}/generar-pago`,
    },
    notificaciones: {
        porTienda: (tiendaId) => `/api/v1/notificaciones/tiendas/${tiendaId}`,
        noLeidas: (tiendaId) => `/api/v1/notificaciones/tiendas/${tiendaId}/no-leidas`,
        generar: (tiendaId) => `/api/v1/notificaciones/tiendas/${tiendaId}/generar`,
        marcarLeida: (tiendaId, id) => `/api/v1/notificaciones/tiendas/${tiendaId}/${id}/marcar-leida`,
    },
    dashboard: {
        porTienda: (tiendaId) => `/api/v1/dashboard/tiendas/${tiendaId}`,
    },
    reportes: {
        ventas: (tiendaId) => `/api/v1/reportes/tiendas/${tiendaId}/ventas`,
        compras: (tiendaId) => `/api/v1/reportes/tiendas/${tiendaId}/compras`,
    },
    fel: {
        porTienda: (tiendaId) => `/api/v1/fel/tiendas/${tiendaId}`,
        emitir: (tiendaId, ventaId) => `/api/v1/fel/tiendas/${tiendaId}/ventas/${ventaId}/emitir`,
        reintentar: (tiendaId, id) => `/api/v1/fel/tiendas/${tiendaId}/${id}/reintentar`,
        anular: (tiendaId, id) => `/api/v1/fel/tiendas/${tiendaId}/${id}/anular`,
    },
};
