import { ref } from 'vue';
import { comprasService } from '@/services/compras.service';
import { ApiClientError } from '@/services/http/ApiClient';
export function useCompras() {
    const items = ref([]);
    const listLoading = ref(false);
    const listError = ref(null);
    const saveLoading = ref(false);
    const saveError = ref(null);
    async function cargar(tiendaId) {
        listLoading.value = true;
        listError.value = null;
        try {
            items.value = await comprasService.listarPorTienda(tiendaId);
        }
        catch (error) {
            listError.value = error instanceof ApiClientError ? error.message : 'No se pudo cargar la lista.';
        }
        finally {
            listLoading.value = false;
        }
    }
    async function crear(tiendaId, proveedorId, lineas) {
        saveLoading.value = true;
        saveError.value = null;
        try {
            const creada = await comprasService.crear(tiendaId, proveedorId, lineas);
            items.value = [...items.value, creada];
            return true;
        }
        catch (error) {
            saveError.value = error instanceof ApiClientError ? error.message : 'No se pudo crear la compra.';
            return false;
        }
        finally {
            saveLoading.value = false;
        }
    }
    async function recibir(tiendaId, compra) {
        listError.value = null;
        try {
            const actualizada = await comprasService.recibir(tiendaId, compra.id);
            items.value = items.value.map((c) => (c.id === compra.id ? actualizada : c));
        }
        catch (error) {
            listError.value = error instanceof ApiClientError ? error.message : 'No se pudo recibir la compra.';
        }
    }
    async function anular(tiendaId, compra) {
        listError.value = null;
        try {
            const actualizada = await comprasService.anular(tiendaId, compra.id);
            items.value = items.value.map((c) => (c.id === compra.id ? actualizada : c));
        }
        catch (error) {
            listError.value = error instanceof ApiClientError ? error.message : 'No se pudo anular la compra.';
        }
    }
    return { items, listLoading, listError, saveLoading, saveError, cargar, crear, recibir, anular };
}
