import type { AxiosError } from 'axios'

export class ApiClientError extends Error {
  readonly status: number
  readonly code: string
  readonly correlationId?: string
  readonly retryAfterMs?: number
  readonly isCanceled: boolean

  constructor(params: {
    message: string
    status: number
    code: string
    correlationId?: string
    retryAfterMs?: number
    isCanceled?: boolean
  }) {
    super(params.message)
    this.status = params.status
    this.code = params.code
    this.correlationId = params.correlationId
    this.retryAfterMs = params.retryAfterMs
    this.isCanceled = params.isCanceled ?? false
  }
}

interface ApiErrorBody {
  status: number
  error: string
  message: string
  correlationId: string
}

export function mapAxiosError(error: AxiosError): ApiClientError {
  if (error.code === 'ERR_CANCELED') {
    return new ApiClientError({
      message: 'Solicitud cancelada',
      status: 0,
      code: 'CANCELED',
      isCanceled: true,
    })
  }

  const body = error.response?.data as ApiErrorBody | undefined
  const retryAfterHeader = error.response?.headers?.['retry-after']
  const retryAfterMs = retryAfterHeader ? Number(retryAfterHeader) * 1000 : undefined

  return new ApiClientError({
    message: body?.message ?? 'Ocurrió un error de red inesperado.',
    status: error.response?.status ?? 0,
    code: body?.error ?? 'NETWORK_ERROR',
    correlationId: body?.correlationId,
    retryAfterMs,
  })
}
