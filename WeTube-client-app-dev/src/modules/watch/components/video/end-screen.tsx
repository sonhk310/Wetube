"use client";

import { useEffect, useState } from "react";
import { RotateCcw, X } from "lucide-react";
import type { RecommendVideoDto } from "@/types/video";
import { formatViews } from "@/lib/utils";
import { formatTime } from "./format-time";

/** Số giây đếm ngược trước khi tự phát video tiếp theo. */
const AUTOPLAY_SECONDS = 5;
const MAX_SUGGESTIONS = 6;

interface EndScreenProps {
  videos: RecommendVideoDto[];
  autoplay: boolean;
  fullscreen: boolean;
  onReplay: () => void;
  onOpen: (videoId: string) => void;
}

function Thumbnail({ video, className }: { video: RecommendVideoDto; className?: string }) {
  return (
    <div
      className={`relative aspect-video overflow-hidden rounded-lg bg-white/10 bg-cover bg-center ${className ?? ""}`}
      style={{ backgroundImage: `url(${video.thumbnailUrl})` }}
    >
      <span className="absolute right-1 bottom-1 rounded bg-black/80 px-1 text-[11px] font-semibold text-white">
        {formatTime(video.duration)}
      </span>
    </div>
  );
}

/** Màn hình khi hết video: đếm ngược tự phát video tiếp theo, huỷ thì hiện lưới gợi ý (giống YouTube). */
export default function EndScreen({ videos, autoplay, fullscreen, onReplay, onOpen }: EndScreenProps) {
  const next = videos[0];
  const [count, setCount] = useState<number | null>(autoplay && next ? AUTOPLAY_SECONDS : null);

  useEffect(() => {
    if (count === null || !next) return;
    if (count <= 0) {
      onOpen(next.id);
      return;
    }
    const timer = window.setTimeout(() => setCount((c) => (c === null ? null : c - 1)), 1000);
    return () => window.clearTimeout(timer);
  }, [count, next, onOpen]);

  // Đang đếm ngược: thẻ "Tiếp theo"
  if (count !== null && next) {
    return (
      <div className="absolute inset-x-0 top-0 bottom-14 z-[15] flex items-center justify-center bg-black/75 px-4">
        <div className="flex w-full max-w-sm flex-col gap-3 text-white">
          <p className="text-sm text-white/80">
            Tiếp theo trong <span className="font-semibold text-white">{Math.max(count, 0)}</span>
          </p>
          <button type="button" className="flex gap-3 text-left" onClick={() => onOpen(next.id)}>
            <Thumbnail video={next} className="w-40 shrink-0 sm:w-48" />
            <span className="min-w-0">
              <span className="line-clamp-2 text-sm font-semibold sm:text-base">{next.title}</span>
              <span className="mt-1 block truncate text-xs text-white/70">{next.name}</span>
              <span className="block text-xs text-white/70">{formatViews(next.totalView)}</span>
            </span>
          </button>
          {/* Vạch chạy hết trong thời gian đếm ngược */}
          <div className="h-1 overflow-hidden rounded bg-white/20">
            <div
              className="h-full bg-white"
              style={{ width: "100%", animation: `wt-countdown ${AUTOPLAY_SECONDS}s linear forwards` }}
            />
          </div>
          <div className="flex gap-2">
            <button
              type="button"
              onClick={() => setCount(null)}
              className="flex-1 rounded-full bg-white/15 px-4 py-2 text-sm font-medium hover:bg-white/25"
            >
              Huỷ
            </button>
            <button
              type="button"
              onClick={() => onOpen(next.id)}
              className="flex-1 rounded-full bg-white px-4 py-2 text-sm font-medium text-black hover:bg-white/90"
            >
              Phát ngay
            </button>
          </div>
        </div>
        <style>{`@keyframes wt-countdown { from { width: 100%; } to { width: 0%; } }`}</style>
      </div>
    );
  }

  // Lưới gợi ý + xem lại
  const suggestions = videos.slice(0, MAX_SUGGESTIONS);
  return (
    <div className="absolute inset-x-0 top-0 bottom-14 z-[15] flex flex-col bg-black/80 p-3 sm:p-6">
      <div className="mb-2 flex items-center justify-between sm:mb-4">
        <button
          type="button"
          onClick={onReplay}
          className="flex items-center gap-2 rounded-full bg-white/15 px-4 py-1.5 text-sm font-medium text-white hover:bg-white/25"
        >
          <RotateCcw size={16} /> Xem lại
        </button>
        {count === null && autoplay && next && (
          <span className="flex items-center gap-1 text-xs text-white/60">
            <X size={14} /> Đã huỷ tự phát
          </span>
        )}
      </div>
      {suggestions.length > 0 && (
        <div
          className={`grid min-h-0 flex-1 auto-rows-min gap-2 overflow-hidden sm:gap-3 ${
            fullscreen ? "grid-cols-3" : "grid-cols-2 md:grid-cols-3"
          }`}
        >
          {suggestions.map((v) => (
            <button
              key={v.id}
              type="button"
              onClick={() => onOpen(v.id)}
              className="group/item min-w-0 text-left text-white"
            >
              <Thumbnail video={v} className="ring-white/70 transition group-hover/item:ring-2" />
              <span className="mt-1 line-clamp-1 text-xs font-medium sm:line-clamp-2 sm:text-sm">{v.title}</span>
              <span className="hidden truncate text-xs text-white/60 sm:block">{v.name}</span>
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
