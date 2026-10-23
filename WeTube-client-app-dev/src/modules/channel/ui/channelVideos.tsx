"use client";

import { useParams } from "next/navigation";
import {
  useGetChannelInfoQuery,
  useGetChannelVideosQuery,
} from "@/app/api/channelApi";
import VideoCard, {
  VideoCardSkeleton,
} from "@/modules/home/ui/components/video/videoCard";

// Danh sách video của kênh đang xem (lấy từ /api/videos/by-user/{userId}).
export default function ChannelVideos({ size = 12 }: { size?: number }) {
  const { channelId } = useParams<{ channelId: string }>();
  const { data: channel } = useGetChannelInfoQuery(channelId);
  const { data, isLoading } = useGetChannelVideosQuery(
    { userId: channel?.userId ?? "", size },
    { skip: !channel?.userId }
  );

  if (isLoading || !channel) {
    return (
      <div className="flex flex-wrap gap-4 p-4">
        {Array.from({ length: 6 }).map((_, i) => (
          <div key={i} className="w-full sm:w-[calc(33.333%-1rem)]">
            <VideoCardSkeleton />
          </div>
        ))}
      </div>
    );
  }

  const videos = data?.content ?? [];
  if (videos.length === 0) {
    return (
      <p className="text-center mt-6 text-gray-500">
        Kênh này chưa có video nào.
      </p>
    );
  }

  return (
    <div className="flex flex-wrap gap-4 p-4">
      {videos.map((v) => (
        <div key={v.id} className="w-full sm:w-[calc(33.333%-1rem)]">
          <VideoCard
            id={v.id}
            title={v.title}
            thumbnailUrl={v.thumbnailUrl}
            videoUrl={v.videoUrl}
            totalView={v.totalView}
            createAt={v.createdAt}
            name={channel.name}
            duration={v.duration}
            picture={channel.picture ?? "https://placehold.co/48x48?text=W"}
          />
        </div>
      ))}
    </div>
  );
}
