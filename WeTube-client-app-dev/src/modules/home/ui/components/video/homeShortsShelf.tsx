"use client";

import ChannelShortCard from "@/modules/channel/ui/channelShortCard";
import VideoCarousel from "@/modules/channel/ui/VideoCarousel";
import { RecommendedVideoItem } from "@/types/video";

function ShortsIcon() {
  return (
    <svg viewBox="0 0 24 24" className="size-6 text-red-600" fill="currentColor" aria-hidden>
      <path d="M17.77 10.32l-1.2-.5L18 9.06a3.74 3.74 0 00-3.5-6.62L6 6.94a3.74 3.74 0 00.23 6.74l1.2.49L6 14.93a3.75 3.75 0 003.5 6.63l8.5-4.5a3.74 3.74 0 00-.23-6.74zM10 14.65v-5.3L15 12l-5 2.65z" />
    </svg>
  );
}

/** Hàng Shorts riêng ở trang chủ: thẻ dọc 9:16 cuộn ngang, bấm mở trình phát Shorts. */
export default function HomeShortsShelf({ shorts }: { shorts: RecommendedVideoItem[] }) {
  if (shorts.length === 0) return null;
  return (
    <div className="my-6">
      <VideoCarousel title="Shorts" icon={<ShortsIcon />}>
        {shorts.map((s) => (
          <ChannelShortCard key={s.id} video={s} className="w-[180px] shrink-0" />
        ))}
      </VideoCarousel>
    </div>
  );
}
