"use client";

import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import VideoOverlay from "@/components/videos/VideoOverlayProps";
import { Check, Clock, ListPlus, MoreVertical, X } from "lucide-react";
import Image from "next/image";
import React, { useState } from "react";
import { useAuth } from "@/contexts/auth-context";
import { playlistService } from "../list/playlist-API";
import { useRouter } from "next/navigation";

interface VideoItemProps {
  videoId: string;
  videoTitle: string;
  videoUrl: string;
  channel?: string;
  totalView?: string;
  duration?: number | string;
  description?: string;
  thumbnailUrl?: string;
  historyDuration?: number;
  onAddedToHistory?: () => void;
  onRemove?: (videoId: string) => void;
}

export default function VideoItem({
  videoId,
  videoTitle,
  videoUrl,
  channel,
  description,
  totalView = "0 lượt xem",
  duration,
  thumbnailUrl,
  historyDuration = 0,
  onAddedToHistory,
  onRemove,
}: VideoItemProps) {
  const [watchLaterClicked, setWatchLaterClicked] = useState(false);
  const [addedPlaylistClicked, setAddedPlaylistClicked] = useState(false);
  const { user } = useAuth();
  const router = useRouter();

  const resetState = () => {
    setWatchLaterClicked(false);
    setAddedPlaylistClicked(false);
  };

  // format duration từ giây -> mm:ss
  const formatDuration = (sec: number | string) => {
    const total = typeof sec === "string" ? parseInt(sec, 10) : sec;
    const m = Math.floor(total / 60);
    const s = Math.floor(total % 60);
    return `${m}:${s.toString().padStart(2, "0")}`;
  };

  const handleClickVideo = async () => {
    if (!user?.sub) {
      console.warn("User chưa login, redirect tới login");
      router.push("/login");
      return;
    }

    try {
      await playlistService.addToHistory(user.sub, videoId);
      console.log("Đã lưu history:", videoId);

      if (onAddedToHistory) onAddedToHistory();

      router.push(`/watch/${videoId}`);
    } catch (err) {
      console.error("Lỗi khi lưu history:", err);
      router.push(`/watch/${videoId}`); 
    }
  };

  return (
    <Card
      onMouseLeave={resetState}
      className="bg-transparent border-none shadow-none py-2"
    >
      <CardContent className="p-0">
        <div className="flex gap-3 group">
          {/* Thumbnail */}
          <div
            className="relative w-1/3 h-38 overflow-hidden rounded-sm flex-shrink-0 cursor-pointer"
            onClick={handleClickVideo}
          >
            <Image
              src={thumbnailUrl || "/images/thumbnail.png"}
              alt={videoTitle}
              fill
              sizes="(max-width: 640px) 100vw, (max-width: 768px) 50vw, 30vw"
              className="object-cover"
            />

            <div className="absolute top-0 right-1 flex flex-col gap-1 transition-all duration-300 translate-y-0 opacity-0 group-hover:opacity-100 group-hover:translate-y-2">
              <button
                className="p-2 bg-black/70 rounded-full backdrop-blur-sm transition-all duration-200 hover:scale-110"
                onClick={(e) => {
                  e.stopPropagation();
                  setWatchLaterClicked(true);
                }}
                aria-label="Watch later"
              >
                {watchLaterClicked ? (
                  <Check className="w-4 h-4 text-green-400" />
                ) : (
                  <Clock className="w-4 h-4 text-white hover:text-blue-400" />
                )}
              </button>

              <button
                className="p-2 bg-black/70 rounded-full backdrop-blur-sm transition-all duration-200 hover:scale-110"
                onClick={(e) => {
                  e.stopPropagation();
                  setAddedPlaylistClicked(true);
                }}
                aria-label="Add to playlist"
              >
                {addedPlaylistClicked ? (
                  <Check className="w-4 h-4 text-green-400" />
                ) : (
                  <ListPlus className="w-4 h-4 text-white hover:text-blue-400" />
                )}
              </button>
            </div>

            <VideoOverlay
              duration={Number(duration)}
              progress={historyDuration}
            />
          </div>

          <div className="flex-1 cursor-pointer" onClick={handleClickVideo}>
            <h3 className="text-[20px] font-semibold text-black line-clamp-2 mb-1 group-hover:text-blue-600">
              {videoTitle}
            </h3>
            <p className="text-[14px] text-gray-600">{channel || "Unknown"} • {totalView} </p>
            
            {description && (
              <p className="text-[14px] text-gray-500 mt-1 line-clamp-2">
                {description}
              </p>
            )}
          </div>

          <Button
              variant="ghost"
              size="sm"
              className="h-8 w-8 p-0 text-black rounded-full hover:bg-gray-200"
              onClick={(e) => {
                e.stopPropagation();
                onRemove?.(videoId);
              }}
            >
              <X className="h-8 w-8 " />
            </Button>

          <div className="w-1/8">
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button
                  variant="ghost"
                  size="sm"
                  className="h-8 w-8 p-0 text-gray-600 hover:text-black hover:bg-gray-100"
                >
                  <MoreVertical className="h-4 w-4" />
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent
                align="end"
                className="bg-white border-gray-200 text-black"
              >
                <DropdownMenuItem 
                className="hover:bg-gray-100 cursor-pointer text-black"
                 onClick={(e) => {
                    e.stopPropagation();
                    onRemove?.(videoId);
                  }}
                >
                  Xóa khỏi nhật ký xem
                </DropdownMenuItem>
                <DropdownMenuItem className="hover:bg-gray-100 cursor-pointer">
                  Chia sẻ
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
