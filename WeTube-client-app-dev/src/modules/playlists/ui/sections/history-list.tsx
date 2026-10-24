"use client";

import { useEffect, useState } from "react";
import { Tabs, TabsList, TabsTrigger, TabsContent } from "@/components/ui/tabs";
import { playlistService } from "../list/playlist-API";
import VideoItem from "./VideoItem";
import { useHistoryStore } from "@/store/zustand/useHistoryStore";
import { useWatchProgressStore } from "@/lib/watchProgress";
import { toastEmitter } from "@/lib/toastEmitter";



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


  if (loading) return <p>Đang tải lịch sử...</p>;
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

  const tabs = [
    { label: "Tất cả", value: "all", type: null },
    { label: "Video", value: "video", type: "VIDEO" },
    { label: "Podcast", value: "podcast", type: "PODCAST" },
    { label: "Âm nhạc", value: "music", type: "MUSIC" },
  ];

  const getVideosForTab = (tab: { value: string; type: string | null }) => {
    if (!tab.type) return matched;
    return matched.filter((v) => v.type === tab.type);
  };

  return (
    <div className="max-w-4xl pl-12 pt-6 text-black">
      <h1 className="text-4xl font-bold mb-3">Lịch sử xem</h1>

      <Tabs defaultValue="all">
        <TabsList className="border-none gap-2 flex-wrap">
          {tabs
            .filter((tab) => getVideosForTab(tab).length > 0)
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
          .filter((tab) => getVideosForTab(tab).length > 0)
          .map((tab) => (
            <TabsContent key={tab.value} value={tab.value} className="mt-8">
              <h2 className="text-xl font-bold mb-6">{tab.label}</h2>
              <div className="space-y-3">
                {getVideosForTab(tab).map((v) => (
                  <VideoItem
                    key={v.videoId}
                    videoId={v.videoId}
                    videoTitle={v.videoTitle}
                    videoUrl={`/watch/${v.videoId}`}
                    historyDuration={parseInt(v.historyDuration || "0", 10)}
                    channel={v.channelName || "Unknown"}
                    totalView={`${v.totalViews || 0} lượt xem`}
                    duration={v.duration}
                    thumbnailUrl={v.thumbnailUrl || "/images/thumbnail.png"}
                    description={v.description}
                    onRemove={handleRemove}
                    
                  />
                ))}
              </div>
            </TabsContent>
          ))}
      </Tabs>
    </div>
  );
}
