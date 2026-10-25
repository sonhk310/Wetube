import { create } from "zustand";

/** Khung "Chương" ở cột phải trang xem: nút tên chương trên trình phát mở / đóng nó. */
interface ChaptersPanelState {
  open: boolean;
  toggle: () => void;
  close: () => void;
}

export const useChaptersPanelStore = create<ChaptersPanelState>((set) => ({
  open: false,
  toggle: () => set((s) => ({ open: !s.open })),
  close: () => set({ open: false }),
}));
