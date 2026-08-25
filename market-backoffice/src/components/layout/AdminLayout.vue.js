import { computed, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth.store';
import { useUserStore } from '@/stores/user.store';
import { usePermissionsStore } from '@/stores/permissions.store';
const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();
const userStore = useUserStore();
const permissions = usePermissionsStore();
const navGroups = [
    {
        label: 'Catálogo',
        items: [
            { label: 'Categorías', path: '/categorias', permission: 'CATEGORIAS_VER' },
            { label: 'Marcas', path: '/marcas', permission: 'MARCAS_VER' },
            { label: 'Unidades de Medida', path: '/unidades-medida', permission: 'UNIDADES_MEDIDA_VER' },
            { label: 'Productos', path: '/productos', permission: 'PRODUCTOS_VER' },
        ],
    },
    {
        label: 'Operación',
        items: [
            { label: 'Inventario', path: '/inventario', permission: 'INVENTARIO_VER' },
            { label: 'Proveedores', path: '/proveedores', permission: 'PROVEEDORES_VER' },
            { label: 'Compras', path: '/compras', permission: 'COMPRAS_VER' },
            { label: 'Cuentas por Pagar', path: '/cuentas-por-pagar', permission: 'CUENTAS_POR_PAGAR_VER' },
            { label: 'Traslados', path: '/traslados', permission: 'TRASLADOS_VER' },
        ],
    },
    {
        label: 'Ventas',
        items: [
            { label: 'Clientes', path: '/clientes', permission: 'CLIENTES_VER' },
            { label: 'Ventas', path: '/ventas', permission: 'VENTAS_VER' },
            { label: 'Cuentas por Cobrar', path: '/cuentas-por-cobrar', permission: 'CUENTAS_POR_COBRAR_VER' },
            { label: 'Caja', path: '/caja', permission: 'CAJA_VER' },
            { label: 'Facturación Electrónica', path: '/fel', permission: 'FEL_VER' },
        ],
    },
    {
        label: 'Administración',
        items: [
            { label: 'Tiendas', path: '/tiendas', permission: 'TIENDAS_VER' },
            { label: 'Gastos Programados', path: '/gastos-programados', permission: 'GASTOS_PROGRAMADOS_VER' },
            { label: 'Notificaciones', path: '/notificaciones', permission: 'NOTIFICACIONES_VER' },
            { label: 'Reportes', path: '/reportes', permission: 'REPORTES_VER' },
            { label: 'Usuarios', path: '/usuarios', permission: 'USUARIOS_VER' },
        ],
    },
];
const visibleGroups = computed(() => navGroups
    .map((group) => ({ ...group, items: group.items.filter((item) => permissions.can(item.permission)) }))
    .filter((group) => group.items.length > 0));
const quickActions = [
    { label: 'Crear Venta', path: '/ventas', permission: 'VENTAS_VER' },
    { label: 'Registrar Compra', path: '/compras', permission: 'COMPRAS_VER' },
    { label: 'Cobrar Cliente', path: '/cuentas-por-cobrar', permission: 'CUENTAS_POR_COBRAR_VER' },
    { label: 'Pagar Proveedor', path: '/cuentas-por-pagar', permission: 'CUENTAS_POR_PAGAR_VER' },
    { label: 'Trasladar Inventario', path: '/traslados', permission: 'TRASLADOS_VER' },
];
const visibleQuickActions = computed(() => quickActions.filter((a) => permissions.can(a.permission)));
const tiendaLabel = computed(() => {
    if (permissions.alcanceGlobal)
        return 'Todas las tiendas';
    const n = permissions.tiendaIds.size;
    return n === 1 ? '1 tienda asignada' : `${n} tiendas asignadas`;
});
const moduleTitle = computed(() => route.meta.title ?? 'Market');
const searchOpen = ref(false);
const searchQuery = ref('');
const allNavItems = computed(() => navGroups.flatMap((g) => g.items).filter((item) => permissions.can(item.permission)));
const searchResults = computed(() => {
    const q = searchQuery.value.trim().toLowerCase();
    if (!q)
        return [];
    return allNavItems.value.filter((item) => item.label.toLowerCase().includes(q)).slice(0, 8);
});
function goToResult(item) {
    router.push(item.path);
    searchQuery.value = '';
    searchOpen.value = false;
}
function onSearchBlur() {
    window.setTimeout(() => {
        searchOpen.value = false;
    }, 150);
}
async function onLogout() {
    await authStore.logout();
    router.push({ name: 'login' });
}
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "flex min-h-screen bg-mk-bg text-mk-text" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.aside, __VLS_intrinsicElements.aside)({
    ...{ class: "flex w-64 shrink-0 flex-col bg-mk-brand text-mk-brand-ink" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "flex items-center gap-2 px-5 py-5" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "flex h-9 w-9 items-center justify-center rounded-md bg-white/10 text-sm font-bold" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "leading-tight" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
    ...{ class: "text-sm font-bold tracking-wide" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
    ...{ class: "text-[11px] text-white/60" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "mx-4 mb-4 rounded-md bg-white/5 px-3 py-2" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
    ...{ class: "text-[10px] font-semibold uppercase tracking-wider text-white/50" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
    ...{ class: "truncate text-sm font-medium" },
});
(__VLS_ctx.tiendaLabel);
__VLS_asFunctionalElement(__VLS_intrinsicElements.nav, __VLS_intrinsicElements.nav)({
    ...{ class: "flex-1 space-y-5 overflow-y-auto px-3 pb-4" },
});
for (const [group] of __VLS_getVForSourceType((__VLS_ctx.visibleGroups))) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        key: (group.label),
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
        ...{ class: "px-2 pb-1.5 text-[11px] font-semibold uppercase tracking-wider text-white/45" },
    });
    (group.label);
    for (const [item] of __VLS_getVForSourceType((group.items))) {
        const __VLS_0 = {}.RouterLink;
        /** @type {[typeof __VLS_components.RouterLink, typeof __VLS_components.RouterLink, ]} */ ;
        // @ts-ignore
        const __VLS_1 = __VLS_asFunctionalComponent(__VLS_0, new __VLS_0({
            key: (item.path),
            to: (item.path),
            ...{ class: "group relative flex items-center gap-2 rounded-md px-2.5 py-2 text-sm font-medium text-white/75 transition-colors hover:bg-white/10 hover:text-white" },
            activeClass: "!bg-white/12 !text-white",
        }));
        const __VLS_2 = __VLS_1({
            key: (item.path),
            to: (item.path),
            ...{ class: "group relative flex items-center gap-2 rounded-md px-2.5 py-2 text-sm font-medium text-white/75 transition-colors hover:bg-white/10 hover:text-white" },
            activeClass: "!bg-white/12 !text-white",
        }, ...__VLS_functionalComponentArgsRest(__VLS_1));
        __VLS_3.slots.default;
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span)({
            ...{ class: "absolute left-0 top-1/2 h-4 w-[3px] -translate-y-1/2 rounded-full bg-mk-accent opacity-0 transition-opacity group-[.router-link-active]:opacity-100" },
        });
        (item.label);
        var __VLS_3;
    }
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "flex min-h-screen flex-1 flex-col" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.header, __VLS_intrinsicElements.header)({
    ...{ class: "flex items-center justify-between gap-4 border-b border-mk-border bg-mk-surface px-6 py-3" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "min-w-0" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h1, __VLS_intrinsicElements.h1)({
    ...{ class: "truncate text-base font-bold text-mk-text" },
});
(__VLS_ctx.moduleTitle);
__VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
    ...{ class: "truncate text-xs text-mk-text-muted" },
});
(__VLS_ctx.moduleTitle);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "relative w-full max-w-sm" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.svg, __VLS_intrinsicElements.svg)({
    ...{ class: "pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-mk-text-muted" },
    viewBox: "0 0 24 24",
    fill: "none",
    stroke: "currentColor",
    'stroke-width': "2",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.circle)({
    cx: "11",
    cy: "11",
    r: "7",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.path)({
    d: "m21 21-4.3-4.3",
    'stroke-linecap': "round",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    ...{ onFocus: (...[$event]) => {
            __VLS_ctx.searchOpen = true;
        } },
    ...{ onBlur: (__VLS_ctx.onSearchBlur) },
    value: (__VLS_ctx.searchQuery),
    type: "text",
    placeholder: "Buscar un módulo…",
    ...{ class: "mk-input w-full rounded-md border border-mk-border py-2 pl-9 pr-3 text-sm" },
});
if (__VLS_ctx.searchOpen && __VLS_ctx.searchResults.length > 0) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "mk-card absolute z-10 mt-1 w-full overflow-hidden py-1" },
    });
    for (const [item] of __VLS_getVForSourceType((__VLS_ctx.searchResults))) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
            ...{ onClick: (...[$event]) => {
                    if (!(__VLS_ctx.searchOpen && __VLS_ctx.searchResults.length > 0))
                        return;
                    __VLS_ctx.goToResult(item);
                } },
            key: (item.path),
            type: "button",
            ...{ class: "block w-full px-3 py-1.5 text-left text-sm hover:bg-mk-surface-2" },
        });
        (item.label);
    }
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "flex items-center gap-2" },
});
if (__VLS_ctx.visibleQuickActions.length > 0) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "hidden items-center gap-2 lg:flex" },
    });
    for (const [action] of __VLS_getVForSourceType((__VLS_ctx.visibleQuickActions.slice(0, 2)))) {
        const __VLS_4 = {}.RouterLink;
        /** @type {[typeof __VLS_components.RouterLink, typeof __VLS_components.RouterLink, ]} */ ;
        // @ts-ignore
        const __VLS_5 = __VLS_asFunctionalComponent(__VLS_4, new __VLS_4({
            key: (action.path),
            to: (action.path),
            ...{ class: "mk-btn mk-btn-primary px-3 py-1.5 text-xs" },
        }));
        const __VLS_6 = __VLS_5({
            key: (action.path),
            to: (action.path),
            ...{ class: "mk-btn mk-btn-primary px-3 py-1.5 text-xs" },
        }, ...__VLS_functionalComponentArgsRest(__VLS_5));
        __VLS_7.slots.default;
        (action.label);
        var __VLS_7;
    }
}
if (__VLS_ctx.permissions.can('NOTIFICACIONES_VER')) {
    const __VLS_8 = {}.RouterLink;
    /** @type {[typeof __VLS_components.RouterLink, typeof __VLS_components.RouterLink, ]} */ ;
    // @ts-ignore
    const __VLS_9 = __VLS_asFunctionalComponent(__VLS_8, new __VLS_8({
        to: "/notificaciones",
        ...{ class: "mk-btn-ghost flex h-9 w-9 items-center justify-center rounded-md" },
        title: "Notificaciones",
    }));
    const __VLS_10 = __VLS_9({
        to: "/notificaciones",
        ...{ class: "mk-btn-ghost flex h-9 w-9 items-center justify-center rounded-md" },
        title: "Notificaciones",
    }, ...__VLS_functionalComponentArgsRest(__VLS_9));
    __VLS_11.slots.default;
    __VLS_asFunctionalElement(__VLS_intrinsicElements.svg, __VLS_intrinsicElements.svg)({
        ...{ class: "h-5 w-5" },
        viewBox: "0 0 24 24",
        fill: "none",
        stroke: "currentColor",
        'stroke-width': "2",
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.path)({
        d: "M18 8a6 6 0 1 0-12 0c0 7-3 9-3 9h18s-3-2-3-9",
        'stroke-linecap': "round",
        'stroke-linejoin': "round",
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.path)({
        d: "M13.73 21a2 2 0 0 1-3.46 0",
        'stroke-linecap': "round",
        'stroke-linejoin': "round",
    });
    var __VLS_11;
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.div)({
    ...{ class: "mx-1 h-6 w-px bg-mk-border" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "hidden text-sm text-mk-text-muted sm:inline" },
});
(__VLS_ctx.userStore.username);
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.onLogout) },
    type: "button",
    ...{ class: "mk-btn mk-btn-ghost px-3 py-1.5 text-sm" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.main, __VLS_intrinsicElements.main)({
    ...{ class: "flex-1" },
});
const __VLS_12 = {}.RouterView;
/** @type {[typeof __VLS_components.RouterView, ]} */ ;
// @ts-ignore
const __VLS_13 = __VLS_asFunctionalComponent(__VLS_12, new __VLS_12({}));
const __VLS_14 = __VLS_13({}, ...__VLS_functionalComponentArgsRest(__VLS_13));
/** @type {__VLS_StyleScopedClasses['flex']} */ ;
/** @type {__VLS_StyleScopedClasses['min-h-screen']} */ ;
/** @type {__VLS_StyleScopedClasses['bg-mk-bg']} */ ;
/** @type {__VLS_StyleScopedClasses['text-mk-text']} */ ;
/** @type {__VLS_StyleScopedClasses['flex']} */ ;
/** @type {__VLS_StyleScopedClasses['w-64']} */ ;
/** @type {__VLS_StyleScopedClasses['shrink-0']} */ ;
/** @type {__VLS_StyleScopedClasses['flex-col']} */ ;
/** @type {__VLS_StyleScopedClasses['bg-mk-brand']} */ ;
/** @type {__VLS_StyleScopedClasses['text-mk-brand-ink']} */ ;
/** @type {__VLS_StyleScopedClasses['flex']} */ ;
/** @type {__VLS_StyleScopedClasses['items-center']} */ ;
/** @type {__VLS_StyleScopedClasses['gap-2']} */ ;
/** @type {__VLS_StyleScopedClasses['px-5']} */ ;
/** @type {__VLS_StyleScopedClasses['py-5']} */ ;
/** @type {__VLS_StyleScopedClasses['flex']} */ ;
/** @type {__VLS_StyleScopedClasses['h-9']} */ ;
/** @type {__VLS_StyleScopedClasses['w-9']} */ ;
/** @type {__VLS_StyleScopedClasses['items-center']} */ ;
/** @type {__VLS_StyleScopedClasses['justify-center']} */ ;
/** @type {__VLS_StyleScopedClasses['rounded-md']} */ ;
/** @type {__VLS_StyleScopedClasses['bg-white/10']} */ ;
/** @type {__VLS_StyleScopedClasses['text-sm']} */ ;
/** @type {__VLS_StyleScopedClasses['font-bold']} */ ;
/** @type {__VLS_StyleScopedClasses['leading-tight']} */ ;
/** @type {__VLS_StyleScopedClasses['text-sm']} */ ;
/** @type {__VLS_StyleScopedClasses['font-bold']} */ ;
/** @type {__VLS_StyleScopedClasses['tracking-wide']} */ ;
/** @type {__VLS_StyleScopedClasses['text-[11px]']} */ ;
/** @type {__VLS_StyleScopedClasses['text-white/60']} */ ;
/** @type {__VLS_StyleScopedClasses['mx-4']} */ ;
/** @type {__VLS_StyleScopedClasses['mb-4']} */ ;
/** @type {__VLS_StyleScopedClasses['rounded-md']} */ ;
/** @type {__VLS_StyleScopedClasses['bg-white/5']} */ ;
/** @type {__VLS_StyleScopedClasses['px-3']} */ ;
/** @type {__VLS_StyleScopedClasses['py-2']} */ ;
/** @type {__VLS_StyleScopedClasses['text-[10px]']} */ ;
/** @type {__VLS_StyleScopedClasses['font-semibold']} */ ;
/** @type {__VLS_StyleScopedClasses['uppercase']} */ ;
/** @type {__VLS_StyleScopedClasses['tracking-wider']} */ ;
/** @type {__VLS_StyleScopedClasses['text-white/50']} */ ;
/** @type {__VLS_StyleScopedClasses['truncate']} */ ;
/** @type {__VLS_StyleScopedClasses['text-sm']} */ ;
/** @type {__VLS_StyleScopedClasses['font-medium']} */ ;
/** @type {__VLS_StyleScopedClasses['flex-1']} */ ;
/** @type {__VLS_StyleScopedClasses['space-y-5']} */ ;
/** @type {__VLS_StyleScopedClasses['overflow-y-auto']} */ ;
/** @type {__VLS_StyleScopedClasses['px-3']} */ ;
/** @type {__VLS_StyleScopedClasses['pb-4']} */ ;
/** @type {__VLS_StyleScopedClasses['px-2']} */ ;
/** @type {__VLS_StyleScopedClasses['pb-1.5']} */ ;
/** @type {__VLS_StyleScopedClasses['text-[11px]']} */ ;
/** @type {__VLS_StyleScopedClasses['font-semibold']} */ ;
/** @type {__VLS_StyleScopedClasses['uppercase']} */ ;
/** @type {__VLS_StyleScopedClasses['tracking-wider']} */ ;
/** @type {__VLS_StyleScopedClasses['text-white/45']} */ ;
/** @type {__VLS_StyleScopedClasses['group']} */ ;
/** @type {__VLS_StyleScopedClasses['relative']} */ ;
/** @type {__VLS_StyleScopedClasses['flex']} */ ;
/** @type {__VLS_StyleScopedClasses['items-center']} */ ;
/** @type {__VLS_StyleScopedClasses['gap-2']} */ ;
/** @type {__VLS_StyleScopedClasses['rounded-md']} */ ;
/** @type {__VLS_StyleScopedClasses['px-2.5']} */ ;
/** @type {__VLS_StyleScopedClasses['py-2']} */ ;
/** @type {__VLS_StyleScopedClasses['text-sm']} */ ;
/** @type {__VLS_StyleScopedClasses['font-medium']} */ ;
/** @type {__VLS_StyleScopedClasses['text-white/75']} */ ;
/** @type {__VLS_StyleScopedClasses['transition-colors']} */ ;
/** @type {__VLS_StyleScopedClasses['hover:bg-white/10']} */ ;
/** @type {__VLS_StyleScopedClasses['hover:text-white']} */ ;
/** @type {__VLS_StyleScopedClasses['absolute']} */ ;
/** @type {__VLS_StyleScopedClasses['left-0']} */ ;
/** @type {__VLS_StyleScopedClasses['top-1/2']} */ ;
/** @type {__VLS_StyleScopedClasses['h-4']} */ ;
/** @type {__VLS_StyleScopedClasses['w-[3px]']} */ ;
/** @type {__VLS_StyleScopedClasses['-translate-y-1/2']} */ ;
/** @type {__VLS_StyleScopedClasses['rounded-full']} */ ;
/** @type {__VLS_StyleScopedClasses['bg-mk-accent']} */ ;
/** @type {__VLS_StyleScopedClasses['opacity-0']} */ ;
/** @type {__VLS_StyleScopedClasses['transition-opacity']} */ ;
/** @type {__VLS_StyleScopedClasses['group-[.router-link-active]:opacity-100']} */ ;
/** @type {__VLS_StyleScopedClasses['flex']} */ ;
/** @type {__VLS_StyleScopedClasses['min-h-screen']} */ ;
/** @type {__VLS_StyleScopedClasses['flex-1']} */ ;
/** @type {__VLS_StyleScopedClasses['flex-col']} */ ;
/** @type {__VLS_StyleScopedClasses['flex']} */ ;
/** @type {__VLS_StyleScopedClasses['items-center']} */ ;
/** @type {__VLS_StyleScopedClasses['justify-between']} */ ;
/** @type {__VLS_StyleScopedClasses['gap-4']} */ ;
/** @type {__VLS_StyleScopedClasses['border-b']} */ ;
/** @type {__VLS_StyleScopedClasses['border-mk-border']} */ ;
/** @type {__VLS_StyleScopedClasses['bg-mk-surface']} */ ;
/** @type {__VLS_StyleScopedClasses['px-6']} */ ;
/** @type {__VLS_StyleScopedClasses['py-3']} */ ;
/** @type {__VLS_StyleScopedClasses['min-w-0']} */ ;
/** @type {__VLS_StyleScopedClasses['truncate']} */ ;
/** @type {__VLS_StyleScopedClasses['text-base']} */ ;
/** @type {__VLS_StyleScopedClasses['font-bold']} */ ;
/** @type {__VLS_StyleScopedClasses['text-mk-text']} */ ;
/** @type {__VLS_StyleScopedClasses['truncate']} */ ;
/** @type {__VLS_StyleScopedClasses['text-xs']} */ ;
/** @type {__VLS_StyleScopedClasses['text-mk-text-muted']} */ ;
/** @type {__VLS_StyleScopedClasses['relative']} */ ;
/** @type {__VLS_StyleScopedClasses['w-full']} */ ;
/** @type {__VLS_StyleScopedClasses['max-w-sm']} */ ;
/** @type {__VLS_StyleScopedClasses['pointer-events-none']} */ ;
/** @type {__VLS_StyleScopedClasses['absolute']} */ ;
/** @type {__VLS_StyleScopedClasses['left-3']} */ ;
/** @type {__VLS_StyleScopedClasses['top-1/2']} */ ;
/** @type {__VLS_StyleScopedClasses['h-4']} */ ;
/** @type {__VLS_StyleScopedClasses['w-4']} */ ;
/** @type {__VLS_StyleScopedClasses['-translate-y-1/2']} */ ;
/** @type {__VLS_StyleScopedClasses['text-mk-text-muted']} */ ;
/** @type {__VLS_StyleScopedClasses['mk-input']} */ ;
/** @type {__VLS_StyleScopedClasses['w-full']} */ ;
/** @type {__VLS_StyleScopedClasses['rounded-md']} */ ;
/** @type {__VLS_StyleScopedClasses['border']} */ ;
/** @type {__VLS_StyleScopedClasses['border-mk-border']} */ ;
/** @type {__VLS_StyleScopedClasses['py-2']} */ ;
/** @type {__VLS_StyleScopedClasses['pl-9']} */ ;
/** @type {__VLS_StyleScopedClasses['pr-3']} */ ;
/** @type {__VLS_StyleScopedClasses['text-sm']} */ ;
/** @type {__VLS_StyleScopedClasses['mk-card']} */ ;
/** @type {__VLS_StyleScopedClasses['absolute']} */ ;
/** @type {__VLS_StyleScopedClasses['z-10']} */ ;
/** @type {__VLS_StyleScopedClasses['mt-1']} */ ;
/** @type {__VLS_StyleScopedClasses['w-full']} */ ;
/** @type {__VLS_StyleScopedClasses['overflow-hidden']} */ ;
/** @type {__VLS_StyleScopedClasses['py-1']} */ ;
/** @type {__VLS_StyleScopedClasses['block']} */ ;
/** @type {__VLS_StyleScopedClasses['w-full']} */ ;
/** @type {__VLS_StyleScopedClasses['px-3']} */ ;
/** @type {__VLS_StyleScopedClasses['py-1.5']} */ ;
/** @type {__VLS_StyleScopedClasses['text-left']} */ ;
/** @type {__VLS_StyleScopedClasses['text-sm']} */ ;
/** @type {__VLS_StyleScopedClasses['hover:bg-mk-surface-2']} */ ;
/** @type {__VLS_StyleScopedClasses['flex']} */ ;
/** @type {__VLS_StyleScopedClasses['items-center']} */ ;
/** @type {__VLS_StyleScopedClasses['gap-2']} */ ;
/** @type {__VLS_StyleScopedClasses['hidden']} */ ;
/** @type {__VLS_StyleScopedClasses['items-center']} */ ;
/** @type {__VLS_StyleScopedClasses['gap-2']} */ ;
/** @type {__VLS_StyleScopedClasses['lg:flex']} */ ;
/** @type {__VLS_StyleScopedClasses['mk-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['mk-btn-primary']} */ ;
/** @type {__VLS_StyleScopedClasses['px-3']} */ ;
/** @type {__VLS_StyleScopedClasses['py-1.5']} */ ;
/** @type {__VLS_StyleScopedClasses['text-xs']} */ ;
/** @type {__VLS_StyleScopedClasses['mk-btn-ghost']} */ ;
/** @type {__VLS_StyleScopedClasses['flex']} */ ;
/** @type {__VLS_StyleScopedClasses['h-9']} */ ;
/** @type {__VLS_StyleScopedClasses['w-9']} */ ;
/** @type {__VLS_StyleScopedClasses['items-center']} */ ;
/** @type {__VLS_StyleScopedClasses['justify-center']} */ ;
/** @type {__VLS_StyleScopedClasses['rounded-md']} */ ;
/** @type {__VLS_StyleScopedClasses['h-5']} */ ;
/** @type {__VLS_StyleScopedClasses['w-5']} */ ;
/** @type {__VLS_StyleScopedClasses['mx-1']} */ ;
/** @type {__VLS_StyleScopedClasses['h-6']} */ ;
/** @type {__VLS_StyleScopedClasses['w-px']} */ ;
/** @type {__VLS_StyleScopedClasses['bg-mk-border']} */ ;
/** @type {__VLS_StyleScopedClasses['hidden']} */ ;
/** @type {__VLS_StyleScopedClasses['text-sm']} */ ;
/** @type {__VLS_StyleScopedClasses['text-mk-text-muted']} */ ;
/** @type {__VLS_StyleScopedClasses['sm:inline']} */ ;
/** @type {__VLS_StyleScopedClasses['mk-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['mk-btn-ghost']} */ ;
/** @type {__VLS_StyleScopedClasses['px-3']} */ ;
/** @type {__VLS_StyleScopedClasses['py-1.5']} */ ;
/** @type {__VLS_StyleScopedClasses['text-sm']} */ ;
/** @type {__VLS_StyleScopedClasses['flex-1']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            userStore: userStore,
            permissions: permissions,
            visibleGroups: visibleGroups,
            visibleQuickActions: visibleQuickActions,
            tiendaLabel: tiendaLabel,
            moduleTitle: moduleTitle,
            searchOpen: searchOpen,
            searchQuery: searchQuery,
            searchResults: searchResults,
            goToResult: goToResult,
            onSearchBlur: onSearchBlur,
            onLogout: onLogout,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
