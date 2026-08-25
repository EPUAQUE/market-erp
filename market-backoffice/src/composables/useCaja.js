import { ref } from 'vue';
import { cajaService } from '@/services/caja.service';
import { ApiClientError } from '@/services/http/ApiClient';
export function useCaja() {
    const sesionAbierta = ref(null);
    const sesionLoading = ref(false);
    const sesionError = ref(null);
    const historial = ref([]);
    const historialLoading = ref(false);
    const historialPagina = ref(1);
    const historialTamano = ref(10);
    const historialTotalElementos = ref(0);
    const historialTotalPaginas = ref(1);
    const actionLoading = ref(false);
    const actionError = ref(null);
    async function cargarAbierta(tiendaId) {
        sesionLoading.value = true;
        sesionError.value = null;
        try {
            sesionAbierta.value = await cajaService.obtenerAbierta(tiendaId);
        }
        catch (error) {
            if (error instanceof ApiClientError && error.status === 404) {
                sesionAbierta.value = null;
            }
            else {
                sesionError.value = error instanceof ApiClientError ? error.message : 'No se pudo cargar la caja.';
            }
        }
        finally {
            sesionLoading.value = false;
        }
    }
    async function cargarHistorial(tiendaId) {
        historialLoading.value = true;
        try {
            const resultado = await cajaService.listarPorTienda(tiendaId, historialPagina.value - 1, historialTamano.value);
            historial.value = resultado.contenido;
            historialTotalElementos.value = resultado.totalElementos;
            historialTotalPaginas.value = resultado.totalPaginas;
        }
        finally {
            historialLoading.value = false;
        }
    }
    async function abrir(tiendaId, montoInicial) {
        actionLoading.value = true;
        actionError.value = null;
        try {
            sesionAbierta.value = await cajaService.abrir(tiendaId, montoInicial);
            return true;
        }
        catch (error) {
            actionError.value = error instanceof ApiClientError ? error.message : 'No se pudo abrir la caja.';
            return false;
        }
        finally {
            actionLoading.value = false;
        }
    }
    async function registrarMovimiento(tiendaId, datos) {
        actionLoading.value = true;
        actionError.value = null;
        try {
            sesionAbierta.value = await cajaService.registrarMovimiento(tiendaId, datos);
            return true;
        }
        catch (error) {
            actionError.value = error instanceof ApiClientError ? error.message : 'No se pudo registrar el movimiento.';
            return false;
        }
        finally {
            actionLoading.value = false;
        }
    }
    async function cerrar(tiendaId, montoFinalContado) {
        actionLoading.value = true;
        actionError.value = null;
        try {
            await cajaService.cerrar(tiendaId, montoFinalContado);
            sesionAbierta.value = null;
            return true;
        }
        catch (error) {
            actionError.value = error instanceof ApiClientError ? error.message : 'No se pudo cerrar la caja.';
            return false;
        }
        finally {
            actionLoading.value = false;
        }
    }
    return {
        sesionAbierta,
        sesionLoading,
        sesionError,
        historial,
        historialLoading,
        historialPagina,
        historialTamano,
        historialTotalElementos,
        historialTotalPaginas,
        actionLoading,
        actionError,
        cargarAbierta,
        cargarHistorial,
        abrir,
        registrarMovimiento,
        cerrar,
    };
}
