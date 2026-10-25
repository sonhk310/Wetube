"use client";

import { type RefObject, useEffect, useRef, useState } from "react";

/** Thanh tiến trình đỏ ở đáy Short: chạy mượt theo khung hình, bấm / kéo để tua, rê chuột thì dày lên. */
export function ShortsProgress({ videoRef, active }: { videoRef: RefObject<HTMLVideoElement | null>; active: boolean }) {
  const [ratio, setRatio] = useState(0);
  const barRef = useRef<HTMLDivElement>(null);
  const dragging = useRef(false);

  useEffect(() => {
    if (!active) return;
    let frame = 0;
    const tick = () => {
      const v = videoRef.current;
      if (v && v.duration > 0 && !dragging.current) setRatio(v.currentTime / v.duration);
      frame = requestAnimationFrame(tick);
    };
    frame = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(frame);
  }, [active, videoRef]);

  const seekTo = (clientX: number) => {
    const bar = barRef.current;
    const v = videoRef.current;
    if (!bar || !v || !(v.duration > 0)) return;
    const rect = bar.getBoundingClientRect();
    const next = Math.min(1, Math.max(0, (clientX - rect.left) / rect.width));
    setRatio(next);
    v.currentTime = next * v.duration;
  };

  return (
    <div
      ref={barRef}
      role="slider"
      aria-label="Thanh tiến trình"
      aria-valuemin={0}
      aria-valuemax={100}
      aria-valuenow={Math.round(ratio * 100)}
      className="group/progress absolute inset-x-0 bottom-0 z-20 flex h-4 cursor-pointer items-end"
      onClick={(e) => e.stopPropagation()}
      onPointerDown={(e) => {
        e.stopPropagation();
        dragging.current = true;
        e.currentTarget.setPointerCapture(e.pointerId);
        seekTo(e.clientX);
      }}
      onPointerMove={(e) => {
        if (dragging.current) seekTo(e.clientX);
      }}
      onPointerUp={() => {
        dragging.current = false;
      }}
      onPointerCancel={() => {
        dragging.current = false;
      }}
    >
      <div className="relative h-1 w-full bg-white/30 transition-[height] group-hover/progress:h-1.5">
        <div className="absolute inset-y-0 left-0 bg-red-600" style={{ width: `${ratio * 100}%` }} />
      </div>
    </div>
  );
}
