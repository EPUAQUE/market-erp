<script setup lang="ts">
defineProps<{ name: string }>()
</script>

<template>
  <svg
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    stroke-width="2"
    stroke-linecap="round"
    stroke-linejoin="round"
  >
    <template v-if="name === 'categorias'">
      <rect x="3" y="3" width="7" height="7" rx="1.5" />
      <rect x="14" y="3" width="7" height="7" rx="1.5" />
      <rect x="3" y="14" width="7" height="7" rx="1.5" />
      <rect x="14" y="14" width="7" height="7" rx="1.5" />
    </template>
    <template v-else-if="name === 'marcas'">
      <path d="M20.6 12.6 12 21.2 2.8 12 4.4 3l9.1-1.4z" />
      <circle cx="8.5" cy="7.5" r="1.2" fill="currentColor" stroke="none" />
    </template>
    <template v-else-if="name === 'unidades-medida'">
      <path d="M4 15 15 4l5 5-11 11z" />
      <path d="m9 10 2 2M12 7l2 2M6 13l2 2" />
    </template>
    <template v-else-if="name === 'productos'">
      <rect x="3" y="7" width="18" height="13" rx="2" />
      <path d="M8 7V5a4 4 0 0 1 8 0v2" />
    </template>
    <template v-else-if="name === 'inventario'">
      <rect x="3" y="7" width="18" height="14" rx="2" />
      <path d="M3 11h18M8 3v4M16 3v4" />
    </template>
    <template v-else-if="name === 'proveedores'">
      <path d="M1 16V6a1 1 0 0 1 1-1h10v11" />
      <path d="M12 9h4l4 4v3h-2" />
      <circle cx="6" cy="18" r="2" />
      <circle cx="16.5" cy="18" r="2" />
    </template>
    <template v-else-if="name === 'compras'">
      <path d="M6 3h9l5 5v13a1 1 0 0 1-1 1H6a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1z" />
      <path d="M14 3v5h5" />
    </template>
    <template v-else-if="name === 'cuentas-por-pagar'">
      <circle cx="12" cy="12" r="9" />
      <path d="M12 8v5l3 2" />
      <path d="M12 8V6M12 6l-2 2M12 6l2 2" />
    </template>
    <template v-else-if="name === 'traslados'">
      <path d="m17 3 4 4-4 4" />
      <path d="M21 7H8" />
      <path d="m7 21-4-4 4-4" />
      <path d="M3 17h13" />
    </template>
    <template v-else-if="name === 'clientes'">
      <circle cx="12" cy="8" r="3.2" />
      <path d="M4.5 20a7.5 7.5 0 0 1 15 0" />
    </template>
    <template v-else-if="name === 'ventas'">
      <path d="M6 2h9l5 5v14a1 1 0 0 1-1 1H6a1 1 0 0 1-1-1V3a1 1 0 0 1 1-1z" />
      <path d="M9 12h6M9 16h6" />
    </template>
    <template v-else-if="name === 'cuentas-por-cobrar'">
      <circle cx="12" cy="12" r="9" />
      <path d="M9 10h6M9 14h4" />
    </template>
    <template v-else-if="name === 'caja'">
      <rect x="2.5" y="6" width="19" height="13" rx="2" />
      <path d="M2.5 10.5h19M6 15h4" />
    </template>
    <template v-else-if="name === 'fel'">
      <path d="M6 3h9l5 5v13a1 1 0 0 1-1 1H6a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1z" />
      <path d="M14 3v5h5" />
      <path d="m9 14 2 2 4-4" />
    </template>
    <template v-else-if="name === 'tiendas'">
      <path d="M4 10V21h16V10" />
      <path d="M2.5 10 4 4h16l1.5 6z" />
      <path d="M9 21v-6h6v6" />
    </template>
    <template v-else-if="name === 'grupos-tienda'">
      <path d="M3 21V10l5-4 5 4v11" />
      <path d="M13 21V7l5-3 3 2v15" />
      <path d="M6 13h2M6 17h2" />
    </template>
    <template v-else-if="name === 'gastos-programados'">
      <rect x="3" y="4" width="18" height="17" rx="2" />
      <path d="M3 9h18M8 2v4M16 2v4" />
      <path d="M8 14h2M8 17h2M14 14h2" />
    </template>
    <template v-else-if="name === 'notificaciones'">
      <path d="M18 8a6 6 0 1 0-12 0c0 7-3 9-3 9h18s-3-2-3-9" />
      <path d="M13.73 21a2 2 0 0 1-3.46 0" />
    </template>
    <template v-else-if="name === 'reportes'">
      <path d="M4 20V10M10 20V4M16 20v-7M22 20H2" />
    </template>
    <template v-else-if="name === 'usuarios'">
      <circle cx="9" cy="8" r="3" />
      <path d="M2.5 20a6.5 6.5 0 0 1 13 0" />
      <path d="M16 8.5a3 3 0 1 1 3.5 2.96" />
      <path d="M15.5 14.5c2.9.4 5 2.3 5 5.5" />
    </template>
    <template v-else-if="name === 'dashboard'">
      <path d="M3 9.5 12 3l9 6.5V20a1 1 0 0 1-1 1h-5v-7H9v7H4a1 1 0 0 1-1-1z" />
    </template>
  </svg>
</template>
