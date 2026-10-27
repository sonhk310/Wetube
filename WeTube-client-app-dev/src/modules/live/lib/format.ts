/** "1:02:03" / "12:05" từ hai mốc ISO; null nếu thiếu mốc hoặc âm. */
export function formatLiveDuration(startIso: string | null, endIso: string | null): string | null {
  if (!startIso || !endIso) return null;
  const seconds = Math.floor((new Date(endIso).getTime() - new Date(startIso).getTime()) / 1000);
  if (!Number.isFinite(seconds) || seconds < 0) return null;
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  const s = seconds % 60;
  const pad = (n: number) => String(n).padStart(2, "0");
  return h > 0 ? `${h}:${pad(m)}:${pad(s)}` : `${m}:${pad(s)}`;
}

/** "1:02:03" / "12:05" từ số mili-giây đã trôi qua (đồng hồ của buổi đang phát). */
export function formatElapsed(ms: number): string {
  const seconds = Math.max(0, Math.floor(ms / 1000));
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  const s = seconds % 60;
  const pad = (n: number) => String(n).padStart(2, "0");
  return h > 0 ? `${h}:${pad(m)}:${pad(s)}` : `${m}:${pad(s)}`;
}

/** Địa chỉ công khai của trang xem một buổi phát (dùng cho chia sẻ). */
export function liveUrl(id: string): string {
  const origin = typeof window !== "undefined" ? window.location.origin : "";
  return `${origin}/live/${id}`;
}
