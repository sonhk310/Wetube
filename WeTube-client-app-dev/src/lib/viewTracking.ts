"use client";

import { useEffect, useRef } from "react";
import { API_PREFIX } from "@/constants/appConstant";
import { apiFetch } from "@/lib/apiFetch";

const ANON_ID_KEY = "wetube_anon_id";

/** Mã ẩn danh của trình duyệt, để backend không đếm trùng lượt xem của khách chưa đăng nhập. */
export function getAnonId(): string {
  try {
    let id = window.localStorage.getItem(ANON_ID_KEY);
    if (!id) {
      id =
        typeof crypto !== "undefined" && "randomUUID" in crypto
          ? crypto.randomUUID()
          : `${Date.now()}-${Math.random().toString(36).slice(2)}`;
      window.localStorage.setItem(ANON_ID_KEY, id);
    }
    return id;
  } catch {
    return "";
  }
}

/** Ghi nhận 1 lượt xem; backend tự bỏ qua nếu người này đã xem video trong 30 phút gần đây. */
export async function registerView(videoId: string): Promise<void> {
  try {
    await apiFetch(`${API_PREFIX}/videos/${videoId}/view`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ anonId: getAnonId() }),
    });
  } catch (err) {
    console.error("Không ghi nhận được lượt xem:", err);
  }
}

/**
 * Khách chưa đăng nhập: gửi vị trí đang xem để tính thời gian xem cho chủ kênh (không lưu lịch sử).
 * Tham số trên URL để gửi được cả khi đóng tab (keepalive), giống saveWatchProgress.
 */
export function saveAnonWatchTime(videoId: string, seconds: number) {
  const t = Math.max(0, Math.round(seconds * 10) / 10);
  apiFetch(`${API_PREFIX}/videos/${videoId}/watch-time?t=${t}&anonId=${encodeURIComponent(getAnonId())}`, {
    method: "POST",
    keepalive: true,
  }).catch((err) => console.error("Không gửi được thời gian xem:", err));
}

/**
 * Đếm 1 lượt xem sau khi video đã chạy liên tục `delayMs` mili giây (active=true).
 * Tạm dừng / chuyển video sẽ huỷ bộ đếm; mỗi video chỉ gửi tối đa một lần cho mỗi lần mở trang.
 */
export function useViewCounter(videoId: string | undefined, active: boolean, delayMs: number) {
  const countedFor = useRef<string | null>(null);

  useEffect(() => {
    if (!videoId || !active || countedFor.current === videoId) return;
    const timer = setTimeout(() => {
      countedFor.current = videoId;
      void registerView(videoId);
    }, delayMs);
    return () => clearTimeout(timer);
  }, [videoId, active, delayMs]);
}
