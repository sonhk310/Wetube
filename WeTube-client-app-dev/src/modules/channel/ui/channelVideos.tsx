"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import {
  useGetChannelInfoQuery,
  useGetChannelVideosQuery,
} from "@/app/api/channelApi";
import VideoCard, {
  VideoCardSkeleton,
} from "@/modules/home/ui/components/video/videoCard";
import type { ChannelInfo, ChannelVideo, ChannelVideoSort } from "@/types/channel";
import ChannelShortCard from "./channelShortCard";

/** Thẻ video thường của một kênh (dùng ở lưới thẻ Video và các hàng ở trang chủ kênh). */
export function ChannelVideoCard({ video, channel }: { video: ChannelVideo; channel: ChannelInfo }) {
  return (
    <VideoCard
      id={video.id}
      title={video.title}
      thumbnailUrl={video.thumbnailUrl}
      videoUrl={video.videoUrl}
      totalView={video.totalView}
      createAt={video.createdAt}
      name={channel.name}
      duration={video.duration}
      picture={channel.picture ?? "https://placehold.co/48x48?text=W"}
      channelId={channel.id}
    />
  );
}

// Danh sách video (hoặc Shorts) của kênh đang xem (lấy từ /api/videos/by-user/{userId}), tải thêm từng trang.
// Đổi type / sort thì gắn key mới ở chỗ dùng để bắt đầu lại từ trang đầu.
export default function ChannelVideos({
  size = 12,
  type = "video",
  sort = "latest",
}: {
  size?: number;
  type?: "video" | "short";
  sort?: ChannelVideoSort;
}) {
  const { channelId } = useParams<{ channelId: string }>();
  const { data: channel } = useGetChannelInfoQuery(channelId);
  const [page, setPage] = useState(0);
  const [items, setItems] = useState<ChannelVideo[]>([]);
  const { data, isFetching } = useGetChannelVideosQuery(
    { userId: channel?.userId ?? "", page, size, type, sort },
    { skip: !channel?.userId }
  );

  useEffect(() => {
    if (!data) return;
    setItems((prev) => {
      const known = new Set(prev.map((v) => v.id));
      return [...prev, ...data.content.filter((v) => !known.has(v.id))];
    });
  }, [data]);

  if (!channel || (isFetching && items.length === 0)) {
    return (
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {Array.from({ length: 6 }).map((_, i) => (
          <VideoCardSkeleton key={i} />
        ))}
      </div>
    );
  }

  if (items.length === 0) {
    return (
      <p className="mt-6 text-center text-gray-500">
        {type === "short" ? "Kênh này chưa có Shorts nào." : "Kênh này chưa có video nào."}
      </p>
    );
  }

  return (
    <div className="space-y-6">
      {type === "short" ? (
        <div className="grid grid-cols-2 gap-x-3 gap-y-6 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6">
          {items.map((v) => (
            <ChannelShortCard key={v.id} video={v} />
          ))}
        </div>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {items.map((v) => (
            <ChannelVideoCard key={v.id} video={v} channel={channel} />
          ))}
        </div>
      )}
      {data && !data.last && (
        <div className="flex justify-center">
          <button
            type="button"
            disabled={isFetching}
            onClick={() => setPage((p) => p + 1)}
            className="h-9 rounded-full bg-gray-100 px-5 text-sm font-medium hover:bg-gray-200 disabled:opacity-60"
          >
            {isFetching ? "Đang tải..." : "Tải thêm"}
          </button>
        </div>
      )}
    </div>
  );
}
