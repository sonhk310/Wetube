"use client";

import { ChevronLeft, ChevronRight } from "lucide-react";
import { ReactNode, useCallback, useEffect, useRef, useState } from "react";

/** Một hàng cuộn ngang có tiêu đề và nút ‹ › (trang chủ kênh: "Video mới nhất", "Shorts"...). */
export default function VideoCarousel({ title, icon, children }: { title: string; icon?: ReactNode; children: ReactNode }) {
  const rowRef = useRef<HTMLDivElement>(null);
  const [canLeft, setCanLeft] = useState(false);
  const [canRight, setCanRight] = useState(false);

  const update = useCallback(() => {
    const el = rowRef.current;
    if (!el) return;
    setCanLeft(el.scrollLeft > 4);
    setCanRight(el.scrollLeft + el.clientWidth < el.scrollWidth - 4);
  }, []);

  useEffect(() => {
    update();
    window.addEventListener("resize", update);
    return () => window.removeEventListener("resize", update);
  }, [update, children]);

  const scroll = (direction: -1 | 1) => {
    const el = rowRef.current;
    if (el) el.scrollBy({ left: direction * el.clientWidth * 0.8, behavior: "smooth" });
  };

  return (
    <section className="border-t border-gray-200 pt-4 first:border-t-0 first:pt-0">
      <h2 className="mb-3 flex items-center gap-2 text-xl font-bold">
        {icon}
        {title}
      </h2>
      <div className="relative">
        <div ref={rowRef} onScroll={update} className="flex gap-4 overflow-x-auto pb-1 [scrollbar-width:none]">
          {children}
        </div>
        {canLeft && (
          <button
            type="button"
            aria-label="Cuộn sang trái"
            onClick={() => scroll(-1)}
            className="absolute left-1 top-1/3 flex size-10 items-center justify-center rounded-full border bg-white shadow hover:bg-gray-100"
          >
            <ChevronLeft className="size-5" />
          </button>
        )}
        {canRight && (
          <button
            type="button"
            aria-label="Cuộn sang phải"
            onClick={() => scroll(1)}
            className="absolute right-1 top-1/3 flex size-10 items-center justify-center rounded-full border bg-white shadow hover:bg-gray-100"
          >
            <ChevronRight className="size-5" />
          </button>
        )}
      </div>
    </section>
  );
}
