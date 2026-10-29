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

// ---------- quảng cáo giữa video (mid-roll) ----------

/** Video ngắn hơn chừng này (giây) thì không chen quảng cáo giữa video. */
export const MIDROLL_MIN_DURATION_S = 30;
/** Cứ mỗi chừng này giây video thì thêm một quảng cáo (ít nhất 1). */
export const MIDROLL_EVERY_S = 180;
/** Tối đa bao nhiêu quảng cáo giữa video. */
export const MIDROLL_MAX = 3;

/** Bộ sinh số ngẫu nhiên có hạt giống (mulberry32): cùng hạt giống và độ dài thì cùng các điểm quảng cáo. */
function seededRandom(seed: number) {
  let a = Math.floor(seed * 2 ** 32) >>> 0;
  return () => {
    a = (a + 0x6d2b79f5) >>> 0;
    let t = a;
    t = Math.imul(t ^ (t >>> 15), t | 1);
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

/**
 * Các giây chen quảng cáo giữa video, tăng dần. Video được chia thành các đoạn bằng nhau (mỗi đoạn một quảng cáo),
 * điểm nằm ngẫu nhiên trong 60% giữa của đoạn: không bao giờ ở đầu hay cuối video và các quảng cáo cách xa nhau.
 */
export function midrollPoints(duration: number, seed: number): number[] {
  if (!Number.isFinite(duration) || duration < MIDROLL_MIN_DURATION_S) return [];
  const count = Math.min(MIDROLL_MAX, Math.max(1, Math.floor(duration / MIDROLL_EVERY_S)));
  const random = seededRandom(seed);
  const slot = duration / count;
  return Array.from({ length: count }, (_, i) => Math.round((slot * i + slot * (0.2 + 0.6 * random())) * 10) / 10);
}
