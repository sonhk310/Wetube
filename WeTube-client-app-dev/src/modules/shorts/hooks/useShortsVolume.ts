"use client";

import { create } from "zustand";

const STORAGE_KEY = "wetube_shorts_volume";

interface ShortsVolumeState {
  volume: number;
  muted: boolean;
  setVolume: (volume: number) => void;
  setMuted: (muted: boolean) => void;
}

function read(): { volume: number; muted: boolean } {
  try {
    const saved = JSON.parse(window.localStorage.getItem(STORAGE_KEY) ?? "null");
    if (saved && typeof saved.volume === "number" && saved.volume >= 0 && saved.volume <= 1) {
      return { volume: saved.volume, muted: Boolean(saved.muted) };
    }
  } catch {
    // localStorage bị chặn hoặc chạy phía server: dùng mặc định
  }
  // Mặc định tắt tiếng như YouTube Shorts (trình duyệt cũng chặn tự phát có tiếng)
  return { volume: 1, muted: true };
}

function save(volume: number, muted: boolean) {
  try {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify({ volume, muted }));
  } catch {
    // bỏ qua
  }
}

/** Âm lượng / tắt tiếng dùng chung cho mọi Short và nhớ giữa các lần mở. */
export const useShortsVolume = create<ShortsVolumeState>((set, get) => ({
  ...(typeof window === "undefined" ? { volume: 1, muted: true } : read()),
  setVolume: (volume) => {
    // Kéo âm lượng lên thì bật tiếng, kéo về 0 thì tắt
    const muted = volume === 0;
    set({ volume, muted });
    save(volume, muted);
  },
  setMuted: (muted) => {
    set({ muted });
    save(get().volume, muted);
  },
}));
