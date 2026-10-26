import { API_PREFIX } from "@/constants/appConstant";
import { apiFetch } from "@/lib/apiFetch";
import { toastEmitter } from "@/lib/toastEmitter";

/** Người tạo phòng thấy hộp thoại "Mời bạn bè" ngay khi vào phòng (cờ này được phòng đọc và xoá). */
export const inviteFlagKey = (code: string) => `wetube_room_invite_${code}`;

/**
 * Tạo phòng xem chung (tuỳ chọn: bắt đầu bằng video và giây đang xem); trả mã phòng, hoặc null nếu lỗi (đã hiện toast).
 */
export async function createRoom(videoId?: string, position?: number): Promise<string | null> {
  try {
    const res = await apiFetch(`${API_PREFIX}/rooms`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ videoId, t: position }),
    });
    if (res.status === 401) {
      toastEmitter.error("Hãy đăng nhập để tạo phòng xem chung");
      return null;
    }
    if (!res.ok) {
      const body = await res.json().catch(() => null);
      toastEmitter.error(body?.message ?? "Không tạo được phòng, vui lòng thử lại");
      return null;
    }
    const { code } = (await res.json()) as { code: string };
    try {
      window.sessionStorage.setItem(inviteFlagKey(code), "1");
    } catch {
      // sessionStorage bị chặn: chỉ không tự mở hộp thoại mời
    }
    return code;
  } catch {
    toastEmitter.error("Không tạo được phòng, vui lòng thử lại");
    return null;
  }
}

export const roomUrl = (code: string) => `${window.location.origin}/rooms/${code}`;
