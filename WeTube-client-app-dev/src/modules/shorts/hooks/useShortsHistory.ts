"use client";

import { useEffect, useRef, type RefObject } from "react";
import { useAuth } from "@/contexts/auth-context";
import { saveAnonWatchTime } from "@/lib/viewTracking";
import { saveWatchProgress } from "@/lib/watchProgress";

/** Xem đủ ngần này giây mới tính là đã xem (giống trình phát trang xem). */
const MIN_SECONDS = 3;

/**
 * Lưu Short đang xem vào lịch sử: gửi một lần sau 3 giây, và một lần cuối khi rời Short
 * (vuốt sang Short khác, rời trang, ẩn / đóng tab). Khách chưa đăng nhập chỉ gửi thời gian xem cho chủ kênh.
 */
export function useShortsHistory(
  videoId: string,
  active: boolean,
  videoRef: RefObject<HTMLVideoElement | null>
) {
  const { user } = useAuth();
  const userId = user?.sub;
  const lastTime = useRef(0);
  const lastSent = useRef(-1);

  useEffect(() => {
    if (!active) return;
    lastTime.current = 0;
    lastSent.current = -1;

    const report = (final: boolean) => {
      const t = Math.max(lastTime.current, videoRef.current?.currentTime ?? 0);
      if (!Number.isFinite(t) || t < MIN_SECONDS || Math.abs(t - lastSent.current) < 1) return;
      lastSent.current = t;
      if (userId) saveWatchProgress(videoId, t, final);
      else saveAnonWatchTime(videoId, t);
    };

    // Theo dõi vị trí: lúc cleanup chạy, thẻ video có thể đã bị gỡ
    const tick = window.setInterval(() => {
      const v = videoRef.current;
      if (v && !v.paused) lastTime.current = v.currentTime;
    }, 500);
    const first = window.setTimeout(() => report(true), MIN_SECONDS * 1000 + 200);

    const onHide = () => {
      if (document.visibilityState === "hidden") report(true);
    };
    const onPageHide = () => report(true);
    document.addEventListener("visibilitychange", onHide);
    window.addEventListener("pagehide", onPageHide);

    return () => {
      window.clearInterval(tick);
      window.clearTimeout(first);
      document.removeEventListener("visibilitychange", onHide);
      window.removeEventListener("pagehide", onPageHide);
      report(true);
    };
  }, [active, videoId, userId, videoRef]);
}
