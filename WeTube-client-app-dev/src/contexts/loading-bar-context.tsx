"use client";

import React, {
  createContext,
  useContext,
  useState,
  useCallback,
  useRef,
  ReactNode,
  useMemo,
} from "react";

interface LoadingBarContextType {
  start: () => void;
  finish: () => void;
  progress: number;
  isLoading: boolean;
}

const LoadingBarContext = createContext<LoadingBarContextType | null>(null);

/** Chuyển trang bị huỷ (URL không đổi) thì thanh tự xong sau chừng này, không chạy mãi. */
const MAX_LOADING_MS = 10_000;

export const useLoadingBar = () => {
  const context = useContext(LoadingBarContext);
  if (!context) {
    throw new Error("useLoadingBar must be used within a LoadingBarProvider");
  }
  return context;
};

export const LoadingBarProvider = ({ children }: { children: ReactNode }) => {
  const [isLoading, setIsLoading] = useState(false);
  const [progress, setProgress] = useState(0);

  const progressIntervalRef = useRef<NodeJS.Timeout | null>(null);
  // Các hẹn giờ của finish() / giới hạn MAX_LOADING_MS: start() mới phải huỷ chúng,
  // nếu không lượt finish() trước sẽ tắt thanh của lượt chuyển trang sau.
  const timeoutsRef = useRef<NodeJS.Timeout[]>([]);
  const activeRef = useRef(false);

  const clearTimers = useCallback(() => {
    if (progressIntervalRef.current) {
      clearInterval(progressIntervalRef.current);
      progressIntervalRef.current = null;
    }
    timeoutsRef.current.forEach(clearTimeout);
    timeoutsRef.current = [];
  }, []);

  const finish = useCallback(() => {
    if (!activeRef.current) return;
    activeRef.current = false;
    clearTimers();
    setProgress(100);
    timeoutsRef.current.push(
      setTimeout(() => {
        setIsLoading(false);
        timeoutsRef.current.push(setTimeout(() => setProgress(0), 200));
      }, 300)
    );
  }, [clearTimers]);

  const start = useCallback(() => {
    clearTimers();
    activeRef.current = true;
    setIsLoading(true);
    setProgress(0);

    progressIntervalRef.current = setInterval(() => {
      setProgress((prev) => {
        if (prev >= 95) return 95;

        let diff = 0;
        if (prev < 40) {
          diff = Math.random() * 10 + 5;
        } else if (prev < 80) {
          diff = Math.random() * 4 + 1;
        } else {
          diff = Math.random() * 2;
        }
        return Math.min(prev + diff, 95);
      });
    }, 100);
    timeoutsRef.current.push(setTimeout(finish, MAX_LOADING_MS));
  }, [clearTimers, finish]);

  const value = useMemo(
    () => ({
      start,
      finish,
      progress,
      isLoading,
    }),
    [start, finish, progress, isLoading]
  );

  return (
    <LoadingBarContext.Provider value={value}>
      {children}
    </LoadingBarContext.Provider>
  );
};
