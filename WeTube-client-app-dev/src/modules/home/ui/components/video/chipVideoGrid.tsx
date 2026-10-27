"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { useInView } from "react-intersection-observer";
import { API_PREFIX } from "@/constants/appConstant";
import { apiFetch } from "@/lib/apiFetch";
import { RecommendedVideoItem } from "@/types/video";
import VideoCard, { VideoCardSkeleton } from "./videoCard";

const PAGE_SIZE = 18;

/**
 * Lưới video của một chip ở trang chủ (Kênh đăng ký, Mới tải lên, Chưa xem, Đã xem), tải thêm khi cuộn xuống cuối.
 * Dữ liệu từ GET /api/home/feed, cùng dạng với API gợi ý nên dùng thẳng VideoCard.
 */
export default function ChipVideoGrid({ chip, seed }: { readonly chip: string; readonly seed: string }) {
  const [videos, setVideos] = useState<RecommendedVideoItem[]>([]);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(false);
  const loadingRef = useRef(false);
  const { ref, inView } = useInView({ threshold: 0 });

  const loadPage = useCallback(
    async (pageToLoad: number) => {
      if (loadingRef.current) return;
      loadingRef.current = true;
      setIsLoading(true);
      try {
        const res = await apiFetch(
          `${API_PREFIX}/home/feed?chip=${encodeURIComponent(chip)}&page=${pageToLoad}&size=${PAGE_SIZE}&seed=${encodeURIComponent(seed)}`
        );
        if (!res.ok) throw new Error("Không tải được video của chip");
        const data = await res.json();
        const items: RecommendedVideoItem[] = data.content ?? [];
        setVideos((prev) => {
          const merged = pageToLoad === 0 ? items : [...prev, ...items];
          return Array.from(new Map(merged.map((v) => [v.id, v])).values());
        });
        setPage(pageToLoad);
        setHasMore(!data.last);
        setError(false);
      } catch (err) {
        console.error(err);
        setError(true);
        setHasMore(false);
      } finally {
        loadingRef.current = false;
        setIsLoading(false);
      }
    },
    [chip, seed]
  );

  useEffect(() => {
    void loadPage(0);
  }, [loadPage]);

  useEffect(() => {
    if (inView && hasMore && !isLoading) void loadPage(page + 1);
  }, [inView, hasMore, isLoading, page, loadPage]);

  if (error && videos.length === 0) return <p className="p-4 text-red-500">Không tải được video</p>;

  return (
    <div className="p-4">
      <div className="flex flex-wrap gap-4">
        {videos.map((video) => (
          <div key={video.id} className="w-full sm:w-[calc(33.333%-1rem)]">
            <VideoCard {...video} />
          </div>
        ))}
        {isLoading &&
          Array.from({ length: 3 }).map((_, i) => (
            <div key={`skeleton-${i}`} className="w-full sm:w-[calc(33.333%-1rem)]">
              <VideoCardSkeleton />
            </div>
          ))}
      </div>

      {hasMore && !isLoading && videos.length > 0 && <div ref={ref} className="h-10" />}

      {!isLoading && videos.length === 0 && <p className="mt-6 text-center text-gray-500">Chưa có video nào ở mục này.</p>}
    </div>
  );
}
