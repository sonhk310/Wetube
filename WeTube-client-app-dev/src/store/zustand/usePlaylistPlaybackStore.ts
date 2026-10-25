import { create } from "zustand";
import type { PlaylistDetail } from "@/modules/playlists/types";
import { shuffleOrder } from "@/lib/playlistOrder";

/**
 * Danh sách phát đang phát (`/watch/{id}?list=`): dữ liệu, thứ tự phát (theo trang danh sách lúc bấm,
 * hoặc đã trộn), lặp lại. Trang danh sách và khung danh sách trên trang xem dùng chung.
 */
interface PlaylistPlaybackState {
  detail: PlaylistDetail | null;
  /** Thứ tự đã chọn (sắp xếp + chip ở trang danh sách); trộn bài xáo từ đây, tắt trộn thì về lại đây. */
  baseOrder: string[];
  /** Thứ tự đang phát. */
  order: string[];
  loop: boolean;
  shuffle: boolean;
  /** Bấm Phát tất cả / Trộn bài / một hàng ở trang danh sách: `baseOrder` là thứ tự đang hiển thị. */
  start: (detail: PlaylistDetail, baseOrder: string[], shuffle: boolean, firstId?: string) => void;
  /**
   * Mở link `?list=` khi chưa giữ danh sách này: thứ tự mặc định (Ngày thêm mới nhất), trộn nếu `shuffle`
   * (video đang xem `firstId` đứng đầu).
   */
  load: (detail: PlaylistDetail, shuffle: boolean, firstId?: string) => void;
  setLoop: (on: boolean) => void;
  setShuffle: (on: boolean, currentId?: string) => void;
  removeVideo: (videoId: string) => void;
}

export const usePlaylistPlaybackStore = create<PlaylistPlaybackState>((set, get) => ({
  detail: null,
  baseOrder: [],
  order: [],
  loop: false,
  shuffle: false,
  start: (detail, baseOrder, shuffle, firstId) =>
    set((s) => ({
      detail,
      baseOrder,
      order: shuffle ? shuffleOrder(baseOrder, firstId) : baseOrder,
      shuffle,
      loop: s.detail?.playlistId === detail.playlistId ? s.loop : false,
    })),
  load: (detail, shuffle, firstId) => {
    if (get().detail?.playlistId === detail.playlistId) return;
    const baseOrder = detail.videos.map((v) => v.videoId);
    set({ detail, baseOrder, order: shuffle ? shuffleOrder(baseOrder, firstId) : baseOrder, shuffle, loop: false });
  },
  setLoop: (on) => set({ loop: on }),
  setShuffle: (on, currentId) =>
    set((s) => (s.detail ? { shuffle: on, order: on ? shuffleOrder(s.baseOrder, currentId) : s.baseOrder } : {})),
  removeVideo: (videoId) =>
    set((s) =>
      s.detail
        ? {
            detail: {
              ...s.detail,
              videos: s.detail.videos.filter((v) => v.videoId !== videoId),
              totalVideos: Math.max(0, s.detail.totalVideos - 1),
            },
            baseOrder: s.baseOrder.filter((id) => id !== videoId),
            order: s.order.filter((id) => id !== videoId),
          }
        : {}
    ),
}));
