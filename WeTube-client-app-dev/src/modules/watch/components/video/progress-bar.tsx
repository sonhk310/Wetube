"use client";

import { useRef, useState } from "react";
import { cn } from "@/lib/utils";
import { chapterIndexAt, type Chapter } from "../../lib/chapters";
import { formatTime } from "./format-time";

interface ProgressBarProps {
  currentTime: number;
  duration: number;
  /** Đã tải tới giây thứ mấy (tính từ vị trí đang xem). */
  buffered: number;
  /** Có chương thì thanh tua chia đoạn và nhãn rê chuột hiện tên chương. */
  chapters: Chapter[];
  /** Giây có quảng cáo sắp tới (người chưa Premium): hiện vạch vàng trên thanh. */
  adMarks?: number[];
  onScrubStart: () => void;
  onScrub: (time: number) => void;
  onScrubEnd: (time: number) => void;
}

/** Thanh tua kiểu YouTube: kéo bằng chuột hoặc ngón tay, rê chuột thì hiện thời điểm. */
export default function ProgressBar({
  currentTime,
  duration,
  buffered,
  chapters,
  adMarks = [],
  onScrubStart,
  onScrub,
  onScrubEnd,
}: ProgressBarProps) {
  const barRef = useRef<HTMLDivElement>(null);
  const [hover, setHover] = useState<{ x: number; time: number; width: number } | null>(null);
  const [dragging, setDragging] = useState(false);

  const pointAt = (clientX: number) => {
    const rect = barRef.current!.getBoundingClientRect();
    const ratio = rect.width > 0 ? Math.min(1, Math.max(0, (clientX - rect.left) / rect.width)) : 0;
    return { x: ratio * rect.width, time: ratio * duration, width: rect.width };
  };

  const handlePointerDown = (e: React.PointerEvent<HTMLDivElement>) => {
    if (!duration || e.button !== 0) return;
    e.currentTarget.setPointerCapture(e.pointerId);
    setDragging(true);
    onScrubStart();
    onScrub(pointAt(e.clientX).time);
  };

  const handlePointerMove = (e: React.PointerEvent<HTMLDivElement>) => {
    if (!duration) return;
    const point = pointAt(e.clientX);
    setHover(point);
    if (dragging) onScrub(point.time);
  };

  const handlePointerUp = (e: React.PointerEvent<HTMLDivElement>) => {
    if (!dragging) return;
    setDragging(false);
    onScrubEnd(pointAt(e.clientX).time);
    if (e.pointerType !== "mouse") setHover(null);
  };

  const played = duration ? Math.min(100, (currentTime / duration) * 100) : 0;
  const expanded = dragging || hover !== null;
  // Không có chương thì cả thanh là một đoạn
  const segments = chapters.length ? chapters : [{ start: 0, end: duration }];
  const hoverIndex = hover ? chapterIndexAt(chapters, hover.time) : -1;
  const hoverTitle = hoverIndex >= 0 ? chapters[hoverIndex].title : null;
  // Phần trăm của đoạn [start, end] đã tới giây `time`
  const fill = (time: number, start: number, end: number) =>
    end > start ? Math.min(100, Math.max(0, ((time - start) / (end - start)) * 100)) : 0;
  // Giữ nhãn thời gian (và tên chương) nằm trong khung
  const labelHalf = hoverTitle ? 80 : 24;
  const labelX = hover ? Math.min(Math.max(hover.x, labelHalf), hover.width - labelHalf) : 0;

  return (
    <div
      ref={barRef}
      role="slider"
      aria-label="Thanh tua video"
      aria-valuemin={0}
      aria-valuemax={Math.round(duration)}
      aria-valuenow={Math.round(currentTime)}
      aria-valuetext={`${formatTime(currentTime)} / ${formatTime(duration)}`}
      className="relative flex h-4 w-full cursor-pointer touch-none items-center"
      onPointerDown={handlePointerDown}
      onPointerMove={handlePointerMove}
      onPointerUp={handlePointerUp}
      onPointerCancel={handlePointerUp}
      onPointerLeave={() => {
        if (!dragging) setHover(null);
      }}
    >
      <div className="flex w-full items-center gap-0.5">
        {segments.map((s, i) => (
          <div
            key={s.start}
            className={cn(
              "relative bg-white/25 transition-[height] duration-100",
              chapters.length && i === hoverIndex ? "h-[8px]" : expanded ? "h-[5px]" : "h-[3px]"
            )}
            // Rộng theo độ dài đoạn (chưa biết thời lượng thì vẫn rộng hết thanh)
            style={{ flex: `${s.end - s.start || 1} 1 0` }}
          >
            <div className="absolute inset-y-0 left-0 bg-white/40" style={{ width: `${fill(buffered, s.start, s.end)}%` }} />
            {hover && !dragging && (
              <div
                className="absolute inset-y-0 left-0 bg-white/30"
                style={{ width: `${fill(hover.time, s.start, s.end)}%` }}
              />
            )}
            <div className="absolute inset-y-0 left-0 bg-red-600" style={{ width: `${fill(currentTime, s.start, s.end)}%` }} />
          </div>
        ))}
      </div>
      {/* Vạch vàng: chỗ sắp có quảng cáo (hết quảng cáo thì vạch biến mất) */}
      {duration > 0 &&
        adMarks.map((time) => (
          <span
            key={time}
            data-ad-mark
            aria-hidden
            className="pointer-events-none absolute top-1/2 z-[1] h-2.5 w-[5px] -translate-x-1/2 -translate-y-1/2 rounded-[1px] bg-yellow-400 shadow-[0_0_2px_rgba(0,0,0,0.6)]"
            style={{ left: `${Math.min(100, (time / duration) * 100)}%` }}
          />
        ))}
      <div
        className={cn(
          "pointer-events-none absolute top-1/2 h-3.5 w-3.5 -translate-x-1/2 -translate-y-1/2 rounded-full bg-red-600 transition-transform duration-100",
          expanded ? "scale-100" : "scale-0"
        )}
        style={{ left: `${played}%` }}
      />

      {hover && (
        <div
          className="pointer-events-none absolute bottom-5 flex -translate-x-1/2 flex-col items-center gap-0.5 text-xs font-medium text-white"
          style={{ left: labelX }}
        >
          {hoverTitle && (
            <span className="max-w-40 truncate [text-shadow:0_0_4px_rgba(0,0,0,0.8)]">{hoverTitle}</span>
          )}
          <span className="rounded bg-black/80 px-1.5 py-0.5 tabular-nums">{formatTime(hover.time)}</span>
        </div>
      )}
    </div>
  );
}
