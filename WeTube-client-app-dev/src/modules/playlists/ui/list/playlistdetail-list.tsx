"use client";

import { useEffect, useRef, useState } from "react";
import { MoreVertical, PlayCircle, Shuffle } from "lucide-react";
import { playlistService } from "./playlist-API";
import ColorThief from "colorthief";
import Image from 'next/image';

export default function PlaylistDetail({ playlistId }: { playlistId: string }) {
  const [playlist, setPlaylist] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [bgColor, setBgColor] = useState<string>("rgb(107,114,128)"); 
  const imgRef = useRef<HTMLImageElement>(null);

  useEffect(() => {
    if (!playlistId) return;
    const fetchData = async () => {
      try {
        const data = await playlistService.getDetail(playlistId);
        setPlaylist(data);
      } catch (err) {
        console.error("Error loading playlist detail", err);
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, [playlistId]);

  useEffect(() => {
    if (!playlist?.videos?.length) return;
    if (imgRef.current) {
      const colorThief = new ColorThief();
      imgRef.current.crossOrigin = "anonymous";
      imgRef.current.onload = () => {
        try {
          const color = colorThief.getColor(imgRef.current!);
          setBgColor(`rgb(${color[0]}, ${color[1]}, ${color[2]})`);
        } catch (e) {
          console.error("ColorThief error:", e);
        }
      };
    }
  }, [playlist]);

  if (loading) return <p>Loading...</p>;
  if (!playlist) return <p>Không tìm thấy playlist</p>;

  const firstVideo = playlist.videos?.[0];

  return (
    <div className="w-full flex flex-col md:flex-row bg-white">

      <div className="w-full md:w-96 md:sticky md:top-0 md:h-full p-6">
        <div
          className="rounded-lg p-6 text-white flex flex-col h-full"
          style={{ backgroundColor: bgColor }}
        >
          <Image
            ref={imgRef}
            src={firstVideo?.thumbnailUrl || "/api/placeholder/300/200"}
            alt="Playlist"
            width={300}
            height={200}
            priority
            className="w-full h-48 object-cover rounded mb-4"
          />
          <h1 className="text-2xl font-bold mb-2">
            {playlist.playlistTitle}
          </h1>
          <p className="text-sm text-white/80 mb-4">
            {playlist.videos?.length || 0} video • Cập nhật{" "}
            {new Date(playlist.createdAt).toLocaleDateString("vi-VN")}
          </p>

          <div className="flex gap-2 mb-4">
            <button className="flex items-center gap-2 bg-white text-black px-4 py-2 rounded-full font-medium hover:bg-gray-100">
              <PlayCircle size={16} />
              Phát tất cả
            </button>
            <button className="flex items-center gap-2 border border-white/30 px-4 py-2 rounded-full hover:bg-white/10">
              <Shuffle size={16} />
              Trộn bài
            </button>
          </div>
        </div>
      </div>

      <div className="flex-1 overflow-y-auto space-y-4 p-6 md:h-screen">
        {playlist.videos?.map((video: any, index: number) => (
          <div
            key={video.videoId}
            className="flex gap-4 hover:bg-gray-50 p-2 rounded-lg"
            
          >
            <div className="text-sm text-gray-500 w-8 flex-shrink-0 pt-2">
              {index + 1}
            </div>
            <div className="relative flex-shrink-0">
              <Image
                src={video.thumbnailUrl || "/api/placeholder/168/94"}
                alt={video.videoTitle}
                width={168}
                height={94}
                className="w-42 h-24 object-cover rounded-lg"
              />
              <span className="absolute bottom-2 right-2 bg-black/80 text-white text-xs px-1 rounded">
                {video.duration || "0:00"}
                
              </span>
            </div>
            <div className="flex-1 min-w-0">
              <h3 className="font-medium text-gray-900 mb-1 line-clamp-2">
                {video.videoTitle}
              </h3>
              <div className="text-sm text-gray-600 space-y-1">
                <p>{video.channel || "Unknown Channel"}</p>
                <p>{video.totalView || 0} lượt xem</p>
              </div>
            </div>
            <div className="flex-shrink-0">
              <button className="p-2 hover:bg-gray-200 rounded-full">
                <MoreVertical size={16} />
              </button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
