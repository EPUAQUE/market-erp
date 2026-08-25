/**
 * Access token en memoria únicamente — nunca localStorage/sessionStorage. El
 * refresh token vive en una cookie HttpOnly que el backend gestiona; este
 * servicio nunca lo lee ni lo expone.
 */
let accessToken: string | null = null

export const tokenService = {
  get(): string | null {
    return accessToken
  },
  set(token: string): void {
    accessToken = token
  },
  clear(): void {
    accessToken = null
  },
  hasToken(): boolean {
    return accessToken !== null
  },
}
