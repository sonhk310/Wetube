"use client";

import { useEffect, type RefObject } from "react";
import { correction, expectedPosition, RESUME_SEEK_THRESHOLD } from "@/lib/roomSync";
import { useRoomStore } from "@/store/zustand/useRoomStore";
import { sendRoom } from "./roomSocket";
import type { RoomControlType } from "../types";

/** Sau thao tác của chính mình, chưa sửa lệch trong khoảng này để không kéo ngược lại lúc server chưa kịp xác nhận. */
const LOCAL_ACTION_GRACE_MS = 1500;
const DRIFT_CHECK_MS = 500;
/** Gom nhiều lần tua liên tiếp (kéo thanh tua) thành một thao tác. */
const SEEK_DEBOUNCE_MS = 250;
/** Cờ "do mình gây ra" tự hết hạn phòng khi trình duyệt không bắn sự kiện. */
const EXPECT_TTL_MS = 1500;

/**
 * Nối thẻ video của trình phát với phòng xem chung (chỉ chạy khi `enabled`):
 * - áp trạng thái phòng (phát / dừng / vị trí) lên thẻ video mỗi khi server báo trạng thái mới;
 * - gửi thao tác của người xem (phát, dừng, tua, hết video) lên server;
 * - mỗi giây so với vị trí đúng và sửa lệch (tua nếu lệch nhiều, chỉnh tốc độ nhẹ nếu lệch ít).
 * `ready` đổi khi thẻ video xuất hiện / đổi nguồn để hook gắn lại.
 */
export function useRoomPlayback(videoRef: RefObject<HTMLVideoElement | null>, enabled: boolean, ready: unknown) {
  useEffect(() => {
    const v = videoRef.current;
    if (!enabled || !v) return;

    // Thay đổi do chính mình gây ra (áp trạng thái, sửa lệch) bắn lại sự kiện của thẻ video: không gửi ngược lên server
    const expect = { play: false, pause: false, seek: false };
    const expectTimers: Partial<Record<keyof typeof expect, number>> = {};
    const mark = (kind: keyof typeof expect) => {
      expect[kind] = true;
      window.clearTimeout(expectTimers[kind]);
      expectTimers[kind] = window.setTimeout(() => (expect[kind] = false), EXPECT_TTL_MS);
    };
    const consume = (kind: keyof typeof expect) => {
      if (!expect[kind]) return false;
      expect[kind] = false;
      window.clearTimeout(expectTimers[kind]);
      return true;
    };

    let buffering = false;
    let lastLocalAction = 0;
    let seekTimer: number | undefined;

    const serverNow = () => Date.now() + useRoomStore.getState().clockOffset;
    const expectedNow = () => {
      const playback = useRoomStore.getState().playback;
      if (!playback) return null;
      const target = expectedPosition(playback, serverNow());
      return Number.isFinite(v.duration) ? Math.min(target, v.duration) : target;
    };

    const apply = () => {
      const playback = useRoomStore.getState().playback;
      const target = expectedNow();
      if (!playback || target === null || v.readyState < 1) return;
      if (Math.abs(v.currentTime - target) > (playback.playing ? 0.3 : 0.05)) {
        mark("seek");
        v.currentTime = target;
      }
      if (playback.playing && v.paused) {
        mark("play");
        v.play().catch(() => {
          // Trình duyệt chặn tự phát: người xem bấm nút phát, vị trí lúc đó vẫn đúng nên không lệch
          expect.play = false;
        });
      } else if (!playback.playing && !v.paused) {
        mark("pause");
        v.pause();
      }
    };

    const sync = (seekThreshold?: number) => {
      const playback = useRoomStore.getState().playback;
      const target = expectedNow();
      if (!playback || target === null || v.seeking || buffering || v.readyState < 3) return;
      if (Date.now() - lastLocalAction < LOCAL_ACTION_GRACE_MS) return;
      if (playback.playing && v.paused) return; // chưa phát được (bị chặn tự phát): đợi người xem bấm
      if (Number.isFinite(v.duration) && target >= v.duration - 0.25) return;
      const fix = correction(v.currentTime, target, playback.playing, seekThreshold);
      if (fix.kind === "seek") {
        mark("seek");
        v.currentTime = fix.to;
      } else if (fix.kind === "rate") {
        v.playbackRate = fix.rate;
      } else if (v.playbackRate !== 1) {
        v.playbackRate = 1;
      }
    };

    const send = (type: RoomControlType, position: number) => {
      lastLocalAction = Date.now();
      sendRoom("control", {
        type,
        version: useRoomStore.getState().playback?.version ?? 0,
        position,
      });
    };

    const onPlay = () => {
      if (consume("play")) return;
      window.clearTimeout(seekTimer);
      send("PLAY", v.currentTime);
    };
    const onPause = () => {
      if (consume("pause") || v.ended) return;
      window.clearTimeout(seekTimer);
      send("PAUSE", v.currentTime);
    };
    const onSeeked = () => {
      if (consume("seek")) return;
      lastLocalAction = Date.now();
      window.clearTimeout(seekTimer);
      seekTimer = window.setTimeout(() => send("SEEK", v.currentTime), SEEK_DEBOUNCE_MS);
    };
    const onEnded = () => send("ENDED", Number.isFinite(v.duration) ? v.duration : v.currentTime);
    const onWaiting = () => (buffering = true);
    // Video vừa chạy được (tải xong, vừa phát lại): máy nhận lệnh luôn chậm hơn một chút vì phải tải dữ liệu rồi mới chạy,
    // và dữ liệu lúc này đã có sẵn nên tua rẻ: sửa lệch ngay thay vì chờ chỉnh tốc độ từ từ
    const onPlaying = () => {
      buffering = false;
      sync(RESUME_SEEK_THRESHOLD);
    };

    v.addEventListener("play", onPlay);
    v.addEventListener("pause", onPause);
    v.addEventListener("seeked", onSeeked);
    v.addEventListener("ended", onEnded);
    v.addEventListener("waiting", onWaiting);
    v.addEventListener("playing", onPlaying);
    v.addEventListener("loadedmetadata", apply);

    const unsubscribe = useRoomStore.subscribe((state, previous) => {
      if (state.playback !== previous.playback) apply();
    });
    apply();

    const drift = window.setInterval(() => sync(), DRIFT_CHECK_MS);

    return () => {
      window.clearInterval(drift);
      window.clearTimeout(seekTimer);
      window.clearTimeout(expectTimers.play);
      window.clearTimeout(expectTimers.pause);
      window.clearTimeout(expectTimers.seek);
      unsubscribe();
      v.removeEventListener("play", onPlay);
      v.removeEventListener("pause", onPause);
      v.removeEventListener("seeked", onSeeked);
      v.removeEventListener("ended", onEnded);
      v.removeEventListener("waiting", onWaiting);
      v.removeEventListener("playing", onPlaying);
      v.removeEventListener("loadedmetadata", apply);
      v.playbackRate = 1;
    };
  }, [videoRef, enabled, ready]);
}
