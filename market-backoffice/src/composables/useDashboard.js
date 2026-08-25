import { ref } from 'vue';
import { dashboardService } from '@/services/dashboard.service';
import { ApiClientError } from '@/services/http/ApiClient';
export function useDashboard() {
    const resumen = ref(null);
    const loading = ref(false);
    const error = ref(null);
    async function cargar(tiendaId) {
        loading.value = true;
        error.value = null;
        try {
            resumen.value = await dashboardService.obtenerResumen(tiendaId);
        }
        catch (err) {
            error.value = err instanceof ApiClientError ? err.message : 'No se pudo cargar el resumen.';
        }
        finally {
            loading.value = false;
        }
    }
    return { resumen, loading, error, cargar };
}
