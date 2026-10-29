"use client";

import { type RefObject, useEffect } from "react";

const SKIP_SECONDS = 10;
/** Trong lúc phát nền, báo vị trí đang nghe mỗi khoảng này (cùng nhịp với trình phát). */
const REPORT_INTERVAL_MS = 15_000;
/** Video bị trình duyệt tự dừng khi tắt màn hình: dừng trong chừng này ms trước lúc ẩn trang vẫn tính là "đang phát". */
const AUTO_PAUSE_WINDOW_MS = 1000;
/** Còn chừng này giây thì coi như audio đã hết khi chuyển lại sang video. */
const END_MARGIN_S = 0.3;
/** Lần chạm đầu tiên trên trang: dùng để "mở khoá" thẻ audio (xem prime). */
const GESTURE_EVENTS = ["pointerdown", "touchend", "click", "keydown"] as const;

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
 *
 * Trình duyệt chỉ cho phát tiếng khi trang đã có thao tác chạm của người dùng, và tạo thẻ Audio mới đúng lúc tắt
 * màn hình (không có thao tác nào) có thể bị chặn. Vì vậy một thẻ audio duy nhất được "mở khoá" (phát im lặng
 * rồi dừng) ngay ở lần chạm đầu tiên, rồi dùng lại thẻ đó khi trang bị ẩn.
 */
export function useBackgroundAudio({ enabled, audioUrl, videoRef, title, channelName, artwork, report }: Options) {
  useEffect(() => {
    const video = videoRef.current;
    if (!enabled || !audioUrl || !video || !isTouchDevice()) return;

    const audio = new Audio();
    audio.preload = "none";
    let loaded = false;
    let active = false;
    let primed = false;
    let reportTimer: number | undefined;
    let lastPauseAt = 0;
    const onPause = () => {
      lastPauseAt = Date.now();
    };
    video.addEventListener("pause", onPause);

    const load = () => {
      if (loaded) return;
      loaded = true;
      audio.preload = "auto";
      audio.src = audioUrl;
    };

    /** Mở khoá thẻ audio bằng một lần phát im lặng trong lúc đang có thao tác của người dùng. */
    const prime = () => {
      if (primed) return;
      primed = true;
      removeGestureListeners();
      load();
      audio.muted = true;
      audio
        .play()
        .then(() => {
          if (!active) audio.pause();
        })
        .catch(() => {
          // chưa phát được (mạng, định dạng): đến lúc dùng sẽ thử lại
        })
        .finally(() => {
          if (!active) audio.muted = false;
        });
    };
    const removeGestureListeners = () => {
      for (const type of GESTURE_EVENTS) document.removeEventListener(type, prime, true);
    };
    // Đã có thao tác từ trước (vd. bấm Phát): mở khoá luôn, chưa thì chờ lần chạm đầu tiên
    if ((navigator as Navigator & { userActivation?: { hasBeenActive: boolean } }).userActivation?.hasBeenActive) {
      prime();
    } else {
      for (const type of GESTURE_EVENTS) document.addEventListener(type, prime, true);
    }

    const setSessionActions = () => {
      if (!("mediaSession" in navigator)) return;
      const session = navigator.mediaSession;
      session.metadata = new MediaMetadata({
        title,
        artist: channelName,
        artwork: artwork ? [{ src: artwork }] : [],
      });
      const seekBy = (delta: number) => {
        audio.currentTime = Math.min(Math.max(audio.currentTime + delta, 0), audio.duration || Number.MAX_SAFE_INTEGER);
      };
      const handlers: [MediaSessionAction, MediaSessionActionHandler][] = [
        ["play", () => void audio.play().catch(() => {})],
        ["pause", () => audio.pause()],
        ["seekbackward", () => seekBy(-SKIP_SECONDS)],
        ["seekforward", () => seekBy(SKIP_SECONDS)],
        ["seekto", (d) => {
          if (typeof d.seekTime === "number") audio.currentTime = d.seekTime;
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

    /** Đặt vị trí cho audio: thẻ chưa đọc xong thông tin thì chờ rồi đặt (đặt sớm bị bỏ qua). */
    const seekAudioTo = (time: number) => {
      if (audio.readyState >= 1) {
        audio.currentTime = time;
        return;
      }
      audio.addEventListener("loadedmetadata", () => {
        audio.currentTime = time;
      }, { once: true });
    };

    const startAudio = () => {
      const wasPlaying = !video.paused || Date.now() - lastPauseAt < AUTO_PAUSE_WINDOW_MS;
      if (!wasPlaying || video.ended) return;
      active = true;
      load();
      audio.muted = video.muted;
      audio.volume = video.volume;
      audio.playbackRate = video.playbackRate;
      seekAudioTo(video.currentTime);
      video.pause();
      setSessionActions();
      void audio.play().catch((err: unknown) => {
        // Trình duyệt không cho phát nền (chưa có thao tác chạm, hoặc MP3 chưa tải được): về video khi mở lại trang
        console.warn("Phát nền không bắt đầu được:", err instanceof Error ? err.name : err);
      });
      reportTimer = window.setInterval(() => report(false, audio.currentTime), REPORT_INTERVAL_MS);
    };

    const stopAudio = () => {
      if (!active) return;
      active = false;
      window.clearInterval(reportTimer);
      const wasPlaying = !audio.paused && !audio.ended;
      const finished = audio.ended || (audio.duration > 0 && audio.duration - audio.currentTime < END_MARGIN_S);
      const time = audio.currentTime;
      audio.pause();
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
      removeGestureListeners();
      document.removeEventListener("visibilitychange", onVisibility);
      video.removeEventListener("pause", onPause);
      window.clearInterval(reportTimer);
      clearSessionActions();
      audio.pause();
      audio.removeAttribute("src");
      audio.load();
    };
  }, [enabled, audioUrl, videoRef, title, channelName, artwork, report]);
}
