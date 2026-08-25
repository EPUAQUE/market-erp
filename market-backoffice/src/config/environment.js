function required(name) {
    const value = import.meta.env[name];
    if (!value) {
        throw new Error(`Falta la variable de entorno ${name} (ver .env.example)`);
    }
    return value;
}
export const environment = {
    apiBaseUrl: required('VITE_API_BASE_URL'),
    apiTimeout: Number(import.meta.env.VITE_API_TIMEOUT ?? 15000),
};
