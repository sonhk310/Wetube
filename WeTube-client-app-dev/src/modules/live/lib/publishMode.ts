/** Kiểu phát từ trình duyệt: hình nào được đăng lên. Backend không cần biết nên chỉ nhớ ở trình duyệt, theo id buổi. */
export type PublishMode = "CAMERA" | "SCREEN" | "SCREEN_CAMERA" | "AUDIO";

export const DEFAULT_PUBLISH_MODE: PublishMode = "CAMERA";

const KEY_PREFIX = "wetube.live.mode.";

export const hasCamera = (mode: PublishMode) => mode === "CAMERA" || mode === "SCREEN_CAMERA";
export const hasScreen = (mode: PublishMode) => mode === "SCREEN" || mode === "SCREEN_CAMERA";

/** Trình duyệt này chia sẻ màn hình được không (điện thoại gần như không). */
export function canShareScreen(): boolean {
  return typeof navigator !== "undefined" && typeof navigator.mediaDevices?.getDisplayMedia === "function";
}

export function savePublishMode(liveId: string, mode: PublishMode): void {
  try {
    localStorage.setItem(KEY_PREFIX + liveId, mode);
  } catch {
    // Chặn lưu trữ (cửa sổ riêng tư...): lần sau về mặc định
  }
}

export function loadPublishMode(liveId: string): PublishMode {
  try {
    const saved = localStorage.getItem(KEY_PREFIX + liveId);
    if (saved === "CAMERA" || saved === "SCREEN" || saved === "SCREEN_CAMERA" || saved === "AUDIO") return saved;
  } catch {
    // như trên
  }
  return DEFAULT_PUBLISH_MODE;
}
