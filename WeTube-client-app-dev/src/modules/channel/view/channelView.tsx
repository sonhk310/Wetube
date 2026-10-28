"use client";

import { useParams } from "next/navigation";
import { FileVideo } from "lucide-react";
import { useGetChannelInfoQuery, useGetChannelVideosQuery } from "@/app/api/channelApi";
import { VideoCardSkeleton } from "@/modules/home/ui/components/video/videoCard";
import ChannelShortCard from "../ui/channelShortCard";
import { ChannelVideoCard } from "../ui/channelVideos";
import VideoCarousel from "../ui/VideoCarousel";

const ROW_SIZE = 12;

// Trang chủ của kênh: các hàng cuộn ngang "Video mới nhất", "Phổ biến", "Shorts"; hàng nào trống thì ẩn.
export default function ChannelView() {
  const { channelId } = useParams<{ channelId: string }>();
  const { data: channel } = useGetChannelInfoQuery(channelId);
  const userId = channel?.userId ?? "";
  const skip = !channel?.userId;
  const latest = useGetChannelVideosQuery({ userId, size: ROW_SIZE, type: "video" }, { skip });
  const popular = useGetChannelVideosQuery({ userId, size: ROW_SIZE, type: "video", sort: "popular" }, { skip });
  const shorts = useGetChannelVideosQuery({ userId, size: ROW_SIZE, type: "short" }, { skip });

  if (!channel || latest.isLoading || popular.isLoading || shorts.isLoading) {
    return (
      <div className="flex gap-4 overflow-hidden">
        {Array.from({ length: 4 }).map((_, i) => (
          <div key={i} className="w-[300px] shrink-0">
            <VideoCardSkeleton />
          </div>
        ))}
      </div>
    );
  }

  const latestVideos = latest.data?.content ?? [];
  // Kênh ít video thì "Phổ biến" chỉ là xếp lại hàng "Mới nhất": bỏ để khỏi lặp
  const popularVideos = latest.data?.last ? [] : (popular.data?.content ?? []);
  const shortVideos = shorts.data?.content ?? [];

  if (latestVideos.length === 0 && shortVideos.length === 0) {
    return <p className="mt-6 text-center text-gray-500">Kênh này chưa có video nào.</p>;
  }

  return (
    <div className="space-y-6">
      {latestVideos.length > 0 && (
        <VideoCarousel title="Video mới nhất">
          {latestVideos.map((v) => (
            <div key={v.id} className="w-[300px] shrink-0">
              <ChannelVideoCard video={v} channel={channel} />
            </div>
          ))}
        </VideoCarousel>
      )}
      {popularVideos.length > 0 && (
        <VideoCarousel title="Phổ biến">
          {popularVideos.map((v) => (
            <div key={v.id} className="w-[300px] shrink-0">
              <ChannelVideoCard video={v} channel={channel} />
            </div>
          ))}
        </VideoCarousel>
      )}
      {shortVideos.length > 0 && (
        <VideoCarousel title="Shorts" icon={<FileVideo className="size-6" aria-hidden />}>
          {shortVideos.map((v) => (
            <ChannelShortCard key={v.id} video={v} className="w-[180px] shrink-0" />
          ))}
        </VideoCarousel>
      )}
    </div>
  );
}
