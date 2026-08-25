import { useAuthStore } from '@/stores/auth.store';
import { usePermissionsStore } from '@/stores/permissions.store';
export const authGuard = async (to) => {
    const authStore = useAuthStore();
    if (to.meta.requiresAuth === false) {
        return true;
    }
    if (!authStore.isAuthenticated) {
        return { name: 'login', query: { redirect: to.fullPath } };
    }
    if (!authStore.authorizationLoaded) {
        try {
            await authStore.loadAuthorization();
        }
        catch {
            return { name: 'login' };
        }
    }
    if (to.meta.permission && !usePermissionsStore().can(to.meta.permission)) {
        return { name: 'forbidden' };
    }
    return true;
};
