import { ref } from 'vue';
import { ventasService } from '@/services/ventas.service';
import { ApiClientError } from '@/services/http/ApiClient';
export function useVentas() {
    const items = ref([]);
    const listLoading = ref(false);
    const listError = ref(null);
    const saveLoading = ref(false);
    const saveError = ref(null);
    // `pagina` es 1-based para la UI; se convierte a 0-based (el backend) al llamar al servicio.
    const pagina = ref(1);
    const tamano = ref(10);
    const totalElementos = ref(0);
    const totalPaginas = ref(1);
    let tiendaActual = null;
    async function cargar(tiendaId) {
        tiendaActual = tiendaId;
        listLoading.value = true;
        listError.value = null;
        try {
            const resultado = await ventasService.listarPorTienda(tiendaId, pagina.value - 1, tamano.value);
            items.value = resultado.contenido;
            totalElementos.value = resultado.totalElementos;
            totalPaginas.value = resultado.totalPaginas;
        }
        catch (error) {
            listError.value = error instanceof ApiClientError ? error.message : 'No se pudo cargar la lista.';
        }
        finally {
            listLoading.value = false;
        }
    }
    /** Recarga la página actual desde el servidor — usado tras crear/completar/anular para no desincronizar los totales. */
    async function recargar() {
        if (tiendaActual !== null)
            await cargar(tiendaActual);
    }
    async function crear(tiendaId, clienteId, lineas) {
        saveLoading.value = true;
        saveError.value = null;
        try {
            await ventasService.crear(tiendaId, clienteId, lineas);
            await recargar();
            return true;
        }
        catch (error) {
            saveError.value = error instanceof ApiClientError ? error.message : 'No se pudo crear la venta.';
            return false;
        }
        finally {
            saveLoading.value = false;
        }
    }
    async function completar(tiendaId, venta) {
        listError.value = null;
        try {
            await ventasService.completar(tiendaId, venta.id);
            await recargar();
        }
        catch (error) {
            listError.value = error instanceof ApiClientError ? error.message : 'No se pudo completar la venta.';
        }
    }
    async function anular(tiendaId, venta) {
        listError.value = null;
        try {
            await ventasService.anular(tiendaId, venta.id);
            await recargar();
        }
        catch (error) {
            listError.value = error instanceof ApiClientError ? error.message : 'No se pudo anular la venta.';
        }
    }
    return {
        items,
        listLoading,
        listError,
        saveLoading,
        saveError,
        pagina,
        tamano,
        totalElementos,
        totalPaginas,
        cargar,
        crear,
        completar,
        anular,
    };
}
