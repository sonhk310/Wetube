import { createContext, ReactNode, useState } from "react";

/**
 * Trạng thái trình phát dùng chung cho trang xem. Trình phát tự giữ trạng thái của nó;
 * ở đây chỉ còn giây đang xem (làm tròn, cập nhật mỗi giây) để hộp thoại Chia sẻ dùng cho "Bắt đầu tại".
 */
export type ControlsState = {
  seek: number;
  setSeek: (seek: number) => void;
};

export const ControlsContext = createContext<ControlsState | null>(null);

export function ControlsProvider({ children }: { children: ReactNode }) {
  const [seek, setSeek] = useState(0);

  return (
    <ControlsContext.Provider value={{ seek, setSeek }}>
      {children}
    </ControlsContext.Provider>
  );
}
