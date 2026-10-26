"use client";

import { useEffect } from "react";
import { useAuth } from "@/contexts/auth-context";
import { toastEmitter } from "@/lib/toastEmitter";
import { estimateOffset, type ClockSample } from "@/lib/roomSync";
import { useRoomStore } from "@/store/zustand/useRoomStore";
import { createRoomSocket } from "./roomSocket";

const TIME_SYNC_MS = 30_000;
const MAX_SAMPLES = 8;

/** Mở kết nối tới phòng `code` khi đã đăng nhập; đóng (chờ vài giây phòng khi vào lại) khi rời trang. */
export function useRoomConnection(code: string) {
  const { user } = useAuth();
  const userId = user?.sub;

  useEffect(() => {
    if (!userId) return;
    const store = useRoomStore.getState();
    store.open(code);

    const samples: ClockSample[] = [];
    const socket = createRoomSocket(code, {
      onEvent: (event) => {
        if (event.type === "TIME" && event.serverTime && event.clientSent) {
          samples.push({ clientSent: event.clientSent, serverTime: event.serverTime, clientReceived: Date.now() });
          if (samples.length > MAX_SAMPLES) samples.shift();
          useRoomStore.getState().setOffset(estimateOffset(samples));
          return;
        }
        if (event.type === "ERROR" && (event.error === "RATE" || event.error === "INVALID") && event.text) {
          toastEmitter.error(event.text);
          return;
        }
        useRoomStore.getState().apply(event);
      },
      onConnected: () => socket.send("time", { clientSent: Date.now() }),
      onDisconnected: () => {
        // Mất kết nối: giữ nguyên màn hình, tự kết nối lại và nhận lại ảnh chụp phòng
      },
    });
    const timer = window.setInterval(() => socket.send("time", { clientSent: Date.now() }), TIME_SYNC_MS);

    return () => {
      window.clearInterval(timer);
      socket.send("leave", { immediate: false });
      socket.stop();
      useRoomStore.getState().close();
    };
  }, [code, userId]);
}
