"use client";

import { useEffect, useMemo, useRef } from "react";
import { X } from "lucide-react";
import { useControls } from "@/hooks/use-controls";
import { cn } from "@/lib/utils";
import { videoFrameUrl } from "@/lib/videoMedia";
import { requestSeek } from "@/lib/watchSeek";
import { useChaptersPanelStore } from "@/store/zustand/useChaptersPanelStore";
import { useVideoStore } from "@/store/zustand/videoStore";
import { chapterIndexAt, parseChapters } from "../lib/chapters";
import { formatTime } from "./video/format-time";

/**
 * Khung "Chương" ở cột phải trang xem, như YouTube: mở bằng nút tên chương trên trình phát; mỗi hàng là
 * ảnh khung hình, tên, mốc; bấm để tua tới chương. Video không có chương thì không hiện gì.
 */
export default function ChaptersPanel({ videoId }: { videoId: string }) {
  const response = useVideoStore((s) => s.videoDetail);
  const video = response?.detail.id === videoId ? response.detail : undefined;
  const { seek } = useControls();
  const open = useChaptersPanelStore((s) => s.open);
  const close = useChaptersPanelStore((s) => s.close);
  const panelRef = useRef<HTMLElement>(null);
  const activeRef = useRef<HTMLButtonElement>(null);

  const chapters = useMemo(
    () => parseChapters(video?.description, video?.duration ?? 0),
    [video?.description, video?.duration]
  );
  const current = chapterIndexAt(chapters, seek);

  // Sang video khác thì đóng khung
  useEffect(() => close, [videoId, close]);

  // Vừa mở: đưa khung vào màn hình (màn hẹp / chế độ rạp thì khung nằm dưới trình phát)
  useEffect(() => {
    if (open) panelRef.current?.scrollIntoView({ behavior: "smooth", block: "nearest" });
  }, [open]);

  // Đổi chương: cuộn hàng đang xem vào giữa khung (chỉ cuộn trong khung, không cuộn cả trang)
  useEffect(() => {
    const row = activeRef.current;
    const box = row?.parentElement;
    if (row && box) box.scrollTop = row.offsetTop - box.clientHeight / 2 + row.clientHeight / 2;
  }, [open, current]);

  if (!open || !video || !chapters.length) return null;

  return (
    <section
      ref={panelRef}
      className="animate-in fade-in-0 slide-in-from-top-2 overflow-hidden rounded-xl border border-gray-200 duration-200"
    >
      <div className="flex items-center gap-2 bg-gray-50 px-4 py-3">
        <div className="min-w-0 flex-1">
          <h2 className="text-lg font-bold">Chương</h2>
          <p className="text-xs text-gray-600">
            Chương {current + 1}/{chapters.length}
          </p>
        </div>
        <button type="button" aria-label="Đóng" onClick={close} className="rounded-full p-2 hover:bg-gray-200">
          <X className="size-5" />
        </button>
      </div>
      <div className="relative max-h-[400px] overflow-y-auto py-1">
        {chapters.map((c, i) => {
          const frame = videoFrameUrl(video.videoUrl, c.start);
          const active = i === current;
          return (
            <button
              key={c.start}
              ref={active ? activeRef : undefined}
              type="button"
              onClick={() => requestSeek(c.start)}
              className={cn(
                "flex w-full items-center gap-3 px-3 py-1.5 text-left hover:bg-gray-100",
                active && "bg-gray-200 hover:bg-gray-200"
              )}
            >
              <div className="aspect-video w-28 shrink-0 overflow-hidden rounded-md bg-gray-300">
                {/* eslint-disable-next-line @next/next/no-img-element -- ảnh Cloudinary đã đúng cỡ */}
                {frame && <img src={frame} alt="" loading="lazy" className="size-full object-cover" />}
              </div>
              <div className="min-w-0 flex-1">
                <p className="line-clamp-2 text-sm font-medium text-gray-900">{c.title}</p>
                <p className="text-xs text-blue-600">{formatTime(c.start)}</p>
              </div>
            </button>
          );
        })}
      </div>
    </section>
  );
}
