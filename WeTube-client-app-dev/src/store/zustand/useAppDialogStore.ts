import { create } from "zustand";

export interface AppDialogRequest {
  kind: "confirm" | "notice";
  title: string;
  message?: string;
  /** Nhãn nút chính (xác nhận / đóng). */
  confirmLabel: string;
  cancelLabel: string;
  /** Hành động phá huỷ (xoá, ẩn...): nút chính màu đỏ. */
  destructive: boolean;
  resolve: (ok: boolean) => void;
}

interface AppDialogState {
  /** Hộp thoại đang mở là phần tử đầu; yêu cầu đến sau xếp hàng chờ. */
  queue: AppDialogRequest[];
  push: (request: AppDialogRequest) => void;
  /** Đóng hộp thoại đang mở và trả kết quả cho nơi gọi. */
  settle: (ok: boolean) => void;
}

export const useAppDialogStore = create<AppDialogState>((set, get) => ({
  queue: [],
  push: (request) => set((s) => ({ queue: [...s.queue, request] })),
  settle: (ok) => {
    const [current, ...rest] = get().queue;
    if (!current) return;
    set({ queue: rest });
    current.resolve(ok);
  },
}));
