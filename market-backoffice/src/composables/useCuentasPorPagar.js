import { ref } from 'vue';
import { cuentasPorPagarService } from '@/services/cuentasPorPagar.service';
import { ApiClientError } from '@/services/http/ApiClient';
export function useCuentasPorPagar() {
    const items = ref([]);
    const listLoading = ref(false);
    const listError = ref(null);
    async function cargar(tiendaId) {
        listLoading.value = true;
        listError.value = null;
        try {
            items.value = await cuentasPorPagarService.listarPorTienda(tiendaId);
        }
        catch (error) {
            listError.value = error instanceof ApiClientError ? error.message : 'No se pudo cargar la lista.';
        }
        finally {
            listLoading.value = false;
        }
    }
    async function registrarPago(tiendaId, cuenta, monto) {
        listError.value = null;
        try {
            const actualizada = await cuentasPorPagarService.registrarPago(tiendaId, cuenta.id, monto);
            items.value = items.value.map((c) => (c.id === cuenta.id ? actualizada : c));
            return true;
        }
        catch (error) {
            listError.value = error instanceof ApiClientError ? error.message : 'No se pudo registrar el pago.';
            return false;
        }
    }
    async function anular(tiendaId, cuenta) {
        listError.value = null;
        try {
            const actualizada = await cuentasPorPagarService.anular(tiendaId, cuenta.id);
            items.value = items.value.map((c) => (c.id === cuenta.id ? actualizada : c));
        }
        catch (error) {
            listError.value = error instanceof ApiClientError ? error.message : 'No se pudo anular la cuenta.';
        }
    }
    return { items, listLoading, listError, cargar, registrarPago, anular };
}
