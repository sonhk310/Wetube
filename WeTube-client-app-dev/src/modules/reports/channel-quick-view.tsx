"use client";

import { useEffect, useState } from "react";
import Image from "next/image";
import Link from "next/link";
import { API_PREFIX } from "@/constants/appConstant";
import { apiFetch } from "@/lib/apiFetch";
import { formatCompact } from "@/lib/utils";
import { Skeleton } from "@/components/ui/skeleton";
import type { ChannelInfo } from "@/types/channel";

interface QuickVideo {
  id: string;
  title: string;
  thumbnailUrl: string | null;
  videosStatus: string | null;
  isShort?: boolean;
}

const VIDEO_STATUS_LABELS: Record<string, string> = {
  INACTIVE: "Đã ẩn",
  PRIVATE: "Riêng tư",
  UNLISTED: "Không công khai",
  DRAFT: "Bản nháp",
};

/**
 * Xem nhanh kênh bị báo cáo ngay trong trang admin: ảnh bìa, ảnh đại diện, tên, mô tả và vài video mới nhất
 * (admin xem được cả kênh / video đang bị ẩn).
 */
export default function ChannelQuickView({ channelId }: { readonly channelId: string }) {
  const [channel, setChannel] = useState<ChannelInfo | null>(null);
  const [videos, setVideos] = useState<QuickVideo[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const res = await apiFetch(`${API_PREFIX}/channels/${channelId}`);
        if (!res.ok) throw new Error(res.status === 404 ? "Kênh không còn" : `HTTP ${res.status}`);
        const info: ChannelInfo = await res.json();
        if (cancelled) return;
        setChannel(info);
        if (!info.userId) {
          setVideos([]);
          return;
        }
        const vres = await apiFetch(`${API_PREFIX}/videos/by-user/${info.userId}?page=0&size=6&type=all&sort=latest`);
        const page = vres.ok ? await vres.json() : { content: [] };
        if (!cancelled) setVideos(page.content ?? []);
      } catch (err) {
        if (!cancelled) setError(err instanceof Error ? err.message : "Không tải được kênh");
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [channelId]);

  if (error) return <p className="text-red-500">{error}</p>;
  if (!channel) return <Skeleton className="h-40 w-full rounded-lg" />;

  return (
    <div className="space-y-3 rounded-lg border bg-gray-50 p-3">
      {channel.backgroundImgUrl && (
        <div className="relative aspect-[6/1] w-full overflow-hidden rounded-md bg-gray-200">
          <Image src={channel.backgroundImgUrl} alt="" fill sizes="600px" className="object-cover" />
        </div>
      )}
      <div className="flex items-start gap-3">
        <div className="relative size-14 shrink-0 overflow-hidden rounded-full bg-gray-200">
          {channel.picture && <Image src={channel.picture} alt={channel.name} fill sizes="56px" className="object-cover" />}
        </div>
        <div className="min-w-0 space-y-1">
          <p className="font-semibold break-words">{channel.name}</p>
          <p className="text-xs text-gray-500">
            {channel.handle && `@${channel.handle} · `}
            {formatCompact(channel.totalSubscribers)} người đăng ký · {channel.totalVideos} video công khai
          </p>
          <p className="whitespace-pre-wrap break-words text-gray-700">
            {channel.description || <span className="italic text-gray-500">(chưa có mô tả)</span>}
          </p>
        </div>
      </div>
      {videos === null ? (
        <Skeleton className="h-20 w-full" />
      ) : videos.length === 0 ? (
        <p className="text-xs text-gray-500">Kênh chưa có video.</p>
      ) : (
        <div className="grid grid-cols-2 gap-2 sm:grid-cols-3">
          {videos.map((v) => (
            <Link key={v.id} href={`/watch/${v.id}`} className="group space-y-1">
              <div className="relative aspect-video overflow-hidden rounded bg-gray-200">
                {v.thumbnailUrl && (
                  <Image src={v.thumbnailUrl} alt={v.title} fill sizes="200px" className="object-cover" />
                )}
                {v.videosStatus && VIDEO_STATUS_LABELS[v.videosStatus] && (
                  <span className="absolute left-1 top-1 rounded bg-black/70 px-1.5 text-[11px] text-white">
                    {VIDEO_STATUS_LABELS[v.videosStatus]}
                  </span>
                )}
              </div>
              <p className="line-clamp-2 text-xs group-hover:underline">{v.title}</p>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
