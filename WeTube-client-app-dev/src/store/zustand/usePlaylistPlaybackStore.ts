import { create } from "zustand";
import type { PlaylistDetail } from "@/modules/playlists/types";
import { shuffleOrder } from "@/lib/playlistOrder";

/**
 * Danh sách phát đang phát (`/watch/{id}?list=`): dữ liệu, thứ tự phát (theo trang danh sách lúc bấm,
 * hoặc đã trộn), lặp lại. Trang danh sách và khung danh sách trên trang xem dùng chung.
 */
interface PlaylistPlaybackState {
  detail: PlaylistDetail | null;
  order: string[];
  loop: boolean;
  shuffle: boolean;
  /** Bấm Phát tất cả / Trộn bài / một hàng ở trang danh sách: giữ đúng thứ tự đang hiển thị. */
  start: (detail: PlaylistDetail, order: string[], shuffle: boolean) => void;
  /** Mở link `?list=` khi chưa giữ danh sách này: thứ tự mặc định (Ngày thêm mới nhất), trộn nếu `shuffle`. */
  load: (detail: PlaylistDetail, shuffle: boolean) => void;
  setLoop: (on: boolean) => void;
  setShuffle: (on: boolean, currentId?: string) => void;
  removeVideo: (videoId: string) => void;
}

const defaultOrder = (detail: PlaylistDetail) => detail.videos.map((v) => v.videoId);

export const usePlaylistPlaybackStore = create<PlaylistPlaybackState>((set, get) => ({
  detail: null,
  order: [],
  loop: false,
  shuffle: false,
  start: (detail, order, shuffle) =>
    set((s) => ({
      detail,
      order,
      shuffle,
      loop: s.detail?.playlistId === detail.playlistId ? s.loop : false,
    })),
  load: (detail, shuffle) => {
    if (get().detail?.playlistId === detail.playlistId) return;
    const order = defaultOrder(detail);
    set({ detail, order: shuffle ? shuffleOrder(order) : order, shuffle, loop: false });
  },
  setLoop: (on) => set({ loop: on }),
  setShuffle: (on, currentId) =>
    set((s) => {
      if (!s.detail) return {};
      const order = defaultOrder(s.detail);
      return { shuffle: on, order: on ? shuffleOrder(order, currentId) : order };
    }),
  removeVideo: (videoId) =>
    set((s) =>
      s.detail
        ? {
            detail: {
              ...s.detail,
              videos: s.detail.videos.filter((v) => v.videoId !== videoId),
              totalVideos: Math.max(0, s.detail.totalVideos - 1),
            },
            order: s.order.filter((id) => id !== videoId),
          }
        : {}
    ),
}));
