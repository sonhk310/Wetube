/**
 * Thông báo lỗi để hiện cho người dùng từ lỗi của RTK Query (`{ status, data: { message } }`) hoặc Error
 * thường. Backend trả `message` tiếng Việt (vd. 429 "Bạn bình luận quá nhanh...").
 */
export function apiErrorMessage(err: unknown, fallback: string): string {
  if (err && typeof err === "object") {
    const e = err as { status?: unknown; data?: unknown; message?: unknown };
    if (e.data && typeof e.data === "object") {
      const message = (e.data as { message?: unknown }).message;
      if (typeof message === "string" && message) return message;
    }
    if (e.status === 401) return "Bạn cần đăng nhập để tiếp tục.";
    if (e.status === 429) return "Bạn thao tác quá nhanh, vui lòng thử lại sau.";
    if (e.status === "FETCH_ERROR") return "Không kết nối được máy chủ, vui lòng thử lại.";
    if (err instanceof Error && err.message) return err.message;
  }
  return fallback;
}
