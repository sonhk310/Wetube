"use client";

import Image from "next/image";
import { useParams } from "next/navigation";
import { useGetChannelInfoQuery } from "@/app/api/channelApi";
import { useSubscribeMutation } from "@/app/api/subscriptionsApi";
import { useAuth } from "@/contexts/auth-context";
import { Skeleton } from "@/components/ui/skeleton";
import { useState } from "react";

const FALLBACK_AVATAR = "https://placehold.co/160x160?text=W";

export default function ChannelHeader() {
  const { channelId } = useParams<{ channelId: string }>();
  const { user } = useAuth();
  const { data: channel, isLoading, error } = useGetChannelInfoQuery(channelId);
  const [subscribe, { isLoading: isSubscribing }] = useSubscribeMutation();
  const [subscribed, setSubscribed] = useState(false);

  if (isLoading) {
    return (
      <div className="p-4 pt-0 space-y-4">
        <Skeleton className="w-full h-40 rounded-lg" />
        <Skeleton className="h-20 w-1/2" />
      </div>
    );
  }

  if (error || !channel) {
    return <p className="p-4 text-red-500">Không tìm thấy kênh này.</p>;
  }

  const isOwner = !!user?.sub && user.sub === channel.userId;

  const handleSubscribe = async () => {
    if (!user?.sub) return;
    try {
      await subscribe({ subscriberId: user.sub, channelId: channel.id }).unwrap();
      setSubscribed(true);
    } catch (err) {
      console.error("Subscribe failed:", err);
    }
  };

  return (
    <div className="relative p-4 pt-0">
      <div className="relative w-full h-40 rounded-lg overflow-hidden bg-gray-200">
        {channel.backgroundImgUrl && (
          <Image
            src={channel.backgroundImgUrl}
            alt="Banner"
            fill
            sizes="100vw"
            className="object-cover"
          />
        )}
      </div>
      <div className="flex items-center gap-4 -mt-10 px-4">
        <Image
          src={channel.picture || FALLBACK_AVATAR}
          alt={channel.name}
          width={160}
          height={160}
          className="rounded-full shadow-xl z-10 w-40 h-40 object-cover bg-white"
        />
        <div className="flex flex-col">
          <h1 className="text-2xl font-bold">{channel.name}</h1>
          <p className="text-sm text-gray-500">
            {channel.totalSubscribers} người đăng ký • {channel.totalVideos} video
          </p>
          {channel.description && (
            <p className="text-sm text-gray-500 line-clamp-2 max-w-xl">
              {channel.description}
            </p>
          )}
        </div>
        {!isOwner && user?.sub && (
          <button
            onClick={handleSubscribe}
            disabled={isSubscribing || subscribed}
            className="ml-auto px-4 py-2 bg-black text-white rounded-md disabled:opacity-60"
          >
            {subscribed ? "Đã đăng ký" : "Đăng ký"}
          </button>
        )}
      </div>
    </div>
  );
}
