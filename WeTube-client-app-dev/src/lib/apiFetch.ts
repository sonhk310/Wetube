/**
 * fetch tới backend: luôn gửi kèm cookie đăng nhập (backend nằm ở subdomain khác với client).
 */
export function apiFetch(input: RequestInfo | URL, init: RequestInit = {}) {
  return fetch(input, { ...init, credentials: "include" });
}
