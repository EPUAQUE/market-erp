/**
 * Access token en memoria únicamente — nunca localStorage/sessionStorage. El
 * refresh token vive en una cookie HttpOnly que el backend gestiona; este
 * servicio nunca lo lee ni lo expone.
 */
let accessToken = null;
export const tokenService = {
    get() {
        return accessToken;
    },
    set(token) {
        accessToken = token;
    },
    clear() {
        accessToken = null;
    },
    hasToken() {
        return accessToken !== null;
    },
};
