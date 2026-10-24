import { create } from "zustand";

/** Chung giữa danh sách lịch sử xem và thanh bên của trang Lịch sử. */
interface HistoryState {
  query: string;
  setQuery: (query: string) => void;
  /** Tăng mỗi lần xoá hết lịch sử, để danh sách tự làm trống. */
  clearedCount: number;
  markCleared: () => void;
}

export const useHistoryStore = create<HistoryState>((set) => ({
  query: "",
  setQuery: (query) => set({ query }),
  clearedCount: 0,
  markCleared: () => set((s) => ({ clearedCount: s.clearedCount + 1 })),
}));
