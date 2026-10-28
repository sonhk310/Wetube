"use client";

import { useCallback } from "react";
import { useLocalParticipant } from "@livekit/components-react";
import { toastEmitter } from "@/lib/toastEmitter";

/**
 * Bật / tắt chia sẻ màn hình của chủ kênh (phải nằm trong LiveKitRoom). 1080p ở 15 khung hình mỗi giây: chữ vẫn rõ mà
 * tốn ít băng thông hơn 30 khung. Kèm tiếng của tab / hệ thống nếu trình duyệt cho (Chrome, Edge).
 */
export function useScreenShare() {
  const { localParticipant, isScreenShareEnabled } = useLocalParticipant();

  const start = useCallback(async () => {
    try {
      await localParticipant.setScreenShareEnabled(true, {
        audio: true,
        resolution: { width: 1920, height: 1080, frameRate: 15 },
        contentHint: "detail",
      });
    } catch (err) {
      // Đóng hộp chọn màn hình mà không chọn gì thì trình duyệt báo NotAllowedError: không phải lỗi
      if ((err as { name?: string }).name === "NotAllowedError") return;
      toastEmitter.error("Không chia sẻ được màn hình, vui lòng thử lại.");
    }
  }, [localParticipant]);

  const stop = useCallback(() => localParticipant.setScreenShareEnabled(false), [localParticipant]);

  return { sharing: isScreenShareEnabled, start, stop };
}
