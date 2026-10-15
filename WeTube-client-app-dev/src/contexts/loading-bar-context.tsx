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

  const clearTimers = useCallback(() => {
    if (progressIntervalRef.current) {
      clearInterval(progressIntervalRef.current);
      progressIntervalRef.current = null;
    }
  }, []);

  const finish = useCallback(() => {
    clearTimers();
    setProgress(100);
    setTimeout(() => {
      setIsLoading(false);
      setTimeout(() => setProgress(0), 200);
    }, 300); 
  }, [clearTimers]);

  const start = useCallback(() => {
    clearTimers();
    setIsLoading(true);
    setProgress(0);

    progressIntervalRef.current = setInterval(() => {
      setProgress((prev) => {
        if (prev >= 95) {
          clearTimers();
          return 95;
        }

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
  }, [clearTimers]);

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
