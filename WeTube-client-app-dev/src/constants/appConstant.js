// export const API_PREFIX = "http://localhost:8080/api";
// export const WS_PREFIX = "http://localhost:8080/ws";

const DEFAULT_API_BASE = "https://wetube-service.vibe.tinhgon.xyz";
const API_BASE = process.env.NEXT_PUBLIC_API_BASE || DEFAULT_API_BASE;

export const API_PREFIX = process.env.NEXT_PUBLIC_API_URL || `${API_BASE}/api`;
export const WS_PREFIX = `${API_BASE}/ws`;
