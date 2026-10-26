/**
 * Tính toán đồng bộ cho phòng xem chung. Hàm thuần, không import gì, để chạy được bằng `node` khi kiểm tra
 * (xem scripts/check-roomSync.ts).
 *
 * Server giữ trạng thái gốc { playing, position (giây tại updatedAt), updatedAt (giờ server, ms) }.
 * Mỗi máy tự tính vị trí đúng = position + (giờ server hiện tại - updatedAt), rồi sửa lệch nếu cần.
 */

export interface PlaybackState {
  videoId?: string | null;
  playing: boolean;
  position: number;
  updatedAt: number;
  version: number;
}

/** Vị trí (giây) mà mọi người trong phòng đáng ra phải đang ở. */
export function expectedPosition(state: PlaybackState, serverNow: number): number {
  if (!state.playing) return state.position;
  return state.position + Math.max(0, serverNow - state.updatedAt) / 1000;
}

export interface ClockSample {
  clientSent: number;
  serverTime: number;
  clientReceived: number;
}

/**
 * Độ lệch đồng hồ (ms): giờ server ≈ giờ máy + offset. Mỗi mẫu giả định tin đi và về mất như nhau;
 * lấy mẫu có thời gian khứ hồi ngắn nhất vì nó ít sai nhất (như NTP).
 */
export function estimateOffset(samples: ClockSample[]): number {
  let best: { rtt: number; offset: number } | null = null;
  for (const s of samples) {
    const rtt = s.clientReceived - s.clientSent;
    if (!Number.isFinite(rtt) || rtt < 0) continue;
    const offset = s.serverTime - (s.clientSent + rtt / 2);
    if (!best || rtt < best.rtt) best = { rtt, offset };
  }
  return best ? best.offset : 0;
}

/** Lệch trên mức này thì tua thẳng tới vị trí đúng (giây). */
export const SEEK_THRESHOLD = 0.3;
/** Lệch trong khoảng (MIN_DRIFT, SEEK_THRESHOLD] thì chỉnh tốc độ phát nhẹ cho khớp (giây). */
export const MIN_DRIFT = 0.08;
/** Chỉnh tốc độ 5%; lệch hơn FAST_DRIFT thì chỉnh 10% cho kịp nhanh hơn. */
export const RATE_ADJUST = 0.05;
export const RATE_ADJUST_FAST = 0.1;
export const FAST_DRIFT = 0.2;
/** Ngay sau khi video bắt đầu chạy (vừa tải xong, vừa phát lại) dữ liệu đã có sẵn nên tua rẻ: ngưỡng tua thấp hơn. */
export const RESUME_SEEK_THRESHOLD = 0.15;

export type Correction =
  | { kind: "none" }
  | { kind: "seek"; to: number }
  | { kind: "rate"; rate: number };

/**
 * Máy này đang ở `actual`, đáng ra ở `expected`. Đang tạm dừng thì chỉ sửa khi lệch hẳn (không chỉnh tốc độ).
 * actual > expected nghĩa là phát nhanh hơn mọi người: giảm tốc độ.
 */
export function correction(
  actual: number,
  expected: number,
  playing: boolean,
  seekThreshold: number = SEEK_THRESHOLD
): Correction {
  const drift = actual - expected;
  const size = Math.abs(drift);
  if (size > seekThreshold) return { kind: "seek", to: Math.max(0, expected) };
  if (!playing) return size > MIN_DRIFT ? { kind: "seek", to: Math.max(0, expected) } : { kind: "none" };
  if (size > MIN_DRIFT) {
    const adjust = size > FAST_DRIFT ? RATE_ADJUST_FAST : RATE_ADJUST;
    return { kind: "rate", rate: drift > 0 ? 1 - adjust : 1 + adjust };
  }
  return { kind: "none" };
}

/** "1:02:03" / "3:20" */
export function formatClock(seconds: number): string {
  const total = Math.max(0, Math.floor(seconds));
  const h = Math.floor(total / 3600);
  const m = Math.floor((total % 3600) / 60);
  const s = total % 60;
  const pad = (n: number) => String(n).padStart(2, "0");
  return h > 0 ? `${h}:${pad(m)}:${pad(s)}` : `${m}:${pad(s)}`;
}

/** Lấy mã phòng từ mã trần hoặc link mời; null nếu không giống mã phòng. */
export function parseRoomCode(input: string): string | null {
  const text = input.trim();
  const fromLink = text.match(/\/rooms\/([A-Za-z0-9-]{1,20})(?:[/?#]|$)/);
  const candidate = fromLink ? fromLink[1] : text;
  return /^[A-Za-z0-9]{3}-[A-Za-z0-9]{4}-[A-Za-z0-9]{3}$/.test(candidate) ? candidate : null;
}
