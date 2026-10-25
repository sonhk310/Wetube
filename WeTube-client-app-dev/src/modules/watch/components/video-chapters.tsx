"use client";

import { useMemo } from "react";
import { cn } from "@/lib/utils";
import { videoFrameUrl } from "@/lib/videoMedia";
import { requestSeek } from "@/lib/watchSeek";
import { CHAPTERS_ID, chapterIndexAt, parseChapters } from "../lib/chapters";
import { formatTime } from "./video/format-time";

interface VideoChaptersProps {
  description: string;
  duration: number;
  videoUrl: string;
  /** Giây đang phát, để tô chương hiện tại. */
  currentTime: number;
}

/** Hàng "Chương" dưới mô tả kiểu YouTube: ảnh khung hình, tên, mốc; bấm để tua. Không có chương thì không hiện gì. */
export default function VideoChapters({ description, duration, videoUrl, currentTime }: VideoChaptersProps) {
  const chapters = useMemo(() => parseChapters(description, duration), [description, duration]);
  if (!chapters.length) return null;
  const current = chapterIndexAt(chapters, currentTime);

  return (
    <div id={CHAPTERS_ID} className="mt-4">
      <p className="mb-2 font-semibold">Chương</p>
      <div className="flex gap-2 overflow-x-auto pb-1">
        {chapters.map((c, i) => {
          const frame = videoFrameUrl(videoUrl, c.start);
          return (
            <button
              key={c.start}
              type="button"
              onClick={() => requestSeek(c.start)}
              className={cn(
                "flex w-40 shrink-0 flex-col gap-1 rounded-lg p-1 text-left hover:bg-gray-200",
                i === current && "bg-gray-200"
              )}
            >
              <div className="aspect-video w-full overflow-hidden rounded-md bg-gray-300">
                {/* eslint-disable-next-line @next/next/no-img-element -- ảnh Cloudinary đã đúng cỡ */}
                {frame && <img src={frame} alt="" loading="lazy" className="size-full object-cover" />}
              </div>
              <span className="line-clamp-2 text-sm font-medium">{c.title}</span>
              <span className="text-xs text-blue-600">{formatTime(c.start)}</span>
            </button>
          );
        })}
      </div>
    </div>
  );
}
