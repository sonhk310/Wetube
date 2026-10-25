"use client";

import Image from "next/image";
import Link from "next/link";
import {
  BellIcon,
  BookmarkIcon,
  ChevronDownIcon,
  ClockIcon,
  HandThumbDownIcon,
  HandThumbUpIcon,
  ShareIcon,
} from "@heroicons/react/24/outline";
import {
  ClockIcon as ClockSolid,
  HandThumbDownIcon as HandThumbDownSolid,
  HandThumbUpIcon as HandThumbUpSolid,
} from "@heroicons/react/24/solid";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";
import { useVideoStore } from "@/store/zustand/videoStore";
import { formatCompact, timeAgo } from "@/lib/utils";
import { type ReactNode, useCallback, useEffect, useState } from "react";
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
import { toast } from "sonner";
import { playlistService } from "@/modules/playlists/ui/list/playlist-API";

/** Nút tròn chỉ có icon, rê chuột hiện tên. */
function IconAction({ label, onClick, children }: { label: string; onClick: () => void; children: ReactNode }) {
  return (
    <Tooltip>
      <TooltipTrigger asChild>
        <button
          onClick={onClick}
          aria-label={label}
          className="bg-gray-100 hover:bg-gray-200 rounded-full cursor-pointer p-2"
        >
          {children}
        </button>
      </TooltipTrigger>
      <TooltipContent>{label}</TooltipContent>
    </Tooltip>
  );
}

export default function VideoMetadata() {
  const videoDetail = useVideoStore((state) => state.videoDetail);
  const [isSubscribed, setIsSubscribed] = useState(false);
  // Số người đăng ký đổi ngay khi bấm Đăng ký / Huỷ (server chỉ trả số lúc tải trang)
  const [subscriberDelta, setSubscriberDelta] = useState(0);
  const [subscribing, setSubscribing] = useState(false);
  const [savedLater, setSavedLater] = useState(false);
  const [shareOpen, setShareOpen] = useState(false);
  const [saveOpen, setSaveOpen] = useState(false);
  const { seek } = useControls();
  const { user } = useAuth();
  const videoId = videoDetail?.detail?.id;
  const { data: likeInfo, refetch: refetchLike } = useGetLikeInfoQuery(
    { targetId: videoId ?? "", targetType: "VIDEO", userId: user?.sub ?? "" },
    // Số lượt thích tự cập nhật khi quay lại tab và mỗi 30 giây (khi tab đang mở)
    { skip: !videoId || !user?.sub, refetchOnFocus: true, pollingInterval: 30_000, skipPollingIfUnfocused: true }
  );
  const [toggleLike] = useToggleLikeMutation();
  const [toggleDislike] = useToggleDislikeMutation();

  useEffect(() => {
    if (videoDetail?.detail?.subscribed !== undefined) {
      setIsSubscribed(videoDetail.detail.subscribed);
    }
    setSubscriberDelta(0);
  }, [videoDetail]);

  const [subscribe] = useSubscribeMutation();
  const [unsubscribe] = useUnsubscribeMutation();

  // Video đã nằm trong Xem sau chưa: kiểm lúc mở trang và sau khi đóng hộp thoại Lưu
  const userId = user?.sub;
  const checkSavedLater = useCallback(async () => {
    if (!userId || !videoId) {
      setSavedLater(false);
      return;
    }
    try {
      const [watchLater, containing] = await Promise.all([
        playlistService.getByUserAndType(userId, "WATCH_LATER") as Promise<{ playlistId: string }[]>,
        playlistService.getContaining(videoId),
      ]);
      setSavedLater(watchLater.some((p) => containing.includes(p.playlistId)));
    } catch {
      setSavedLater(false);
    }
  }, [userId, videoId]);

  useEffect(() => {
    void checkSavedLater();
  }, [checkSavedLater]);

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
    if (!user?.sub) {
      toast.error("Hãy đăng nhập để lưu video");
      return;
    }
    const add = !savedLater;
    setSavedLater(add);
    try {
      if (add) await playlistService.addToSystemPlaylist(user.sub, videoDetail.detail.id, "WATCH_LATER");
      else await playlistService.removeFromSystemPlaylist(user.sub, videoDetail.detail.id, "WATCH_LATER");
      toast.success(add ? "Đã lưu vào Xem sau" : "Đã xoá khỏi Xem sau");
    } catch (err) {
      console.error("Failed to update watch later:", err);
      setSavedLater(!add);
      toast.error(add ? "Không lưu được vào Xem sau" : "Không gỡ được khỏi Xem sau");
    }
  };

  const isOwnChannel = !!user?.channelId && user.channelId === channelId;

  const handleSubscribe = async () => {
    if (!user?.sub) {
      toast.error("Hãy đăng nhập để đăng ký kênh");
      return;
    }
    if (subscribing) return;
    setSubscribing(true);
    try {
      if (!isSubscribed) {
        await subscribe({
          subscriberId: user.sub,
          channelId,
          tierId: undefined,
        }).unwrap();
        setIsSubscribed(true);
        setSubscriberDelta((d) => d + 1);
        toast.success("Đã đăng ký kênh");
      } else {
        await unsubscribe({
          subscriberId: user.sub,
          channelId,
        }).unwrap();
        setIsSubscribed(false);
        setSubscriberDelta((d) => d - 1);
        toast.success("Đã huỷ đăng ký");
      }
    } catch (err) {
      console.error("Failed to update subscription:", err);
      toast.error("Không cập nhật được đăng ký, vui lòng thử lại");
    } finally {
      setSubscribing(false);
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
                {formatCompact(Math.max(0, (totalSubscribers ?? 0) + subscriberDelta))} người đăng ký
              </p>
            </section>
          </div>
          {isOwnChannel ? (
            // Video của chính mình: YouTube hiện nút quản lý thay cho Đăng ký
            <Link
              href={`/studio/videos/${videoDetail.detail.id}`}
              className="rounded-full bg-gray-100 hover:bg-gray-200 py-2 px-3 text-sm font-bold"
            >
              Chỉnh sửa video
            </Link>
          ) : (
            <button
              onClick={handleSubscribe}
              disabled={subscribing}
              className={`rounded-full flex gap-2 py-2 px-3 cursor-pointer disabled:opacity-70 ${
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
          )}
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
          <IconAction label={savedLater ? "Đã lưu vào Xem sau" : "Xem sau"} onClick={handleWatchLater}>
            {savedLater ? <ClockSolid className="size-5" /> : <ClockIcon className="size-5" />}
          </IconAction>
          <IconAction label="Lưu" onClick={() => setSaveOpen(true)}>
            <BookmarkIcon className="size-5" />
          </IconAction>
          <IconAction label="Chia sẻ" onClick={handleShare}>
            <ShareIcon className="size-5" />
          </IconAction>
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
        onOpenChange={(next) => {
          setSaveOpen(next);
          // Hộp thoại có hàng Xem sau: đóng lại thì cập nhật icon Xem sau
          if (!next) void checkSavedLater();
        }}
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
