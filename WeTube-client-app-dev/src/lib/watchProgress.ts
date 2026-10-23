"use client";

import { useEffect } from "react";
import { create } from "zustand";
import { API_PREFIX } from "@/constants/appConstant";
import { apiFetch } from "@/lib/apiFetch";
import { useAuth } from "@/contexts/auth-context";

/**
 * Vị trí đã xem (giây) của người dùng đang đăng nhập, theo id video.
 * Thumbnail gọi useWatchProgress(id); các id được gom lại và tải bằng một request cho cả trang.
 */
interface WatchProgressState {
  progress: Record<string, number>;
  setProgress: (videoId: string, seconds: number) => void;
  merge: (values: Record<string, number>) => void;
  reset: () => void;
}

export const useWatchProgressStore = create<WatchProgressState>((set) => ({
  progress: {},
  setProgress: (videoId, seconds) =>
    set((s) => ({ progress: { ...s.progress, [videoId]: seconds } })),
  merge: (values) => set((s) => ({ progress: { ...s.progress, ...values } })),
  reset: () => set({ progress: {} }),
}));

const BATCH_DELAY_MS = 40;
const MAX_IDS_PER_REQUEST = 100;

let loadedForUser: string | null = null;
const requested = new Set<string>();
const queue = new Set<string>();
let timer: ReturnType<typeof setTimeout> | null = null;

async function flushQueue() {
  timer = null;
  const ids = [...queue];
  queue.clear();
  for (let i = 0; i < ids.length; i += MAX_IDS_PER_REQUEST) {
    const chunk = ids.slice(i, i + MAX_IDS_PER_REQUEST);
    try {
      const res = await apiFetch(`${API_PREFIX}/videos/progress?ids=${chunk.join(",")}`);
      if (res.ok) useWatchProgressStore.getState().merge(await res.json());
    } catch (err) {
      console.error("Không tải được vị trí đã xem:", err);
    }
  }
}

function requestProgress(userId: string, videoId: string) {
  if (loadedForUser !== userId) {
    // Đổi tài khoản: bỏ dữ liệu của người trước
    loadedForUser = userId;
    requested.clear();
    useWatchProgressStore.getState().reset();
  }
  if (requested.has(videoId)) return;
  requested.add(videoId);
  queue.add(videoId);
  if (!timer) timer = setTimeout(flushQueue, BATCH_DELAY_MS);
}

/** Giây đã xem của video (undefined nếu chưa xem hoặc chưa đăng nhập). */
export function useWatchProgress(videoId?: string): number | undefined {
  const { user } = useAuth();
  const userId = user?.sub;
  const seconds = useWatchProgressStore((s) => (videoId ? s.progress[videoId] : undefined));

  useEffect(() => {
    if (userId && videoId) requestProgress(userId, videoId);
  }, [userId, videoId]);

  return userId ? seconds : undefined;
}

/**
 * Gửi vị trí đang xem. Tham số để trên URL, không body / header riêng, để trình duyệt gửi được
 * cả khi đóng tab (keepalive). final = ghi ngay vào lịch sử (dừng / hết video / rời trang).
 */
export function saveWatchProgress(videoId: string, seconds: number, final: boolean) {
  const t = Math.max(0, Math.round(seconds * 10) / 10);
  useWatchProgressStore.getState().setProgress(videoId, t);
  apiFetch(`${API_PREFIX}/videos/${videoId}/progress?t=${t}&final=${final}`, {
    method: "POST",
    keepalive: true,
  }).catch((err) => console.error("Không lưu được vị trí đang xem:", err));
}
