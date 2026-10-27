"use client";

import Image from "next/image";
import Link from "next/link";
import { useParams, usePathname } from "next/navigation";
import { useEffect, useState } from "react";
import { ChevronRight } from "lucide-react";
import { useGetChannelInfoQuery } from "@/app/api/channelApi";
import { Skeleton } from "@/components/ui/skeleton";
import { useAuth } from "@/contexts/auth-context";
import { formatCompact } from "@/lib/utils";
import ChannelAboutDialog from "./channelAboutDialog";
import SubscribeControl from "./subscribeControl";

const FALLBACK_AVATAR = "https://placehold.co/160x160?text=W";

export default function ChannelHeader() {
  const { channelId } = useParams<{ channelId: string }>();
  const pathname = usePathname();
  const { user } = useAuth();
  const { data: channel, isLoading, error, refetch } = useGetChannelInfoQuery(channelId);
  const [aboutOpen, setAboutOpen] = useState(false);

  // Mở bằng /channel/{id} mà kênh đã có handle thì đổi thanh địa chỉ sang /@handle (không tải lại trang)
  const handle = channel?.handle;
  useEffect(() => {
    if (handle && channelId !== handle && pathname.startsWith(`/channel/${channelId}`)) {
      window.history.replaceState(null, "", pathname.replace(`/channel/${channelId}`, `/@${handle}`) + window.location.search);
    }
  }, [handle, channelId, pathname]);

  if (isLoading) {
    return (
      <div className="space-y-4">
        <Skeleton className="aspect-[16/5] w-full rounded-xl md:aspect-[6/1]" />
        <div className="flex gap-6">
          <Skeleton className="size-20 shrink-0 rounded-full sm:size-40" />
          <div className="flex-1 space-y-3">
            <Skeleton className="h-9 w-1/2" />
            <Skeleton className="h-5 w-2/3" />
          </div>
        </div>
      </div>
    );
  }

  if (error || !channel) {
    return <p className="p-4 text-red-500">Không tìm thấy kênh này.</p>;
  }

  const isOwner = !!user?.sub && user.sub === channel.userId;

  return (
    <div className="space-y-4">
      <div className="relative aspect-[16/5] w-full overflow-hidden rounded-xl bg-gradient-to-r from-gray-200 via-gray-100 to-gray-300 md:aspect-[6/1]">
        {channel.backgroundImgUrl && (
          <Image
            src={channel.backgroundImgUrl}
            alt="Ảnh bìa"
            fill
            sizes="100vw"
            priority
            className="object-cover"
          />
        )}
      </div>

      <div className="flex flex-col items-center gap-4 sm:flex-row sm:items-start sm:gap-6">
        <Image
          src={channel.picture || FALLBACK_AVATAR}
          alt={channel.name}
          width={160}
          height={160}
          className="size-20 shrink-0 rounded-full bg-white object-cover sm:size-40"
        />
        <div className="min-w-0 flex-1 text-center sm:text-left">
          <h1 className="break-words text-2xl font-bold sm:text-4xl">{channel.name}</h1>
          <p className="mt-1 text-sm text-gray-600">
            {channel.handle && <span className="font-medium text-gray-900">@{channel.handle} • </span>}
            {formatCompact(channel.totalSubscribers)} người đăng ký • {formatCompact(channel.totalVideos)} video
          </p>
          <button
            type="button"
            onClick={() => setAboutOpen(true)}
            className="group mt-2 flex max-w-2xl cursor-pointer items-center gap-1 text-left text-sm text-gray-600 hover:text-gray-900 max-sm:mx-auto"
          >
            <span className="line-clamp-1">{channel.description || "Tìm hiểu thêm về kênh này"}</span>
            {channel.description ? (
              <span className="shrink-0 font-medium text-gray-900 group-hover:underline">...xem thêm</span>
            ) : (
              <ChevronRight className="size-4 shrink-0" />
            )}
          </button>
          <div className="mt-3 flex flex-wrap items-center justify-center gap-2 sm:justify-start">
            {isOwner ? (
              <>
                <Link
                  href="/studio/customization"
                  className="inline-flex h-9 items-center rounded-full bg-gray-100 px-4 text-sm font-medium hover:bg-gray-200"
                >
                  Tuỳ chỉnh kênh
                </Link>
                <Link
                  href="/studio/content"
                  className="inline-flex h-9 items-center rounded-full bg-gray-100 px-4 text-sm font-medium hover:bg-gray-200"
                >
                  Quản lý video
                </Link>
              </>
            ) : (
              <SubscribeControl
                key={channel.id}
                channelId={channel.id}
                subscribed={channel.subscribed ?? false}
                notificationMode={channel.notificationMode}
                onChanged={() => void refetch()}
              />
            )}
          </div>
        </div>
      </div>

      <ChannelAboutDialog channel={channel} isOwner={isOwner} open={aboutOpen} onOpenChange={setAboutOpen} />
    </div>
  );
}
