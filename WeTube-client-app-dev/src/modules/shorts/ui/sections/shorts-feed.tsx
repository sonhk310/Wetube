// modules/shorts/ui/sections/shorts-feed.tsx
"use client";
import ShortsVideoCard from "../components/shorts-video-card";
import { useShortsFeed } from "@/modules/shorts/hooks/useShortsFeed";

export default function ShortsFeed() {
  const { shorts, loadMore, loading, error } = useShortsFeed();

  if (error && shorts.length === 0) {
    return <p className="p-6 text-center text-red-500">Không tải được Shorts.</p>;
  }
  if (!loading && shorts.length === 0) {
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
