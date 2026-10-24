// modules/shorts/ui/sections/shorts-feed.tsx
"use client";
import ShortsVideoCard from "../components/shorts-video-card";
import { useShortsFeed } from "@/modules/shorts/hooks/useShortsFeed";
import { Skeleton } from "@/components/ui/skeleton";

export default function ShortsFeed() {
  const { shorts, loadMore, loading, hasMore, error } = useShortsFeed();

  if (error && shorts.length === 0) {
    return <p className="p-6 text-center text-red-500">Không tải được Shorts.</p>;
  }
  // Chưa tải trang đầu (hasMore vẫn true) cũng là đang tải, không phải "chưa có Shorts"
  if (shorts.length === 0 && (loading || hasMore)) {
    return (
      <div className="flex h-screen items-center justify-center bg-white">
        <Skeleton className="h-[85vh] aspect-[9/16] rounded-xl" />
      </div>
    );
  }
  if (shorts.length === 0) {
    return (
      <p className="p-6 text-center text-gray-500">
        Chưa có Shorts nào. Video dài tối đa 60 giây sẽ tự xuất hiện ở đây.
      </p>
    );
  }

  return (
    <div className="w-full h-screen overflow-y-scroll snap-y snap-mandatory">
      {shorts.map((item, i) => (
        <div key={item.id} className="h-screen snap-start bg-white ">
          <ShortsVideoCard
            {...item}
            onReachEnd={i === shorts.length - 1 ? loadMore : undefined}
          />
        </div>
      ))}
    </div>
  );
}
