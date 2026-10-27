"use client";

import {
  useGetRecommendVideosQuery,
  useGetScoutVideosQuery,
} from "@/app/api/recommentApi";
import { useAuth } from "@/contexts/auth-context";
import { useCallback, useEffect, useMemo, useState } from "react";
import VideoCard, { VideoCardSkeleton } from "./videoCard";
import { useInView } from "react-intersection-observer";
import { RecommendedVideoItem } from "@/types/video";
import { SHORTS_SHELF_AFTER, SHORTS_SHELF_AFTER_MOBILE, splitHomeFeed } from "@/lib/homeFeed";
import { useIsMobile } from "@/hooks/use-mobile";
import HomeShortsShelf from "./homeShortsShelf";
import { SearchIcon } from "lucide-react";

const LOAD_COUNT = 18;

export default function VideoGrid() {
  const { user, isLoading: isAuthLoading } = useAuth();
  const [page, setPage] = useState(1);
  const [allVideos, setAllVideos] = useState<RecommendedVideoItem[]>([]);
  const [hasMore, setHasMore] = useState(true);
  // const [topRanked, setTopRanked] = useState<RecommendedVideoItem[]>([]);

  const userId = user?.sub;

  console.log("User object in VideoGrid:", user);

  // useEffect(() => {
  //   if (!user?.sub) {
  //     // setAllVideos([]);
  //     setTopRanked([]);
  //     setPage(1);
  //     setHasMore(true);
  //     return;
  //   }

  //   // setAllVideos([]);
  //   setTopRanked([]);
  //   setPage(1);
  //   setHasMore(true);
  // }, [user?.sub]);

  useEffect(() => {
    setPage(1);
    setHasMore(true);
    setAllVideos([]);
    // setTopRanked([]);
  }, [userId]);

  const { ref, inView } = useInView({
    threshold: 0,
  });

  console.log("Query Params:", { userId: user?.sub, page, limit: LOAD_COUNT });
  console.log("Is Skipped:", !user?.sub || !hasMore);

  const {
    data: recommendData,
    isLoading: isRecommendLoading,
    isFetching: isRecommendFetching,
    error,
  } = useGetRecommendVideosQuery(
    { userId: userId, page, limit: LOAD_COUNT },
    {
      skip: !user?.sub || !hasMore,
      // refetchOnMountOrArgChange: true,
    }
  );

  // const { data: topRankedData, isLoading: isScoutLoading } =
  //   useGetScoutVideosQuery(
  //     { userId: userId! },
  //     {
  //       skip: !userId,
  //     }
  //   );

  // const isFirstPageLoading =
  //   (isRecommendLoading || isScoutLoading) && page === 1;

  useEffect(() => {
    if (recommendData?.content) {
      setAllVideos((prev) => {
        const combined = [...prev, ...recommendData.content];
        return Array.from(new Map(combined.map((v) => [v.id, v])).values());
      });

      setHasMore(!recommendData.last);
    }
  }, [recommendData]);

  // useEffect(() => {
  //   if (data?.content && data.content.length > 0) {
  //     const newVideos = data.content;
  //     setAllVideos((prevVideos) => {
  //       const combined = [...prevVideos, ...newVideos];
  //       const uniqueVideos = Array.from(
  //         new Map(combined.map((v) => [v.id, v])).values()
  //       );
  //       return uniqueVideos;
  //     });
  //   }

  //   if (!data?.content || data.content.length < LOAD_COUNT) {
  //     setHasMore(false);
  //   }
  // }, [data]);

  // useEffect(() => {
  //   if (topRankedData) {
  //     setTopRanked(topRankedData);
  //   }
  // }, [topRankedData]);

  useEffect(() => {
    if (
      inView &&
      hasMore &&
      !isRecommendFetching &&
      recommendData &&
      page < recommendData.totalPages
    ) {
      console.log("Loading next page:", page + 1);

      setPage((prevPage) => prevPage + 1);
    }
  }, [inView, hasMore, isRecommendFetching, recommendData, page]);

  // const displayedVideos = useMemo(() => {
  //   const topRankedIds = new Set(topRanked.map((v) => v.id));
  //   const otherVideos = allVideos.filter((v) => !topRankedIds.has(v.id));
  //   return [...topRanked, ...otherVideos];
  // }, [allVideos, topRanked]);

  const displayedVideos = useMemo(() => allVideos, [allVideos]);
  // Shorts gom vào một hàng riêng chen giữa lưới, không trộn với video thường
  const isMobile = useIsMobile();
  const feed = useMemo(
    () => splitHomeFeed(displayedVideos, isMobile ? SHORTS_SHELF_AFTER_MOBILE : SHORTS_SHELF_AFTER),
    [displayedVideos, isMobile]
  );

  if ((isRecommendLoading || isAuthLoading) && page === 1)
    return <VideoGridSkeleton />;

  if (!isAuthLoading && !user)
    return <p className="p-4">Hãy đăng nhập để tham gia cộng đồng video WeTube ❤</p>;

  if (error) return <p className="p-4 text-red-500">Không tải được video</p>;

  const isEmpty = !isRecommendFetching && displayedVideos.length === 0;

  console.log({
    inView: inView,
    hasMore: hasMore,
    isFetching: isRecommendFetching,
    "API báo hết?": recommendData?.last,
  });

  return (
    <div className="p-4">
      <VideoRow videos={feed.before} />
      <HomeShortsShelf shorts={feed.shorts} />
      <VideoRow videos={feed.after} />

      {isRecommendFetching && page > 1 && (
        <div className="flex flex-wrap gap-4 mt-4">
          {Array.from({ length: 3 }).map((_, i) => (
            <div
              key={`fetching-skeleton-${i}`}
              className="w-full sm:w-[calc(33.333%-1rem)]"
            >
              <VideoCardSkeleton />
            </div>
          ))}
        </div>
      )}

      {hasMore && !isRecommendFetching && displayedVideos.length > 0 && (
        <div
          ref={ref}
          className="h-10"
        />
      )}
      {/* Như YouTube: chưa xem video nào thì backend chưa gợi ý, mời tìm kiếm trước */}
      {isEmpty && (
        <div className="mx-auto mt-16 flex max-w-md flex-col items-center gap-3 text-center">
          <span className="flex size-20 items-center justify-center rounded-full bg-gray-100">
            <SearchIcon className="size-9 text-gray-600" />
          </span>
          <h2 className="text-xl font-semibold">Hãy thử tìm kiếm để bắt đầu</h2>
          <p className="text-sm text-gray-600">
            Hãy bắt đầu xem video để chúng tôi xây dựng một nguồn cấp dữ liệu gồm những video mà bạn sẽ thích.
          </p>
        </div>
      )}

      {!hasMore && displayedVideos.length > 0 && (
        <p className="text-center mt-6 text-gray-500">
          Bạn đã xem hết video gợi ý.
        </p>
      )}
    </div>
  );
}

const VideoRow = ({ videos }: { videos: RecommendedVideoItem[] }) =>
  videos.length === 0 ? null : (
    <div className="flex flex-wrap gap-4">
      {videos.map((video) => (
        <div key={video.id} className="w-full sm:w-[calc(33.333%-1rem)]">
          <VideoCard {...video} />
        </div>
      ))}
    </div>
  );

const VideoGridSkeleton = () => (
  <div className="p-4">
    <div className="flex flex-wrap gap-4">
      {Array.from({ length: 12 }).map((_, i) => (
        <div
          key={i}
          className="w-full sm:w-[calc(33.333%-1rem)]"
        >
          <VideoCardSkeleton />
        </div>
      ))}
    </div>
  </div>
);
