"use client";

import { useCallback, useEffect, useState } from "react";
import { useInView } from "react-intersection-observer";
import { API_PREFIX } from "@/constants/appConstant";
import { apiFetch } from "@/lib/apiFetch";
import { useAuth } from "@/contexts/auth-context";
import VideoCard, { VideoCardSkeleton } from "@/modules/home/ui/components/video/videoCard";
import type { RecommendedVideoItem } from "@/types/video";

const PAGE_SIZE = 24;

interface FeedPage {
  content: RecommendedVideoItem[];
  last: boolean;
}

/** Bảng tin Đăng ký: video mới nhất của các kênh đã đăng ký (giống trang Kênh đăng ký của YouTube). */
export default function SubscribedFeedPage() {
  const { user, isLoading: authLoading } = useAuth();
  const [videos, setVideos] = useState<RecommendedVideoItem[]>([]);
  const [page, setPage] = useState(0);
  const [last, setLast] = useState(false);
  const [loading, setLoading] = useState(false);
  const [failed, setFailed] = useState(false);
  const { ref, inView } = useInView({ rootMargin: "400px" });

  const loadPage = useCallback(async (nextPage: number) => {
    setLoading(true);
    try {
      const res = await apiFetch(`${API_PREFIX}/feed/subscriptions?page=${nextPage}&size=${PAGE_SIZE}`);
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      const data: FeedPage = await res.json();
      setVideos((prev) => (nextPage === 0 ? data.content : [...prev, ...data.content]));
      setLast(data.last);
      setPage(nextPage);
      setFailed(false);
    } catch (err) {
      console.error("Không tải được bảng tin đăng ký:", err);
      setFailed(true);
    } finally {
      setLoading(false);
    }
  }, []);

  const userId = user?.sub;
  useEffect(() => {
    if (userId) void loadPage(0);
  }, [userId, loadPage]);

  // Cuộn gần cuối thì tải thêm
  useEffect(() => {
    if (inView && !loading && !last && videos.length > 0) void loadPage(page + 1);
  }, [inView, loading, last, videos.length, page, loadPage]);

  if (!authLoading && !user) {
    return (
      <p className="p-10 text-center text-gray-500">
        Đăng nhập để xem video mới từ các kênh bạn đã đăng ký.
      </p>
    );
  }

  const firstLoad = (authLoading || loading) && videos.length === 0;

  return (
    <div className="p-4">
      <h1 className="mb-4 text-xl font-bold">Mới nhất</h1>

      {failed && videos.length === 0 && (
        <p className="py-10 text-center text-red-500">Không tải được bảng tin. Vui lòng thử lại sau.</p>
      )}

      {!firstLoad && !failed && videos.length === 0 && (
        <p className="py-10 text-center text-gray-500">
          Chưa có video nào. Đăng ký kênh để thấy video mới của họ ở đây.
        </p>
      )}

      <div className="flex flex-wrap gap-4">
        {videos.map((video) => (
          <div key={video.id} className="w-full sm:w-[calc(50%-0.5rem)] lg:w-[calc(33.333%-0.7rem)]">
            <VideoCard {...video} />
          </div>
        ))}
        {(firstLoad || (loading && videos.length > 0)) &&
          Array.from({ length: firstLoad ? 9 : 3 }).map((_, i) => (
            <div key={`skeleton-${i}`} className="w-full sm:w-[calc(50%-0.5rem)] lg:w-[calc(33.333%-0.7rem)]">
              <VideoCardSkeleton />
            </div>
          ))}
      </div>

      {!last && videos.length > 0 && <div ref={ref} className="h-10" />}
      {last && videos.length > 0 && (
        <p className="mt-6 text-center text-gray-500">Bạn đã xem hết video mới.</p>
      )}
    </div>
  );
}
