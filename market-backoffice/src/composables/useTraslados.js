import { ref } from 'vue';
import { trasladosService } from '@/services/traslados.service';
import { ApiClientError } from '@/services/http/ApiClient';
export function useTraslados() {
    const items = ref([]);
    const listLoading = ref(false);
    const listError = ref(null);
    const saveLoading = ref(false);
    const saveError = ref(null);
    const pagina = ref(1);
    const tamano = ref(10);
    const totalElementos = ref(0);
    const totalPaginas = ref(1);
    async function cargar() {
        listLoading.value = true;
        listError.value = null;
        try {
            const resultado = await trasladosService.listar(pagina.value - 1, tamano.value);
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
    async function crear(tiendaOrigenId, tiendaDestinoId, lineas) {
        saveLoading.value = true;
        saveError.value = null;
        try {
            await trasladosService.crear(tiendaOrigenId, tiendaDestinoId, lineas);
            await cargar();
            return true;
        }
        catch (error) {
            saveError.value = error instanceof ApiClientError ? error.message : 'No se pudo crear el traslado.';
            return false;
        }
        finally {
            saveLoading.value = false;
        }
    }
    async function completar(traslado) {
        listError.value = null;
        try {
            await trasladosService.completar(traslado.id);
            await cargar();
        }
        catch (error) {
            listError.value = error instanceof ApiClientError ? error.message : 'No se pudo completar el traslado.';
        }
    }
    async function anular(traslado) {
        listError.value = null;
        try {
            await trasladosService.anular(traslado.id);
            await cargar();
        }
        catch (error) {
            listError.value = error instanceof ApiClientError ? error.message : 'No se pudo anular el traslado.';
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
