import { formatDuration } from "@/lib/utils";

/** Giống backend ShortsRule: dài tối đa 180 giây và khung vuông hoặc dọc. */
export const SHORTS_MAX_SECONDS = 180;

export function isShortVideo(duration: number, width: number, height: number): boolean {
  return duration > 0 && duration <= SHORTS_MAX_SECONDS && width > 0 && height > 0 && width <= height;
}

/** Lý do ngắn gọn để hiện cạnh nhãn Shorts / Video thường, vd. "Dọc · 0:45", "Ngang". */
export function shortsReason(duration: number, width: number, height: number): string {
  if (!(width > 0 && height > 0)) return "Chưa đọc được kích thước video";
  if (width > height) return "Video ngang";
  if (duration > SHORTS_MAX_SECONDS) return "Dài hơn 3 phút";
  return `${width === height ? "Vuông" : "Dọc"} · ${formatDuration(duration)}`;
}
