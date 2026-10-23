"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { useInView } from "react-intersection-observer";
import { API_PREFIX } from "@/constants/appConstant";
import { apiFetch } from "@/lib/apiFetch";
import { RecommendedVideoItem } from "@/types/video";
import VideoCard, { VideoCardSkeleton } from "./videoCard";

const PAGE_SIZE = 12;

interface ApiVideo {
  id: string;
  title: string;
  thumbnailUrl: string;
  videoUrl: string;
  totalView: number;
  createdAt: string;
  duration: number;
  author?: { name?: string; picture?: string };
  user?: { name?: string; picture?: string };
}

const toCardItem = (v: ApiVideo): RecommendedVideoItem => ({
  id: v.id,
  title: v.title,
  thumbnailUrl: v.thumbnailUrl,
  videoUrl: v.videoUrl,
  totalView: v.totalView,
  createAt: v.createdAt,
  name: v.author?.name ?? v.user?.name ?? "",
  duration: v.duration,
  picture: v.author?.picture ?? v.user?.picture ?? "",
});

/** Lưới video công khai theo một thẻ (chip ở trang chủ), tải thêm khi cuộn xuống cuối. */
export default function TagVideoGrid({ tag }: { readonly tag: string }) {
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
          `${API_PREFIX}/videos/by-tag?tag=${encodeURIComponent(tag)}&page=${pageToLoad}&size=${PAGE_SIZE}`
        );
        if (!res.ok) throw new Error("Không tải được video theo thẻ");
        const data = await res.json();
        const items: RecommendedVideoItem[] = (data.content ?? []).map(toCardItem);
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
    [tag]
  );

  useEffect(() => {
    void loadPage(0);
  }, [loadPage]);

  useEffect(() => {
    if (inView && hasMore && !isLoading) void loadPage(page + 1);
  }, [inView, hasMore, isLoading, page, loadPage]);

  if (error && videos.length === 0)
    return <p className="p-4 text-red-500">Không tải được video</p>;

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

      {!isLoading && videos.length === 0 && (
        <p className="text-center mt-6 text-gray-500">Chưa có video nào với thẻ #{tag}.</p>
      )}
    </div>
  );
}
