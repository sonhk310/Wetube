import { Clapperboard, Crown, Radio, Captions, Users, type LucideIcon } from "lucide-react";

/** Quảng cáo nội bộ: WeTube tự giới thiệu tính năng của mình. Không mạng quảng cáo, không đo lượt hiển thị. */
export type Ad = {
  id: string;
  title: string;
  description: string;
  cta: string;
  href: string;
  icon: LucideIcon;
  /** Lớp nền gradient (viết trọn chuỗi để Tailwind nhận ra). */
  background: string;
};

export const ADS: Ad[] = [
  {
    id: "premium",
    title: "WeTube Premium",
    description: "Không quảng cáo, tải video về máy, phát nền trên điện thoại.",
    cta: "Xem gói Premium",
    href: "/otherservices/premium",
    icon: Crown,
    background: "bg-gradient-to-br from-amber-500 to-orange-600",
  },
  {
    id: "rooms",
    title: "Xem cùng bạn bè",
    description: "Tạo phòng, mời bạn bè vào và cùng xem một video, phát và tua đồng bộ.",
    cta: "Tạo phòng xem chung",
    href: "/rooms",
    icon: Users,
    background: "bg-gradient-to-br from-sky-500 to-indigo-600",
  },
  {
    id: "live",
    title: "Phát trực tiếp ngay trên trình duyệt",
    description: "Lên sóng bằng webcam hoặc OBS, người xem chat và bình chọn ngay trong buổi phát.",
    cta: "Bắt đầu phát trực tiếp",
    href: "/studio/live",
    icon: Radio,
    background: "bg-gradient-to-br from-rose-500 to-red-700",
  },
  {
    id: "subtitles",
    title: "Phụ đề AI cho video của bạn",
    description: "Tạo phụ đề tự động cho video đã tải lên trong WeTube Studio.",
    cta: "Mở Studio",
    href: "/studio",
    icon: Captions,
    background: "bg-gradient-to-br from-emerald-500 to-teal-700",
  },
  {
    id: "shorts",
    title: "Xem Shorts",
    description: "Lướt các video ngắn dọc, thích và bình luận ngay khi đang xem.",
    cta: "Xem Shorts",
    href: "/shorts",
    icon: Clapperboard,
    background: "bg-gradient-to-br from-fuchsia-500 to-purple-700",
  },
];

export function pickAd(): Ad {
  return ADS[Math.floor(Math.random() * ADS.length)];
}

// ---------- quảng cáo trước video (pre-roll) ----------

/** Một trình duyệt chỉ thấy quảng cáo trước video tối đa mỗi khoảng này. */
export const PREROLL_INTERVAL_MS = 15 * 60 * 1000;
/** Nút "Bỏ qua" hiện sau chừng này giây. */
export const PREROLL_SKIP_AFTER_S = 5;
/** Tự đóng sau chừng này giây. */
export const PREROLL_MAX_S = 10;

const PREROLL_KEY = "wetube_preroll_at";

/** Đã đủ 15 phút từ lần hiện trước. Đọc lỗi (localStorage bị chặn) thì coi như chưa hiện. */
export function prerollDue(now = Date.now()): boolean {
  try {
    const last = Number(window.localStorage.getItem(PREROLL_KEY));
    return !Number.isFinite(last) || now - last >= PREROLL_INTERVAL_MS;
  } catch {
    return true;
  }
}

export function markPrerollShown(now = Date.now()) {
  try {
    window.localStorage.setItem(PREROLL_KEY, String(now));
  } catch {
    // localStorage bị chặn: bỏ qua
  }
}

// Video do trình phát tự mở tiếp (danh sách phát, Tự phát ở màn hình hết video) thì không chen quảng cáo
let continuationVideoId: string | null = null;

export function markContinuation(videoId: string) {
  continuationVideoId = videoId;
}

/** Lấy và xoá dấu hiệu "video này là video tiếp theo của trình phát". */
export function consumeContinuation(videoId: string): boolean {
  const isContinuation = continuationVideoId === videoId;
  continuationVideoId = null;
  return isContinuation;
}
