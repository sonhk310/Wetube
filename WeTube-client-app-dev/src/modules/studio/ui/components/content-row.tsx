"use client";

import { useRef, useState } from "react";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import type { VideoFromPlaylist } from "@/types/playlistSummary";
import { ContentCard } from "./content-card";

interface ContentRowProps {
  title: string;
  videos: VideoFromPlaylist[];
  vertical: boolean;
  emptyText: string;
  onDelete: (video: VideoFromPlaylist) => void;
}

/** Một khu (Video hoặc Shorts): hàng cuộn ngang có nút mũi tên; "Xem tất cả" xếp thành lưới. */
export function ContentRow({ title, videos, vertical, emptyText, onDelete }: ContentRowProps) {
  const scrollerRef = useRef<HTMLDivElement>(null);
  const [expanded, setExpanded] = useState(false);

  const scroll = (dir: 1 | -1) =>
    scrollerRef.current?.scrollBy({ left: dir * scrollerRef.current.clientWidth * 0.8, behavior: "smooth" });

  return (
    <section className="px-6 py-4">
      <div className="mb-3 flex items-center gap-2">
        <h2 className="mr-auto text-xl font-semibold">{title}</h2>
        {videos.length > 0 && (
          <>
            <Button variant="outline" className="rounded-full" onClick={() => setExpanded((e) => !e)}>
              {expanded ? "Thu gọn" : "Xem tất cả"}
            </Button>
            {!expanded && (
              <>
                <Button variant="outline" size="icon" className="rounded-full" onClick={() => scroll(-1)}>
                  <ChevronLeft className="h-4 w-4" />
                  <span className="sr-only">Trước</span>
                </Button>
                <Button variant="outline" size="icon" className="rounded-full" onClick={() => scroll(1)}>
                  <ChevronRight className="h-4 w-4" />
                  <span className="sr-only">Sau</span>
                </Button>
              </>
            )}
          </>
        )}
      </div>
      {videos.length === 0 ? (
        <p className="text-sm text-gray-500">{emptyText}</p>
      ) : (
        <div
          ref={scrollerRef}
          className={cn(
            "flex gap-4",
            expanded ? "flex-wrap" : "no-scrollbar overflow-x-auto scroll-smooth pb-2"
          )}
        >
          {videos.map((video) => (
            <ContentCard key={video.videoId} video={video} vertical={vertical} onDelete={onDelete} />
          ))}
        </div>
      )}
    </section>
  );
}
