import { ref } from 'vue';
import { felService } from '@/services/fel.service';
import { ApiClientError } from '@/services/http/ApiClient';
export function useFel() {
    const items = ref([]);
    const listLoading = ref(false);
    const listError = ref(null);
    const emitirLoading = ref(false);
    const emitirError = ref(null);
    async function cargar(tiendaId) {
        listLoading.value = true;
        listError.value = null;
        try {
            items.value = await felService.listarPorTienda(tiendaId);
        }
        catch (error) {
            listError.value = error instanceof ApiClientError ? error.message : 'No se pudo cargar la lista.';
        }
        finally {
            listLoading.value = false;
        }
    }
    async function emitir(tiendaId, ventaId) {
        emitirLoading.value = true;
        emitirError.value = null;
        try {
            const creado = await felService.emitir(tiendaId, ventaId);
            items.value = [...items.value, creado];
            return true;
        }
        catch (error) {
            emitirError.value = error instanceof ApiClientError ? error.message : 'No se pudo emitir el documento FEL.';
            return false;
        }
        finally {
            emitirLoading.value = false;
        }
    }
    async function reintentar(tiendaId, documento) {
        listError.value = null;
        try {
            const actualizado = await felService.reintentar(tiendaId, documento.id);
            items.value = items.value.map((d) => (d.id === documento.id ? actualizado : d));
        }
        catch (error) {
            listError.value = error instanceof ApiClientError ? error.message : 'No se pudo reintentar la certificación.';
        }
    }
    async function anular(tiendaId, documento, motivo) {
        listError.value = null;
        try {
            const actualizado = await felService.anular(tiendaId, documento.id, motivo);
            items.value = items.value.map((d) => (d.id === documento.id ? actualizado : d));
        }
        catch (error) {
            listError.value = error instanceof ApiClientError ? error.message : 'No se pudo anular el documento.';
        }
    }
    return { items, listLoading, listError, emitirLoading, emitirError, cargar, emitir, reintentar, anular };
}
