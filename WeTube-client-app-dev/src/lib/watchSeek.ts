/** Sự kiện yêu cầu trình phát trên trang xem tua tới một giây (detail: số giây). */
export const SEEK_EVENT = "wetube:seek";

/** Tua trình phát đang mở tới `seconds`, phát tiếp và cuộn lên trình phát (như bấm mốc thời gian trên YouTube). */
export function requestSeek(seconds: number) {
  window.dispatchEvent(new CustomEvent<number>(SEEK_EVENT, { detail: seconds }));
  window.scrollTo({ top: 0, behavior: "smooth" });
}
