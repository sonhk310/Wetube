"use client";

import { useEffect, useState } from "react";
import { Tabs, TabsList, TabsTrigger, TabsContent } from "@/components/ui/tabs";
import { playlistService } from "../list/playlist-API";
import VideoItem from "./VideoItem";
import HistoryShorts from "./history-shorts";
import { useHistoryStore } from "@/store/zustand/useHistoryStore";
import { useWatchProgressStore } from "@/lib/watchProgress";
import { toastEmitter } from "@/lib/toastEmitter";
import { Skeleton } from "@/components/ui/skeleton";



export default function HistoryList({ userId }: { userId: string }) {
  const [loading, setLoading] = useState(true);
  const [videos, setVideos] = useState<any[]>([]);
  const query = useHistoryStore((s) => s.query);
  const clearedCount = useHistoryStore((s) => s.clearedCount);

  useEffect(() => {
    if (clearedCount > 0) setVideos([]);
  }, [clearedCount]);

  useEffect(() => {
    if (!userId) return;

    const fetchData = async () => {
      try {
        const data = await playlistService.getHistory(userId);
        setVideos(data);
      } catch (err) {
        console.error("Error loading playlist detail", err);
      } finally {
        setLoading(false);
      }
    };


    fetchData();
  }, [userId]);

  const handleRemove = async (videoId: string) => {
    try {
        await playlistService.removeFromHistory(videoId);
        setVideos((prev) => prev.filter((v) => v.videoId !== videoId));
        useWatchProgressStore.getState().setProgress(videoId, 0); // bỏ thanh đỏ trên thumbnail
    } catch (err) {
      toastEmitter.error((err as Error).message);
    }
  };


  if (loading) {
    return (
      <div className="max-w-4xl pl-12 pt-6">
        <h1 className="text-4xl font-bold mb-3 text-black">Lịch sử xem</h1>
        <div className="mt-8 space-y-3">
          {Array.from({ length: 4 }).map((_, i) => (
            <div key={i} className="flex gap-3 py-2">
              <Skeleton className="w-1/3 h-38 rounded-sm shrink-0" />
              <div className="flex-1 space-y-2">
                <Skeleton className="h-6 w-3/4" />
                <Skeleton className="h-4 w-1/3" />
                <Skeleton className="h-4 w-1/2" />
              </div>
            </div>
          ))}
        </div>
      </div>
    );
  }
  if (!videos.length) return <p>Chưa có video nào trong lịch sử</p>;

  const q = query.trim().toLowerCase();
  const matched = q
    ? videos.filter(
        (v) =>
          v.videoTitle?.toLowerCase().includes(q) ||
          v.channelName?.toLowerCase().includes(q)
      )
    : videos;
  if (!matched.length) return <p className="pl-12 pt-6">Không tìm thấy video nào khớp với &quot;{query.trim()}&quot;</p>;

  // Như YouTube: Shorts có hàng thẻ dọc riêng, video thường nằm trong danh sách bên dưới
  const shorts = matched.filter((v) => v.isShort === true);
  const regular = matched.filter((v) => v.isShort !== true);

  const tabs = [
    { label: "Tất cả", value: "all", count: matched.length },
    { label: "Video", value: "video", count: regular.length },
    { label: "Shorts", value: "shorts", count: shorts.length },
  ];

  return (
    <div className="max-w-4xl pl-12 pt-6 text-black">
      <h1 className="text-4xl font-bold mb-3">Lịch sử xem</h1>

      <Tabs defaultValue="all">
        <TabsList className="border-none gap-2 flex-wrap">
          {tabs
            .filter((tab) => tab.count > 0)
            .map((tab) => (
              <TabsTrigger
                key={tab.value}
                value={tab.value}
                className="bg-gray-200 hover:bg-gray-400 data-[state=active]:bg-black data-[state=active]:text-white rounded-md px-3 py-1.5 text-sm"
              >
                {tab.label}
              </TabsTrigger>
            ))}
        </TabsList>

        {tabs
          .filter((tab) => tab.count > 0)
          .map((tab) => (
            <TabsContent key={tab.value} value={tab.value} className="mt-8">
              {tab.value === "shorts" ? (
                <HistoryShorts shorts={shorts} onRemove={handleRemove} wrap showTitle={false} />
              ) : (
              <>
              {tab.value === "all" && <HistoryShorts shorts={shorts} onRemove={handleRemove} />}
              {(tab.value === "video" || shorts.length === 0) && (
                <h2 className="text-xl font-bold mb-6">{tab.label}</h2>
              )}
              <div className="space-y-3">
                {regular.map((v) => (
                  <VideoItem
                    key={v.videoId}
                    videoId={v.videoId}
                    videoTitle={v.videoTitle}
                    videoUrl={`/watch/${v.videoId}`}
                    historyDuration={parseInt(v.historyDuration || "0", 10)}
                    channel={v.channelName || "Unknown"}
                    channelId={v.channelId}
                    totalView={`${v.totalView || 0} lượt xem`}
                    duration={v.duration}
                    thumbnailUrl={v.thumbnailUrl || "/images/thumbnail.png"}
                    description={v.description}
                    onRemove={handleRemove}
                    
                  />
                ))}
              </div>
              </>
              )}
            </TabsContent>
          ))}
      </Tabs>
    </div>
  );
}
