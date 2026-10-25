"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import Link from "next/link";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { Button } from "@/components/ui/button";
import type { VideoFromPlaylist } from "@/types/playlistSummary";
import { ContentCard } from "./content-card";

interface ContentRowProps {
  title: string;
  videos: VideoFromPlaylist[];
  vertical: boolean;
  emptyText: string;
  onDelete: (video: VideoFromPlaylist) => void;
}

/** Một khu (Video hoặc Shorts): hàng cuộn ngang có nút mũi tên; "Xem tất cả" mở bảng /studio/content. */
export function ContentRow({ title, videos, vertical, emptyText, onDelete }: ContentRowProps) {
  const scrollerRef = useRef<HTMLDivElement>(null);
  // Đã cuộn tới đầu / cuối hàng thì tắt mũi tên bên đó
  const [canLeft, setCanLeft] = useState(false);
  const [canRight, setCanRight] = useState(false);

  const updateArrows = useCallback(() => {
    const el = scrollerRef.current;
    if (!el) return;
    setCanLeft(el.scrollLeft > 1);
    setCanRight(el.scrollLeft + el.clientWidth < el.scrollWidth - 1);
  }, []);

  useEffect(() => {
    const el = scrollerRef.current;
    if (!el) return;
    updateArrows();
    el.addEventListener("scroll", updateArrows, { passive: true });
    const observer = new ResizeObserver(updateArrows);
    observer.observe(el);
    return () => {
      el.removeEventListener("scroll", updateArrows);
      observer.disconnect();
    };
  }, [videos.length, updateArrows]);

  const scroll = (dir: 1 | -1) =>
    scrollerRef.current?.scrollBy({ left: dir * scrollerRef.current.clientWidth * 0.8, behavior: "smooth" });

  return (
    <section className="px-6 py-4">
      <div className="mb-3 flex items-center gap-2">
        <h2 className="mr-auto text-xl font-semibold">{title}</h2>
        {videos.length > 0 && (
          <>
            <Button asChild variant="outline" className="rounded-full">
              <Link href={`/studio/content?tab=${vertical ? "shorts" : "video"}`}>Xem tất cả</Link>
            </Button>
            <Button
              variant="outline"
              size="icon"
              className="rounded-full"
              onClick={() => scroll(-1)}
              disabled={!canLeft}
            >
              <ChevronLeft className="h-4 w-4" />
              <span className="sr-only">Trước</span>
            </Button>
            <Button
              variant="outline"
              size="icon"
              className="rounded-full"
              onClick={() => scroll(1)}
              disabled={!canRight}
            >
              <ChevronRight className="h-4 w-4" />
              <span className="sr-only">Sau</span>
            </Button>
          </>
        )}
      </div>
      {videos.length === 0 ? (
        <p className="text-sm text-gray-500">{emptyText}</p>
      ) : (
        <div ref={scrollerRef} className="no-scrollbar -mx-2 flex gap-2 overflow-x-auto scroll-smooth pb-2">
          {videos.map((video) => (
            <ContentCard key={video.videoId} video={video} vertical={vertical} onDelete={onDelete} />
          ))}
        </div>
      )}
    </section>
  );
}
