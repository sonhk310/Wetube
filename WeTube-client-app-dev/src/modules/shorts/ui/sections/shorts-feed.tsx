// modules/shorts/ui/sections/shorts-feed.tsx
"use client";
import ShortsVideoCard from "../components/shorts-video-card";
import { useShortsFeed } from "@/modules/shorts/hooks/useShortsFeed";
import { Skeleton } from "@/components/ui/skeleton";
import { Clapperboard } from "lucide-react";

/** Khung trống giữa màn hình, nền trắng như lúc đang tải (trang Shorts có nền đen). */
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

export default function ShortsFeed() {
  const { shorts, loadMore, loading, hasMore, error } = useShortsFeed();

  if (error && shorts.length === 0) {
    return <EmptyState title="Không tải được Shorts" text="Hãy tải lại trang hoặc thử lại sau." />;
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
      <EmptyState
        title="Chưa có Shorts nào"
        text="Video dọc hoặc vuông dài tối đa 3 phút sẽ tự xuất hiện ở đây."
      />
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
