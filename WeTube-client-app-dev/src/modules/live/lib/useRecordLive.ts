"use client";

import { useEffect, useRef } from "react";
import { useLazyGetLiveRecordingQuery } from "@/app/api/liveApi";
import { useLiveRecordingStore } from "@/store/zustand/useLiveRecordingStore";

/**
 * Bắt đầu ghi khi máy chủ đã chuyển bản ghi sang RECORDING và nguồn hình đã chọn có mặt (với kiểu màn hình: sau khi
 * chủ kênh chọn màn hình). Trần thời lượng lấy lại ngay lúc bắt đầu vì phần còn lại của tháng có thể đã đổi từ lúc tạo
 * buổi. Mỗi lần mở trang chỉ thử bắt đầu một lần.
 */
export function useRecordLive(
  liveId: string,
  record: boolean,
  video: MediaStreamTrack | undefined,
  audio: MediaStreamTrack[]
) {
  const start = useLiveRecordingStore((s) => s.start);
  const setAudio = useLiveRecordingStore((s) => s.setAudio);
  const recording = useLiveRecordingStore((s) => s.liveId === liveId && s.phase === "recording");
  const [fetchRecordingInfo] = useLazyGetLiveRecordingQuery();
  const tried = useRef(false);
  // Mảng mới mỗi lần render: so theo id luồng để không chạy lại effect vô ích
  const audioKey = audio.map((t) => t.id).join(",");
  const audioRef = useRef(audio);
  audioRef.current = audio;

  useEffect(() => {
    if (!record || !video || tried.current) return;
    tried.current = true;
    fetchRecordingInfo()
      .unwrap()
      .then((info) => {
        if (info.secondsLeft > 0) start(liveId, video, audioRef.current, info.secondsLeft);
      })
      // Không hỏi được thì vẫn ghi: máy chủ chốt hạn mức và báo dừng (STOPPING) khi chạm trần
      .catch(() => start(liveId, video, audioRef.current, Number.POSITIVE_INFINITY));
  }, [record, video, liveId, start, fetchRecordingInfo]);

  useEffect(() => {
    if (recording) setAudio(audioRef.current);
  }, [recording, audioKey, setAudio]);
}
