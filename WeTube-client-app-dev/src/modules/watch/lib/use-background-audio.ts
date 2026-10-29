"use client";

import { type RefObject, useEffect } from "react";

const SKIP_SECONDS = 10;
/** Trong lúc phát nền, báo vị trí đang nghe mỗi khoảng này (cùng nhịp với trình phát). */
const REPORT_INTERVAL_MS = 15_000;
/** Video bị trình duyệt tự dừng khi trang ẩn: dừng trong chừng này ms trước lúc ẩn vẫn tính là "đang phát". */
const AUTO_PAUSE_WINDOW_MS = 800;
/** Còn chừng này giây thì coi như audio đã hết khi chuyển lại sang video. */
const END_MARGIN_S = 0.3;

type Options = {
  /** Premium, có audioUrl, công tắc Phát nền đang bật, không phải phòng xem chung. */
  enabled: boolean;
  audioUrl?: string;
  videoRef: RefObject<HTMLVideoElement | null>;
  title: string;
  channelName: string;
  artwork?: string | null;
  /** Báo vị trí đang xem (lưu lịch sử + thời gian xem); time lấy từ thẻ audio. */
  report: (final: boolean, time: number) => void;
};

/** Điện thoại / máy tính bảng: trình duyệt dừng thẻ video khi tắt màn hình. Máy tính vẫn phát khi chuyển thẻ nên không cần. */
function isTouchDevice() {
  return typeof window !== "undefined" && window.matchMedia("(pointer: coarse)").matches;
}

/**
 * Phát nền (Premium): trang bị ẩn lúc video đang phát thì chuyển sang thẻ <audio> (bản MP3 của video) tại đúng vị trí,
 * vì trình duyệt điện thoại vẫn cho audio chạy khi tắt màn hình và hiện điều khiển trên màn hình khoá (Media Session).
 * Mở lại trang thì quay về video tại vị trí của audio.
 */
export function useBackgroundAudio({ enabled, audioUrl, videoRef, title, channelName, artwork, report }: Options) {
  useEffect(() => {
    const video = videoRef.current;
    if (!enabled || !audioUrl || !video || !isTouchDevice()) return;

    let audio: HTMLAudioElement | null = null;
    let reportTimer: number | undefined;
    let lastPauseAt = 0;
    const onPause = () => {
      lastPauseAt = Date.now();
    };
    video.addEventListener("pause", onPause);

    const setSessionActions = (a: HTMLAudioElement) => {
      if (!("mediaSession" in navigator)) return;
      const session = navigator.mediaSession;
      session.metadata = new MediaMetadata({
        title,
        artist: channelName,
        artwork: artwork ? [{ src: artwork }] : [],
      });
      const seekBy = (delta: number) => {
        a.currentTime = Math.min(Math.max(a.currentTime + delta, 0), a.duration || Number.MAX_SAFE_INTEGER);
      };
      const handlers: [MediaSessionAction, MediaSessionActionHandler][] = [
        ["play", () => void a.play().catch(() => {})],
        ["pause", () => a.pause()],
        ["seekbackward", () => seekBy(-SKIP_SECONDS)],
        ["seekforward", () => seekBy(SKIP_SECONDS)],
        ["seekto", (d) => {
          if (typeof d.seekTime === "number") a.currentTime = d.seekTime;
        }],
      ];
      for (const [action, handler] of handlers) {
        try {
          session.setActionHandler(action, handler);
        } catch {
          // trình duyệt không hỗ trợ hành động này
        }
      }
    };

    const clearSessionActions = () => {
      if (!("mediaSession" in navigator)) return;
      for (const action of ["play", "pause", "seekbackward", "seekforward", "seekto"] as MediaSessionAction[]) {
        try {
          navigator.mediaSession.setActionHandler(action, null);
        } catch {
          // bỏ qua
        }
      }
    };

    const startAudio = () => {
      const wasPlaying = !video.paused || Date.now() - lastPauseAt < AUTO_PAUSE_WINDOW_MS;
      if (!wasPlaying || video.ended) return;
      const a = new Audio(audioUrl);
      a.preload = "auto";
      a.currentTime = video.currentTime;
      a.playbackRate = video.playbackRate;
      a.volume = video.volume;
      a.muted = video.muted;
      audio = a;
      video.pause();
      setSessionActions(a);
      void a.play().catch(() => {
        // Trình duyệt không cho phát nền (chưa có thao tác chạm gần đây...): thôi, về video khi mở lại trang
      });
      reportTimer = window.setInterval(() => report(false, a.currentTime), REPORT_INTERVAL_MS);
    };

    const stopAudio = () => {
      const a = audio;
      if (!a) return;
      audio = null;
      window.clearInterval(reportTimer);
      const wasPlaying = !a.paused && !a.ended;
      const finished = a.ended || (a.duration > 0 && a.duration - a.currentTime < END_MARGIN_S);
      const time = a.currentTime;
      a.pause();
      a.removeAttribute("src");
      a.load();
      clearSessionActions();
      // Audio đã hết lúc đang ẩn: cho video chạy nốt chút xíu để kích hoạt màn hình hết video như thường (Tự phát / danh sách)
      const target = finished && Number.isFinite(video.duration) ? Math.max(video.duration - 0.1, 0) : time;
      if (Number.isFinite(target)) video.currentTime = target;
      report(true, finished && Number.isFinite(video.duration) ? video.duration : time);
      if (wasPlaying || finished) void video.play().catch(() => {});
    };

    const onVisibility = () => {
      if (document.visibilityState === "hidden") startAudio();
      else stopAudio();
    };
    document.addEventListener("visibilitychange", onVisibility);

    return () => {
      document.removeEventListener("visibilitychange", onVisibility);
      video.removeEventListener("pause", onPause);
      if (audio) {
        audio.pause();
        audio.removeAttribute("src");
        audio.load();
        audio = null;
        window.clearInterval(reportTimer);
        clearSessionActions();
      }
    };
  }, [enabled, audioUrl, videoRef, title, channelName, artwork, report]);
}
