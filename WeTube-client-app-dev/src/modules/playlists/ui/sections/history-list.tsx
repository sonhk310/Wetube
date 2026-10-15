"use client";

import { useEffect, useState } from "react";
import { Tabs, TabsList, TabsTrigger, TabsContent } from "@/components/ui/tabs";
import { playlistService } from "../list/playlist-API";
import VideoItem from "./VideoItem";



export default function HistoryList({ userId }: { userId: string }) {
  const [loading, setLoading] = useState(true);
  const [playlistId, setPlaylistId] = useState<string | null>(null);
  const [videos, setVideos] = useState<any[]>([]);
  
  useEffect(() => {
    if (!userId) return;

    const fetchData = async () => {
      try {
        const data = await playlistService.getHistory(userId);
        setVideos(data);
        setPlaylistId(data.playlistId || "history");
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
      if (!playlistId) return;
        await playlistService.removeVideo(playlistId, videoId);
        setVideos((prev) => prev.filter((v) => v.videoId !== videoId));
    } catch (err) {
      console.error("Lỗi khi xoá video:", err);
    }
  };


  if (loading) return <p>Đang tải lịch sử...</p>;
  if (!videos.length) return <p>Chưa có video nào trong lịch sử</p>;

  const tabs = [
    { label: "All", value: "all", type: null },
    { label: "Video", value: "video", type: "VIDEO" },
    { label: "Podcast", value: "podcast", type: "PODCAST" },
    { label: "Music", value: "music", type: "MUSIC" },
  ];

  const getVideosForTab = (tab: { value: string; type: string | null }) => {
    if (!tab.type) return videos;
    return videos.filter((v) => v.type === tab.type);
  };

  return (
    <div className="max-w-4xl pl-12 pt-6 text-black">
      <h1 className="text-4xl font-bold mb-3">Watch History</h1>

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
                    channel={v.Name || "Unknown"}
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
