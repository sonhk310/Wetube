/**
 * Thứ tự video của danh sách phát: sắp xếp / lọc ở trang danh sách, trộn bài, video kế tiếp khi phát.
 * Không import gì để chạy được bằng `node` khi kiểm tra.
 */

export type PlaylistSort = "added-desc" | "added-asc" | "popular" | "published-desc" | "published-asc";
export type PlaylistChip = "all" | "video" | "short";

export const PLAYLIST_SORT_LABELS: Record<PlaylistSort, string> = {
  "added-desc": "Ngày thêm (mới nhất)",
  "added-asc": "Ngày thêm (cũ nhất)",
  popular: "Phổ biến nhất",
  "published-desc": "Ngày xuất bản (mới nhất)",
  "published-asc": "Ngày xuất bản (cũ nhất)",
};

interface Arrangeable {
  addedAt: string | null;
  createdAt: string;
  totalView: number;
  isShort: boolean | null;
}

const time = (iso: string | null) => (iso ? new Date(iso).getTime() : 0);

/** Lọc theo chip rồi sắp xếp; trả mảng mới. isShort null tính là video thường. */
export function arrangeVideos<T extends Arrangeable>(videos: T[], sort: PlaylistSort, chip: PlaylistChip): T[] {
  const filtered = videos.filter((v) => chip === "all" || (chip === "short") === (v.isShort === true));
  const compare: Record<PlaylistSort, (a: T, b: T) => number> = {
    "added-desc": (a, b) => time(b.addedAt) - time(a.addedAt),
    "added-asc": (a, b) => time(a.addedAt) - time(b.addedAt),
    popular: (a, b) => b.totalView - a.totalView,
    "published-desc": (a, b) => time(b.createdAt) - time(a.createdAt),
    "published-asc": (a, b) => time(a.createdAt) - time(b.createdAt),
  };
  return [...filtered].sort(compare[sort]);
}

/** Xáo ngẫu nhiên (Fisher–Yates); firstId có trong danh sách thì đứng đầu. */
export function shuffleOrder(ids: string[], firstId?: string, random: () => number = Math.random): string[] {
  const rest = ids.filter((id) => id !== firstId);
  for (let i = rest.length - 1; i > 0; i--) {
    const j = Math.floor(random() * (i + 1));
    [rest[i], rest[j]] = [rest[j], rest[i]];
  }
  return firstId && ids.includes(firstId) ? [firstId, ...rest] : rest;
}

/** Video phát sau currentId; không có trong danh sách thì phát video đầu; hết danh sách thì về đầu nếu loop. */
export function nextInOrder(order: string[], currentId: string, loop: boolean): string | undefined {
  if (order.length === 0) return undefined;
  const i = order.indexOf(currentId);
  if (i < 0) return order[0];
  if (i < order.length - 1) return order[i + 1];
  return loop ? order[0] : undefined;
}

/** Video trước currentId; đang ở đầu hoặc không có trong danh sách thì không có. */
export function prevInOrder(order: string[], currentId: string): string | undefined {
  const i = order.indexOf(currentId);
  return i > 0 ? order[i - 1] : undefined;
}
