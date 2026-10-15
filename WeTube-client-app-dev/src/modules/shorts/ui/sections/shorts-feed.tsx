// modules/shorts/ui/sections/shorts-feed.tsx
"use client";
import ShortsVideoCard from "../components/shorts-video-card";
import { useShortsFeed } from "@/modules/shorts/hooks/useShortsFeed";

export default function ShortsFeed() {
  const shorts = useShortsFeed();

  return (
    <div className="w-full h-screen overflow-y-scroll snap-y snap-mandatory">
      {shorts.map((item) => (
        <div key={item.id} className="h-screen snap-start bg-white ">
          <ShortsVideoCard {...item} />
        </div>
      ))}
    </div>
  );
}
