import { ref } from 'vue';
import { tiendasService } from '@/services/tiendas.service';
import { ApiClientError } from '@/services/http/ApiClient';
export function useTiendas() {
    const items = ref([]);
    const listLoading = ref(false);
    const listError = ref(null);
    const saveLoading = ref(false);
    const saveError = ref(null);
    async function cargar() {
        listLoading.value = true;
        listError.value = null;
        try {
            items.value = await tiendasService.listar();
        }
        catch (error) {
            listError.value = error instanceof ApiClientError ? error.message : 'No se pudo cargar la lista.';
        }
        finally {
            listLoading.value = false;
        }
    }
    async function crear(codigo, datos) {
        saveLoading.value = true;
        saveError.value = null;
        try {
            const creada = await tiendasService.crear(codigo, datos);
            items.value = [...items.value, creada];
            return true;
        }
        catch (error) {
            saveError.value = error instanceof ApiClientError ? error.message : 'No se pudo crear la tienda.';
            return false;
        }
        finally {
            saveLoading.value = false;
        }
    }
    async function actualizar(id, datos) {
        saveLoading.value = true;
        saveError.value = null;
        try {
            const actualizada = await tiendasService.actualizar(id, datos);
            items.value = items.value.map((t) => (t.id === id ? actualizada : t));
            return true;
        }
        catch (error) {
            saveError.value = error instanceof ApiClientError ? error.message : 'No se pudo actualizar la tienda.';
            return false;
        }
        finally {
            saveLoading.value = false;
        }
    }
    async function alternarEstado(tienda) {
        try {
            if (tienda.estado === 'ACTIVA') {
                await tiendasService.desactivar(tienda.id);
                tienda.estado = 'INACTIVA';
            }
            else {
                await tiendasService.activar(tienda.id);
                tienda.estado = 'ACTIVA';
            }
        }
        catch (error) {
            listError.value = error instanceof ApiClientError ? error.message : 'No se pudo cambiar el estado.';
        }
    }
    return { items, listLoading, listError, saveLoading, saveError, cargar, crear, actualizar, alternarEstado };
}
