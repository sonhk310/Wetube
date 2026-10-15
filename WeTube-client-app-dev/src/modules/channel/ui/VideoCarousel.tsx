"use client";

import { useRef } from "react";
import { ChevronLeft, ChevronRight } from "lucide-react";
import Image from "next/image";

interface Video {
  id: string;
  title: string;
  thumbnail: string;
  views: number;
  uploadedAt: string;
}

interface VideoCarouselProps {
  title: string;
  videos: Video[];
}

const formatViews = (num: number) => {
  if (num >= 1_000_000)
    return (num / 1_000_000).toFixed(1).replace(".0", "") + "M views";
  if (num >= 1_000)
    return (num / 1_000).toFixed(1).replace(".0", "") + "K views";
  return num + " views";
};

export default function VideoCarousel({ title, videos }: VideoCarouselProps) {
  const scrollRef = useRef<HTMLDivElement>(null);

  // Không hiển thị gì nếu không có video
  if (!videos || videos.length === 0) {
    return null;
  }

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
    <div className="relative pt-4 border-t border-gray-200">
      <h3 className="text-xl font-bold mb-3">{title}</h3>{" "}
      {/* Dùng title từ props */}
      <div className="relative">
        <button
          onClick={() => scroll("left")}
          className="absolute left-0 top-1/2 -translate-y-1/2 z-10 bg-white dark:bg-black/70 p-1 rounded-full shadow-md hover:scale-105 transition-transform"
        >
          <ChevronLeft size={34} />
        </button>

        <div
          ref={scrollRef}
          className="flex gap-4 overflow-x-auto scroll-smooth snap-x snap-mandatory px-6 py-1 no-scrollbar"
        >
          {/* Lặp qua mảng videos từ props */}
          {videos.map((video) => (
            <div
              key={video.id}
              className="w-[350px] snap-start shrink-0 flex flex-col gap-2 cursor-pointer group"
            >
              <div className="w-full h-[200px] overflow-hidden rounded-lg relative">
                <Image
                  src={video.thumbnail}
                  alt={video.title}
                  fill
                  sizes="(max-width: 768px) 100vw, 50vw, 33vw"
                  className="object-cover group-hover:scale-105 transition-transform duration-300"
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
          className="absolute right-0 top-1/2 -translate-y-1/2 z-10 bg-white dark:bg-black/70 p-1 rounded-full shadow-md hover:scale-105 transition-transform"
        >
          <ChevronRight size={34} />
        </button>
      </div>
    </div>
  );
}
