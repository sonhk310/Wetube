"use client";

import { useRef, useState } from "react";
import { cn } from "@/lib/utils";
import { formatTime } from "./format-time";

interface ProgressBarProps {
  currentTime: number;
  duration: number;
  /** Đã tải tới giây thứ mấy (tính từ vị trí đang xem). */
  buffered: number;
  onScrubStart: () => void;
  onScrub: (time: number) => void;
  onScrubEnd: (time: number) => void;
}

/** Thanh tua kiểu YouTube: kéo bằng chuột hoặc ngón tay, rê chuột thì hiện thời điểm. */
export default function ProgressBar({
  currentTime,
  duration,
  buffered,
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
  const loaded = duration ? Math.min(100, (buffered / duration) * 100) : 0;
  const expanded = dragging || hover !== null;
  // Giữ nhãn thời gian nằm trong khung
  const labelX = hover ? Math.min(Math.max(hover.x, 24), hover.width - 24) : 0;

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
      <div
        className={cn(
          "relative w-full bg-white/25 transition-[height] duration-100",
          expanded ? "h-[5px]" : "h-[3px]"
        )}
      >
        <div className="absolute inset-y-0 left-0 bg-white/40" style={{ width: `${loaded}%` }} />
        {hover && !dragging && (
          <div className="absolute inset-y-0 left-0 bg-white/30" style={{ width: hover.x }} />
        )}
        <div className="absolute inset-y-0 left-0 bg-red-600" style={{ width: `${played}%` }} />
        <div
          className={cn(
            "absolute top-1/2 h-3.5 w-3.5 -translate-x-1/2 -translate-y-1/2 rounded-full bg-red-600 transition-transform duration-100",
            expanded ? "scale-100" : "scale-0"
          )}
          style={{ left: `${played}%` }}
        />
      </div>

      {hover && (
        <div
          className="pointer-events-none absolute bottom-5 -translate-x-1/2 rounded bg-black/80 px-1.5 py-0.5 text-xs font-medium text-white tabular-nums"
          style={{ left: labelX }}
        >
          {formatTime(hover.time)}
        </div>
      )}
    </div>
  );
}
