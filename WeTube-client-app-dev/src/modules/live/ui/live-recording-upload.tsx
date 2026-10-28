"use client";

import { useEffect } from "react";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { Progress } from "@/components/ui/progress";
import { hasPendingRecording, useLiveRecordingStore } from "@/store/zustand/useLiveRecordingStore";

/**
 * Trang Studio sau khi buổi phát kết thúc: tải bản ghi của buổi vừa rồi lên (tự bắt đầu), tiến độ, Thử lại khi lỗi,
 * kết quả. Bản ghi chỉ nằm trong tab này nên nhắc đừng đóng tab, và trình duyệt hỏi lại nếu đóng.
 */
export default function LiveRecordingUpload() {
  const { liveId, phase, progress, error, retryable, published, stop, upload, dismiss } = useLiveRecordingStore();
  const pending = hasPendingRecording(phase, retryable);

  // Buổi kết thúc từ nơi khác (quản trị viên, mất kết nối) khi bộ ghi còn chạy: dừng rồi tải lên phần đã ghi
  useEffect(() => {
    if (liveId && phase === "recording") stop(liveId, "USER");
    if (phase === "stopped") upload();
  }, [liveId, phase, stop, upload]);

  useEffect(() => {
    if (!pending) return;
    const warn = (e: BeforeUnloadEvent) => {
      e.preventDefault();
    };
    window.addEventListener("beforeunload", warn);
    return () => window.removeEventListener("beforeunload", warn);
  }, [pending]);

  if (!liveId || phase === "idle" || phase === "unsupported") return null;

  if (phase === "done") {
    const videoId = published?.replayVideoId;
    return (
      <div className="mx-6 mt-4 flex flex-wrap items-center gap-3 rounded-xl border p-4 text-sm">
        <p className="flex-1">
          {videoId
            ? "Bản ghi của buổi phát vừa rồi đã thành video riêng tư của kênh. Vào Nội dung đổi sang Công khai khi muốn mọi người xem."
            : published?.recordingMessage ?? "Bản ghi đã được tải lên."}
        </p>
        {videoId ? (
          <Button asChild variant="secondary" className="rounded-full">
            <Link href={`/watch/${videoId}`}>Xem video</Link>
          </Button>
        ) : null}
        <Button variant="ghost" className="rounded-full" onClick={dismiss}>
          Đóng
        </Button>
      </div>
    );
  }

  if (phase === "failed") {
    return (
      <div className="mx-6 mt-4 flex flex-wrap items-center gap-3 rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-700">
        <p className="flex-1">{error}</p>
        {retryable ? (
          <Button variant="secondary" className="rounded-full" onClick={upload}>
            Thử lại
          </Button>
        ) : null}
        <Button variant="ghost" className="rounded-full" onClick={dismiss}>
          {retryable ? "Bỏ bản ghi" : "Đóng"}
        </Button>
      </div>
    );
  }

  return (
    <div className="mx-6 mt-4 space-y-2 rounded-xl border p-4 text-sm">
      <p className="font-medium">
        {phase !== "uploading"
          ? "Đang chuẩn bị bản ghi…"
          : progress < 1
            ? `Đang tải bản ghi lên… ${Math.round(progress * 100)}%`
            : "Đã tải lên, máy chủ đang chuyển bản ghi sang kho video…"}
      </p>
      <Progress value={phase === "uploading" ? progress * 100 : 0} />
      <p className="text-gray-500">
        Đừng đóng tab này cho tới khi tải xong: bản ghi chỉ nằm trong trình duyệt. Tải xong, máy chủ còn cần ít phút để
        xử lý video.
      </p>
    </div>
  );
}
