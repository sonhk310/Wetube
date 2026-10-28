"use client";

import { useEffect, useRef } from "react";
import type { TrackPublication } from "livekit-client";
import { useLazyGetLiveRecordingQuery } from "@/app/api/liveApi";
import { useLiveRecordingStore } from "@/store/zustand/useLiveRecordingStore";

/** Track đang phát của một publication; null nếu chủ kênh đã tắt nó. */
function liveTrackOf(publication: TrackPublication | undefined): MediaStreamTrack | null {
  if (!publication || publication.isMuted) return null;
  return publication.track?.mediaStreamTrack ?? null;
}

/**
 * Bắt đầu ghi khi máy chủ đã chuyển bản ghi sang RECORDING và nguồn hình đã chọn có mặt (với kiểu màn hình: sau khi
 * chủ kênh chọn màn hình). Bộ ghi đọc lại nguồn mỗi giây qua publication mới nhất, vì LiveKit thay track bên dưới mà
 * không phải lúc nào cũng làm trang vẽ lại. Trần thời lượng lấy lại ngay lúc bắt đầu vì phần còn lại của tháng có thể
 * đã đổi từ lúc tạo buổi. Mỗi lần mở trang chỉ thử bắt đầu một lần.
 */
export function useRecordLive(
  liveId: string,
  record: boolean,
  video: TrackPublication | undefined,
  audio: TrackPublication[]
) {
  const start = useLiveRecordingStore((s) => s.start);
  const [fetchRecordingInfo] = useLazyGetLiveRecordingQuery();
  const tried = useRef(false);
  const videoRef = useRef(video);
  const audioRef = useRef(audio);
  useEffect(() => {
    videoRef.current = video;
    audioRef.current = audio;
  });
  const hasVideo = !!liveTrackOf(video);

  useEffect(() => {
    if (!record || !hasVideo || tried.current) return;
    tried.current = true;
    const sources = {
      video: () => liveTrackOf(videoRef.current),
      audio: () => audioRef.current.map(liveTrackOf).filter((t): t is MediaStreamTrack => !!t),
    };
    fetchRecordingInfo()
      .unwrap()
      .then((info) => {
        if (info.secondsLeft > 0) start(liveId, sources, info.secondsLeft);
      })
      // Không hỏi được thì vẫn ghi: máy chủ chốt hạn mức và báo dừng (STOPPING) khi chạm trần
      .catch(() => start(liveId, sources, Number.POSITIVE_INFINITY));
  }, [record, hasVideo, liveId, start, fetchRecordingInfo]);
}
