// modules/shorts/ui/sections/shorts-feed.tsx
"use client";
import { useCallback, useEffect, useRef, useState } from "react";
import ShortsVideoCard, { isTypingTarget } from "../components/shorts-video-card";
import { useShortsFeed } from "@/modules/shorts/hooks/useShortsFeed";
import { Skeleton } from "@/components/ui/skeleton";
import { ChevronDown, ChevronUp, Clapperboard } from "lucide-react";
import { useVideoStore } from "@/store/zustand/videoStore";

/** Khung trống giữa màn hình (khi đang tải cũng nền trắng). */
function EmptyState({ title, text }: { title: string; text: string }) {
  return (
    <div className="flex h-screen flex-col items-center justify-center gap-3 bg-white px-6 pb-24 text-center">
      <span className="flex size-20 items-center justify-center rounded-full bg-gray-100">
        <Clapperboard className="size-9 text-gray-500" />
      </span>
      <p className="text-lg font-semibold text-gray-900">{title}</p>
      <p className="max-w-sm text-sm text-gray-500">{text}</p>
    </div>
  );
}

export default function ShortsFeed({ startId }: { startId?: string }) {
  const { shorts, loadMore, loading, hasMore, error } = useShortsFeed(startId);
  const [active, setActive] = useState(0);
  const scrollerRef = useRef<HTMLDivElement>(null);
  const clearVideoDetail = useVideoStore((s) => s.clearVideoDetail);

  // Bình luận dùng chi tiết video trong store: rời trang Shorts thì xoá
  useEffect(() => () => clearVideoDetail(), [clearVideoDetail]);

  // URL theo Short đang xem (như YouTube), không tải lại trang
  const activeId = shorts[active]?.id;
  useEffect(() => {
    if (activeId) window.history.replaceState(null, "", `/shorts/${activeId}`);
  }, [activeId]);

  const goTo = useCallback(
    (index: number) => {
      const box = scrollerRef.current;
      if (!box || index < 0 || index >= shorts.length) return;
      box.scrollTo({ top: index * box.clientHeight, behavior: "smooth" });
    },
    [shorts.length]
  );

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (isTypingTarget(e.target) || e.ctrlKey || e.metaKey || e.altKey) return;
      if (e.key === "ArrowDown") {
        e.preventDefault();
        goTo(active + 1);
      } else if (e.key === "ArrowUp") {
        e.preventDefault();
        goTo(active - 1);
      }
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [active, goTo]);

  if (error && shorts.length === 0) {
    return <EmptyState title="Không tải được Shorts" text="Hãy tải lại trang hoặc thử lại sau." />;
  }
  // Chưa tải trang đầu (hasMore vẫn true) cũng là đang tải, không phải "chưa có Shorts"
  if (shorts.length === 0 && (loading || hasMore)) {
    return (
      <div className="flex h-[calc(100vh-4rem)] items-center justify-center bg-white">
        <Skeleton className="h-[calc(100vh-7rem)] aspect-[9/16] rounded-xl" />
      </div>
    );
  }
  if (shorts.length === 0) {
    return (
      <EmptyState
        title="Chưa có Shorts nào"
        text="Video dọc hoặc vuông dài tối đa 3 phút sẽ tự xuất hiện ở đây."
      />
    );
  }

  return (
    <div className="relative">
      <div
        ref={scrollerRef}
        className="w-full h-[calc(100vh-4rem)] overflow-y-scroll snap-y snap-mandatory [scrollbar-width:none] [&::-webkit-scrollbar]:hidden"
      >
        {shorts.map((item, i) => (
          <div key={item.id} className="h-full snap-start snap-always">
            <ShortsVideoCard
              {...item}
              active={i === active}
              onActive={() => setActive(i)}
              onReachEnd={i === shorts.length - 1 ? loadMore : undefined}
            />
          </div>
        ))}
      </div>

      {/* Nút lên / xuống bên phải như YouTube (màn hình nhỏ thì vuốt) */}
      <div className="absolute right-6 top-1/2 hidden -translate-y-1/2 flex-col gap-4 md:flex">
        <button
          type="button"
          aria-label="Video trước"
          onClick={() => goTo(active - 1)}
          disabled={active === 0}
          className="flex size-14 items-center justify-center rounded-full bg-gray-100 hover:bg-gray-200 disabled:invisible"
        >
          <ChevronUp className="size-7" />
        </button>
        <button
          type="button"
          aria-label="Video tiếp theo"
          onClick={() => goTo(active + 1)}
          disabled={active >= shorts.length - 1}
          className="flex size-14 items-center justify-center rounded-full bg-gray-100 hover:bg-gray-200 disabled:invisible"
        >
          <ChevronDown className="size-7" />
        </button>
      </div>
    </div>
  );
}
