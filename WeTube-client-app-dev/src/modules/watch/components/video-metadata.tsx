"use client";

import Image from "next/image";
import {
  BellIcon,
  ChevronDownIcon,
  HandThumbDownIcon,
  HandThumbUpIcon,
} from "@heroicons/react/24/outline";
import {
  HandThumbDownIcon as HandThumbDownSolid,
  HandThumbUpIcon as HandThumbUpSolid,
} from "@heroicons/react/24/solid";
import { useVideoStore } from "@/store/zustand/videoStore";
import { timeAgo } from "@/lib/utils";
import { useEffect, useState } from "react";
import {
  useSubscribeMutation,
  useUnsubscribeMutation,
} from "@/app/api/subscriptionsApi";
import { useGetLikeInfoQuery, useToggleLikeMutation } from "@/app/api/likeApi";
import { useAuth } from "@/contexts/auth-context";
import VideoMoreMenu from "./video-more-menu";
import LinkifiedText from "./linkified-text";
import ShareDialog from "./share-dialog";
import SaveToPlaylistDialog from "./save-to-playlist-dialog";
import { useToggleDislikeMutation } from "@/app/api/videoApi";
import { useControls } from "@/hooks/use-controls";
import toast from "react-hot-toast";
import { playlistService } from "@/modules/playlists/ui/list/playlist-API";

export default function VideoMetadata() {
  const videoDetail = useVideoStore((state) => state.videoDetail);
  const [isSubscribed, setIsSubscribed] = useState(false);
  const [savedLater, setSavedLater] = useState(false);
  const [shareOpen, setShareOpen] = useState(false);
  const [saveOpen, setSaveOpen] = useState(false);
  const { seek } = useControls();
  const { user } = useAuth();
  const videoId = videoDetail?.detail?.id;
  const { data: likeInfo, refetch: refetchLike } = useGetLikeInfoQuery(
    { targetId: videoId ?? "", targetType: "VIDEO", userId: user?.sub ?? "" },
    { skip: !videoId || !user?.sub }
  );
  const [toggleLike] = useToggleLikeMutation();
  const [toggleDislike] = useToggleDislikeMutation();

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

  const handleToggleLike = async () => {
    if (!user?.sub) {
      toast.error("Hãy đăng nhập để thích video");
      return;
    }
    try {
      await toggleLike({ targetId: videoDetail.detail.id, targetType: "VIDEO", userId: user.sub }).unwrap();
    } catch (err) {
      console.error("Failed to toggle like:", err);
    }
  };

  const handleToggleDislike = async () => {
    if (!user?.sub) {
      toast.error("Hãy đăng nhập để đánh giá video");
      return;
    }
    try {
      await toggleDislike(videoDetail.detail.id).unwrap();
      await refetchLike();
    } catch (err) {
      console.error("Failed to toggle dislike:", err);
    }
  };

  const handleShare = async () => {
    const isMobile = typeof window !== "undefined" && window.matchMedia("(max-width: 767px)").matches;
    if (isMobile && typeof navigator !== "undefined" && "share" in navigator) {
      try {
        await navigator.share({
          title,
          url: `${window.location.origin}/watch/${videoDetail.detail.id}`,
        });
        return;
      } catch (err) {
        // Người dùng đóng bảng chia sẻ của hệ thống: không mở thêm hộp thoại
        if (err instanceof DOMException && err.name === "AbortError") return;
      }
    }
    setShareOpen(true);
  };

  const handleWatchLater = async () => {
    if (!user?.sub) return;
    const add = !savedLater;
    setSavedLater(add);
    try {
      if (add) await playlistService.addToSystemPlaylist(user.sub, videoDetail.detail.id, "WATCH_LATER");
      else await playlistService.removeFromSystemPlaylist(user.sub, videoDetail.detail.id, "WATCH_LATER");
    } catch (err) {
      console.error("Failed to update watch later:", err);
      setSavedLater(!add);
    }
  };

  const handleSubscribe = async () => {
    if (!user?.sub) return;
    try {
      if (!isSubscribed) {
        await subscribe({
          subscriberId: user.sub,
          channelId,
          tierId: undefined,
        }).unwrap();
        setIsSubscribed(true);
      } else {
        await unsubscribe({
          subscriberId: user.sub,
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
              {isSubscribed ? "Đã đăng ký" : "Đăng ký"}
            </p>
            {isSubscribed && <ChevronDownIcon className="size-4.5" />}
          </button>
        </section>

        <section className="flex justify-end items-center gap-3 ">
          <div className="cursor-pointer flex justify-between bg-gray-100 rounded-full overflow-hidden">
            <button
              onClick={handleToggleLike}
              className="flex gap-2 py-2 px-3 bg-gray-100 hover:bg-gray-200 rounded-l-full cursor-pointer"
            >
              {likeInfo?.liked ? (
                <HandThumbUpSolid className="size-5" />
              ) : (
                <HandThumbUpIcon className="size-5" />
              )}
              <p className="text-sm font-bold">{likeInfo?.likeCount ?? videoDetail.detail.like?.likeCount ?? 0}</p>
            </button>
            <div className="border-l-[1px] border-gray-300 my-2" />
            <button
              onClick={handleToggleDislike}
              aria-label="Không thích"
              className="py-2 px-3 bg-gray-100 hover:bg-gray-200 rounded-r-full cursor-pointer"
            >
              {likeInfo?.disliked ? (
                <HandThumbDownSolid className="size-5" />
              ) : (
                <HandThumbDownIcon className="size-5" />
              )}
            </button>
          </div>
          <button
            onClick={handleWatchLater}
            className="bg-gray-100 hover:bg-gray-200 p-2 rounded-full cursor-pointer flex gap-2 py-2 px-2.5"
          >
            <p className="text-sm font-bold">{savedLater ? "Đã lưu xem sau" : "Xem sau"}</p>
          </button>
          <button
            onClick={() => setSaveOpen(true)}
            className="bg-gray-100 hover:bg-gray-200 p-2 rounded-full cursor-pointer flex gap-2 py-2 px-2.5"
          >
            <p className="text-sm font-bold">Lưu</p>
          </button>
          <button
            onClick={handleShare}
            className="bg-gray-100 hover:bg-gray-200 p-2 rounded-full cursor-pointer flex gap-2 py-2 px-2.5"
          >
            <p className="text-sm font-bold">Chia sẻ</p>
          </button>
          <VideoMoreMenu videoId={videoDetail.detail.id} channelId={channelId} />
            {/*<TranscriptModal videoId={videoDetail.detail.id} />*/}
        </section>
      </div>

      <ShareDialog
        videoId={videoDetail.detail.id}
        title={title}
        currentTime={seek}
        open={shareOpen}
        onOpenChange={setShareOpen}
      />
      <SaveToPlaylistDialog
        videoId={videoDetail.detail.id}
        open={saveOpen}
        onOpenChange={setSaveOpen}
      />

      {/* Description */}
      <div className="bg-gray-100 p-3 mt-2 w-full rounded-xl text-sm">
        <p className="font-semibold">
          {totalView.toLocaleString("vi-VN")} lượt xem · {timeAgo(createAt)}
        </p>
        {description && <LinkifiedText text={description} className="mt-1" />}
      </div>
    </section>
  );
}
