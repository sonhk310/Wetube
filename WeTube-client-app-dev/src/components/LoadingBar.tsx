"use client";

import React from "react";
import { useLoadingBar } from "@/contexts/loading-bar-context";

export const LoadingBar: React.FC = () => {
  const { isLoading, progress } = useLoadingBar();

  return (
    <div
      className="fixed top-0 left-0 right-0 h-[3px] z-[9999]"
      style={{
        opacity: isLoading ? 1 : 0,
        transition: `opacity 200ms linear`,
      }}
    >
      <div
        className="h-full bg-red-600"
        style={{
          width: `${progress}%`,
          transition: `width 200ms linear`,
          boxShadow: "0 0 10px rgba(239, 68, 68, 0.7)",
        }}
      />
    </div>
  );
};
