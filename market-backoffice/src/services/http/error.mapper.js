export class ApiClientError extends Error {
    status;
    code;
    correlationId;
    retryAfterMs;
    isCanceled;
    constructor(params) {
        super(params.message);
        this.status = params.status;
        this.code = params.code;
        this.correlationId = params.correlationId;
        this.retryAfterMs = params.retryAfterMs;
        this.isCanceled = params.isCanceled ?? false;
    }
}
export function mapAxiosError(error) {
    if (error.code === 'ERR_CANCELED') {
        return new ApiClientError({
            message: 'Solicitud cancelada',
            status: 0,
            code: 'CANCELED',
            isCanceled: true,
        });
    }
    const body = error.response?.data;
    const retryAfterHeader = error.response?.headers?.['retry-after'];
    const retryAfterMs = retryAfterHeader ? Number(retryAfterHeader) * 1000 : undefined;
    return new ApiClientError({
        message: body?.message ?? 'Ocurrió un error de red inesperado.',
        status: error.response?.status ?? 0,
        code: body?.error ?? 'NETWORK_ERROR',
        correlationId: body?.correlationId,
        retryAfterMs,
    });
}
