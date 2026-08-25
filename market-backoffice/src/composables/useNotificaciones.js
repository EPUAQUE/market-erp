import { ref } from 'vue';
import { notificacionesService } from '@/services/notificaciones.service';
import { ApiClientError } from '@/services/http/ApiClient';
export function useNotificaciones() {
    const items = ref([]);
    const listLoading = ref(false);
    const listError = ref(null);
    const generarLoading = ref(false);
    async function cargar(tiendaId) {
        listLoading.value = true;
        listError.value = null;
        try {
            items.value = await notificacionesService.listarPorTienda(tiendaId);
        }
        catch (error) {
            listError.value = error instanceof ApiClientError ? error.message : 'No se pudo cargar la lista.';
        }
        finally {
            listLoading.value = false;
        }
    }
    async function generar(tiendaId) {
        generarLoading.value = true;
        listError.value = null;
        try {
            await notificacionesService.generar(tiendaId);
            await cargar(tiendaId);
        }
        catch (error) {
            listError.value = error instanceof ApiClientError ? error.message : 'No se pudo generar notificaciones.';
        }
        finally {
            generarLoading.value = false;
        }
    }
    async function marcarLeida(tiendaId, notificacion) {
        listError.value = null;
        try {
            const actualizada = await notificacionesService.marcarLeida(tiendaId, notificacion.id);
            items.value = items.value.map((n) => (n.id === notificacion.id ? actualizada : n));
        }
        catch (error) {
            listError.value = error instanceof ApiClientError ? error.message : 'No se pudo marcar como leída.';
        }
    }
    return { items, listLoading, listError, generarLoading, cargar, generar, marcarLeida };
}
