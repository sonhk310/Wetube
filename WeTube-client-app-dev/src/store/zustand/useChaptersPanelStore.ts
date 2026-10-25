import { create } from "zustand";

/** Khung "Chương" ở cột phải trang xem: nút tên chương trên trình phát mở, nút ✕ của khung đóng. */
interface ChaptersPanelState {
  open: boolean;
  show: () => void;
  close: () => void;
}

export const useChaptersPanelStore = create<ChaptersPanelState>((set) => ({
  open: false,
  show: () => set({ open: true }),
  close: () => set({ open: false }),
}));
