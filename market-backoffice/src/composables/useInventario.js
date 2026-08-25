import { ref } from 'vue';
import { inventarioService } from '@/services/inventario.service';
import { ApiClientError } from '@/services/http/ApiClient';
export function useInventario() {
    const items = ref([]);
    const listLoading = ref(false);
    const listError = ref(null);
    const pagina = ref(1);
    const tamano = ref(10);
    const totalElementos = ref(0);
    const totalPaginas = ref(1);
    const movimientos = ref([]);
    const movimientosLoading = ref(false);
    const movimientosError = ref(null);
    const movimientosPagina = ref(1);
    const movimientosTamano = ref(10);
    const movimientosTotalElementos = ref(0);
    const movimientosTotalPaginas = ref(1);
    const saveLoading = ref(false);
    const saveError = ref(null);
    let tiendaActual = null;
    let productoKardexActual = null;
    async function cargar(tiendaId) {
        tiendaActual = tiendaId;
        listLoading.value = true;
        listError.value = null;
        try {
            const resultado = await inventarioService.listarPorTienda(tiendaId, pagina.value - 1, tamano.value);
            items.value = resultado.contenido;
            totalElementos.value = resultado.totalElementos;
            totalPaginas.value = resultado.totalPaginas;
        }
        catch (error) {
            listError.value = error instanceof ApiClientError ? error.message : 'No se pudo cargar el inventario.';
        }
        finally {
            listLoading.value = false;
        }
    }
    async function cargarMovimientos(tiendaId, productoId) {
        tiendaActual = tiendaId;
        productoKardexActual = productoId;
        movimientosLoading.value = true;
        movimientosError.value = null;
        try {
            const resultado = await inventarioService.listarMovimientos(tiendaId, productoId, movimientosPagina.value - 1, movimientosTamano.value);
            movimientos.value = resultado.contenido;
            movimientosTotalElementos.value = resultado.totalElementos;
            movimientosTotalPaginas.value = resultado.totalPaginas;
        }
        catch (error) {
            movimientosError.value = error instanceof ApiClientError ? error.message : 'No se pudo cargar el kardex.';
        }
        finally {
            movimientosLoading.value = false;
        }
    }
    async function registrarMovimiento(tiendaId, datos) {
        saveLoading.value = true;
        saveError.value = null;
        try {
            await inventarioService.registrarMovimiento(tiendaId, datos);
            if (tiendaActual !== null)
                await cargar(tiendaActual);
            if (productoKardexActual === datos.productoId && tiendaActual !== null) {
                await cargarMovimientos(tiendaActual, productoKardexActual);
            }
            return true;
        }
        catch (error) {
            saveError.value = error instanceof ApiClientError ? error.message : 'No se pudo registrar el movimiento.';
            return false;
        }
        finally {
            saveLoading.value = false;
        }
    }
    return {
        items,
        listLoading,
        listError,
        pagina,
        tamano,
        totalElementos,
        totalPaginas,
        movimientos,
        movimientosLoading,
        movimientosError,
        movimientosPagina,
        movimientosTamano,
        movimientosTotalElementos,
        movimientosTotalPaginas,
        saveLoading,
        saveError,
        cargar,
        cargarMovimientos,
        registrarMovimiento,
    };
}
