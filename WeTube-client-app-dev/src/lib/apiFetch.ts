import { refreshSession } from "@/lib/apiClient";

/**
 * fetch tới backend: luôn gửi kèm cookie đăng nhập (backend nằm ở subdomain khác với client).
 * Gặp 401 (access token hết hạn sau 1 giờ) thì làm mới phiên rồi gửi lại một lần, giống apiClient.
 */
export async function apiFetch(input: RequestInfo | URL, init: RequestInit = {}) {
  // Request (vd. của RTK Query) chỉ đọc body được một lần: giữ bản sao để gửi lại
  const retryInput = input instanceof Request ? input.clone() : input;
  const res = await fetch(input, { ...init, credentials: "include" });
  if (res.status !== 401 || urlOf(input).includes("/auth/")) return res;
  if (!(await refreshSession())) return res;
  return fetch(retryInput, { ...init, credentials: "include" });
}

function urlOf(input: RequestInfo | URL) {
  if (typeof input === "string") return input;
  return input instanceof URL ? input.href : input.url;
}
