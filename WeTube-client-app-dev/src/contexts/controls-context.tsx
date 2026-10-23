import { createContext, ReactNode, useCallback, useEffect, useMemo, useState } from "react";

/**
 * Trạng thái trình phát dùng chung cho trang xem. Trình phát tự giữ trạng thái của nó;
 * ở đây chỉ còn giây đang xem (làm tròn, cập nhật mỗi giây) để hộp thoại Chia sẻ dùng cho "Bắt đầu tại".
 */
export type ControlsState = {
  seek: number;
  setSeek: (seek: number) => void;
};

export const ControlsContext = createContext<ControlsState | null>(null);

/** Chế độ rạp, tách riêng để việc cập nhật `seek` mỗi giây không làm cả trang render lại. */
export type TheaterState = {
  theater: boolean;
  toggleTheater: () => void;
};

export const TheaterContext = createContext<TheaterState | null>(null);

const THEATER_KEY = "wetube_theater_mode";

export function ControlsProvider({ children }: { children: ReactNode }) {
  const [seek, setSeek] = useState(0);
  const [theater, setTheater] = useState(false);

  // Nhớ lựa chọn chế độ rạp giữa các lần xem (giống YouTube)
  useEffect(() => {
    try {
      setTheater(window.localStorage.getItem(THEATER_KEY) === "1");
    } catch {
      // localStorage bị chặn: dùng mặc định
    }
  }, []);

  const toggleTheater = useCallback(() => {
    setTheater((prev) => {
      try {
        window.localStorage.setItem(THEATER_KEY, prev ? "0" : "1");
      } catch {
        // bỏ qua
      }
      return !prev;
    });
  }, []);

  const controls = useMemo(() => ({ seek, setSeek }), [seek]);
  const theaterValue = useMemo(() => ({ theater, toggleTheater }), [theater, toggleTheater]);

  return (
    <TheaterContext.Provider value={theaterValue}>
      <ControlsContext.Provider value={controls}>{children}</ControlsContext.Provider>
    </TheaterContext.Provider>
  );
}
