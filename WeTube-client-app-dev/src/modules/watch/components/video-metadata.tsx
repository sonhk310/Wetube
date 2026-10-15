"use client";

import Image from "next/image";
import {
  BellIcon,
  ChevronDownIcon,
  EllipsisHorizontalIcon,
  HandThumbDownIcon,
  HandThumbUpIcon,
  HeartIcon,
} from "@heroicons/react/24/outline";
import { useVideoStore } from "@/store/zustand/videoStore";
import { timeAgo } from "@/lib/utils";
import { useEffect, useState } from "react";
import {
  useSubscribeMutation,
  useUnsubscribeMutation,
} from "@/app/api/subscriptionsApi";
import TranscriptModal from "./transcriptModal";

export default function VideoMetadata() {
  const videoDetail = useVideoStore((state) => state.videoDetail);
  const [isSubscribed, setIsSubscribed] = useState(false);

  useEffect(() => {
    if (videoDetail?.detail?.subscribed !== undefined) {
      setIsSubscribed(videoDetail.detail.subscribed);
    }
  }, [videoDetail]);

  const [subscribe] = useSubscribeMutation();
  const [unsubscribe] = useUnsubscribeMutation();

  if (!videoDetail) return null;

  // const {title, description, totalView, createAt, name, picture, totalSubscribers, subscribed, channelId} =
  const {
    title,
    description,
    totalView,
    createAt,
    name,
    picture,
    totalSubscribers,
    channelId,
  } = videoDetail.detail;

  const handleSubscribe = async () => {
    try {
      if (!isSubscribed) {
        await subscribe({
          subscriberId: "current-user-id",
          channelId,
          tierId: undefined,
        }).unwrap();
        setIsSubscribed(true);
      } else {
        await unsubscribe({
          subscriberId: "current-user-id",
          channelId,
        }).unwrap();
        setIsSubscribed(false);
      }
    } catch (err) {
      console.error("Failed to update subscription:", err);
    }
  };

  return (
    <section className="w-full mt-2">
      {/* Title */}
      <p className="text-2xl font-bold tracking-tight">{title}</p>

      <div className="py-1 flex justify-between items-center mt-2">
        <section className="flex justify-start items-center gap-3">
          <Image
            className="rounded-full h-10 w-10 cursor-pointer"
            src={picture}
            alt={name}
            height={40}
            width={40}
          />
          <div className="hidden md:block">
            <section className="flex flex-col tracking-tight mr-4">
              <p className="font-bold font-lg cursor-pointer">{name}</p>

              <p className="text-gray-500 text-xs font-semibold">
                {totalSubscribers}
              </p>
            </section>
          </div>
          <button
            onClick={handleSubscribe}
            className={`rounded-full flex gap-2 py-2 px-3 cursor-pointer ${
              isSubscribed
                ? "bg-gray-100 hover:bg-gray-200"
                : "bg-black text-white hover:bg-gray-800"
            } items-center`}
          >
            {isSubscribed && <BellIcon className="size-5" />}
            <p className="text-sm font-bold">
              {isSubscribed ? "Subscribed" : "Subscribe"}
            </p>
            {isSubscribed && <ChevronDownIcon className="size-4.5" />}
          </button>
        </section>

        <section className="flex justify-end items-center gap-3 ">
          <div className="cursor-pointer flex justify-between bg-gray-100 rounded-full overflow-hidden">
            <button className="flex gap-2 py-2 px-3 bg-gray-100 hover:bg-gray-200 rounded-l-full cursor-pointer">
              <HandThumbUpIcon className="size-5" />
              <p className="text-sm font-bold">17K</p>
            </button>
            <div className="border-l-[1px] border-gray-300 my-2" />
            <button className="py-2 px-3 bg-gray-100 hover:bg-gray-200 rounded-r-full cursor-pointer">
              <HandThumbDownIcon className="size-5" />
            </button>
          </div>
          <button className="bg-gray-100 hover:bg-gray-200 p-2 rounded-full cursor-pointer flex gap-2 py-2 px-2.5">
            <p className="text-sm font-bold">Share</p>
          </button>
          <div className="hidden md:block">
            <button className="bg-gray-100 hover:bg-gray-200 p-2 rounded-full cursor-pointer flex gap-2 py-2 px-2.5">
              <HeartIcon className="size-5" />
              <p className="text-sm font-bold">Thanks</p>
            </button>
          </div>
          <button className="bg-gray-100 hover:bg-gray-200 p-2 rounded-full cursor-pointer">
            <EllipsisHorizontalIcon className="size-6" />
          </button>
            {/*<TranscriptModal videoId={videoDetail.detail.id} />*/}
        </section>
      </div>

      {/* Description */}
      <div className="bg-gray-100 p-3 mt-2 w-full rounded-xl text-sm">
        <p className="font-semibold">
          {totalView.toLocaleString()} views · {timeAgo(createAt)}
        </p>
        <p>{description}</p>
      </div>
    </section>
  );
}
