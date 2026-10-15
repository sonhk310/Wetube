// modules/channel/ui/components/suggested-videos.tsx
"use client";

import { useRef } from "react";
import { ChevronLeft, ChevronRight } from "lucide-react";
import Image from 'next/image';

const mockSuggested = [
  {
    id: "1",
    title: "PHÂN TÍCH XU HƯỚNG GIÁ VÀNG NGÀY MỚI 11/12/2024",
    channelName: "Nature World",
    thumbnail:
      "https://i.ytimg.com/vi/yZYs5FOjWqQ/hqdefault.jpg?sqp=-oaymwEcCNACELwBSFXyq4qpAw4IARUAAIhCGAFwAcABBg==&rs=AOn4CLAf9COX21yILAwkOBhFN8Epz0xXag",
    avatar: "https://placehold.co/80x80/nature/fff.png?text=N",
    videoUrl: "/videos/nature.mp4",
    views: 100000,
    uploadedAt: "3 months ago",
  },
  {
    id: "2",
    title: "Tuyệt kĩ tìm điểm Buy-Sell vàng trong Forex với vùng Key Level.",
    channelName: "Nature World",
    thumbnail:
      "https://i.ytimg.com/vi/hOZvkLQxCNo/hqdefault.jpg?sqp=-oaymwEcCNACELwBSFXyq4qpAw4IARUAAIhCGAFwAcABBg==&rs=AOn4CLDw_-mPwHmG4dhvUOVGYjK8vPTjDg",
    avatar: "https://placehold.co/80x80/nature/fff.png?text=N",
    videoUrl: "/videos/nature.mp4",
    views: 100000,
    uploadedAt: "3 months ago",
  },
  {
    id: "3",
    title: "Tuyệt kĩ tìm điểm Buy-Sell vàng trong Forex với vùng Key Level.",
    channelName: "Nature World",
    thumbnail:
      "https://i.ytimg.com/vi/hOZvkLQxCNo/hqdefault.jpg?sqp=-oaymwEcCNACELwBSFXyq4qpAw4IARUAAIhCGAFwAcABBg==&rs=AOn4CLDw_-mPwHmG4dhvUOVGYjK8vPTjDg",
    avatar: "https://placehold.co/80x80/nature/fff.png?text=N",
    videoUrl: "/videos/nature.mp4",
    views: 100000,
    uploadedAt: "3 months ago",
  },
  {
    id: "4",
    title: "Tuyệt kĩ tìm điểm Buy-Sell vàng trong Forex với vùng Key Level.",
    channelName: "Nature World",
    thumbnail:
      "https://i.ytimg.com/vi/hOZvkLQxCNo/hqdefault.jpg?sqp=-oaymwEcCNACELwBSFXyq4qpAw4IARUAAIhCGAFwAcABBg==&rs=AOn4CLDw_-mPwHmG4dhvUOVGYjK8vPTjDg",
    avatar: "https://placehold.co/80x80/nature/fff.png?text=N",
    videoUrl: "/videos/nature.mp4",
    views: 100000,
    uploadedAt: "3 months ago",
  },
  {
    id: "5",
    title: "Tuyệt kĩ tìm điểm Buy-Sell vàng trong Forex với vùng Key Level.",
    channelName: "Nature World",
    thumbnail:
      "https://i.ytimg.com/vi/hOZvkLQxCNo/hqdefault.jpg?sqp=-oaymwEcCNACELwBSFXyq4qpAw4IARUAAIhCGAFwAcABBg==&rs=AOn4CLDw_-mPwHmG4dhvUOVGYjK8vPTjDg",
    avatar: "https://placehold.co/80x80/nature/fff.png?text=N",
    videoUrl: "/videos/nature.mp4",
    views: 100000,
    uploadedAt: "3 months ago",
  },
];

const formatViews = (num: number) => {
  if (num >= 1_000_000) return (num / 1_000_000).toFixed(1) + "M views";
  if (num >= 1_000) return (num / 1_000).toFixed(1) + "K views";
  return num + " views";
};

export default function HighlightedVideos() {
  const scrollRef = useRef<HTMLDivElement>(null);

  const scroll = (direction: "left" | "right") => {
    if (scrollRef.current) {
      const amount = 320; 
      scrollRef.current.scrollBy({
        left: direction === "left" ? -amount : amount,
        behavior: "smooth",
      });
    }
  };

  return (
    <div className="relative pt-2 border-t border-gray-300">
      <h3 className="text-lg font-semibold mb-3">Video Nổi Bật</h3>
      <div className="relative">
        <button
          onClick={() => scroll("left")}
          className="absolute left-0 top-1/2 -translate-y-1/2 z-10 bg-white dark:bg-black/70 p-1 rounded-full shadow-md hover:scale-105"
        >
          <ChevronLeft size={34} />
        </button>

        <div
          ref={scrollRef}
          className="flex gap-4 overflow-x-auto scroll-smooth snap-x snap-mandatory px-6 py-1 no-scrollbar"
        >
          {mockSuggested.map((video) => (
            <div
              key={video.id}
              className="w-[350px] snap-start shrink-0 flex flex-col gap-2"
            >
              <div className="w-full h-[200px] overflow-hidden rounded-lg relative">
                <Image
                  src={video.thumbnail}
                  alt={video.title}
                  fill 
                  sizes="(max-width: 768px) 100vw, (max-width: 1200px) 50vw, 33vw"
                  className="object-cover" 
                />
              </div>
              <div>
                <p className="text-sm font-medium line-clamp-2">
                  {video.title}
                </p>
                <p className="text-xs text-gray-500 pt-2">
                  {formatViews(video.views)} • {video.uploadedAt}
                </p>
              </div>
            </div>
          ))}
        </div>

        <button
          onClick={() => scroll("right")}
          className="absolute right-0 top-1/2 -translate-y-1/2 z-10 bg-white dark:bg-black/70 p-1 rounded-full shadow-md hover:scale-105"
        >
          <ChevronRight size={34} />
        </button>
      </div>
    </div>
  );
}
