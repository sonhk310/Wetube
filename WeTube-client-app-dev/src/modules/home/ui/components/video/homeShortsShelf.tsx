"use client";

import { FileVideo } from "lucide-react";
import ChannelShortCard from "@/modules/channel/ui/channelShortCard";
import VideoCarousel from "@/modules/channel/ui/VideoCarousel";
import { RecommendedVideoItem } from "@/types/video";

/** Hàng Shorts riêng ở trang chủ: thẻ dọc 9:16 cuộn ngang, bấm mở trình phát Shorts. */
export default function HomeShortsShelf({ shorts }: { shorts: RecommendedVideoItem[] }) {
  if (shorts.length === 0) return null;
  return (
    <div className="my-6">
      <VideoCarousel title="Shorts" icon={<FileVideo className="size-6" aria-hidden />}>
        {shorts.map((s) => (
          <ChannelShortCard key={s.id} video={s} className="w-[180px] shrink-0" />
        ))}
      </VideoCarousel>
    </div>
  );
}
