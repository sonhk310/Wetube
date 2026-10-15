"use client";
import React, { useState, useEffect, useRef, useMemo } from "react";
import { ThumbsUp, ThumbsDown, Share, MoreHorizontal } from "lucide-react";

interface VideoData {
  id: string;
  title: string;
  views: string;
  uploadTime: string;
  likes: string;
  description: string;
  videoUrl: string;
}

const SportsBanner = () => {
  const [currentVideoIndex, setCurrentVideoIndex] = useState(0);
  const videoRef = useRef<HTMLVideoElement>(null);

  const videos: VideoData[] = useMemo(
    () => [
      {
        id: "1",
        title: "2025 F1 Opening",
        views: "3,3 Tr lượt xem",
        uploadTime: "5 ngày trước",
        likes: "125K",
        description: "2025 F1 Opening",
        videoUrl:
          "https://firebasestorage.googleapis.com/v0/b/appxemphim-b758d.firebasestorage.app/o/New%202025%20F1%20Opening%20Titles.mp4?alt=media&token=750bcbd5-1c35-47c7-b6f3-45a3c83f9e64",
      },
      {
        id: "2",
        title: "PNC 2025 Trailer ",
        views: "2,1 Tr lượt xem",
        uploadTime: "1 tuần trước",
        likes: "98K",
        description: "PNC 2025 Trailer ",
        videoUrl:
          "https://firebasestorage.googleapis.com/v0/b/appxemphim-b758d.firebasestorage.app/o/PNC%202025%20Main%20Trailer%20-%20PUBG.mp4?alt=media&token=e1ee37e7-46eb-4723-aed4-90af4975c61a",
      },
      {
        id: "3",
        title: "Oklahoma City Thunder vs Indiana Pacers",
        views: "5,2 Tr lượt xem",
        uploadTime: "2 tuần trước",
        likes: "187K",
        description: "Oklahoma City Thunder vs Indiana Pacers",
        videoUrl:
          "https://firebasestorage.googleapis.com/v0/b/appxemphim-b758d.firebasestorage.app/o/Oklahoma%20City%20Thunder%20vs%20Indiana%20Pacers%20Full%20Game%206%20Highlights%20-%20June%2019%2C%202025%20-%202025%20NBA%20Finals.mp4?alt=media&token=e60ffebc-43de-4d0c-a975-0ca75034a95c",
      },
      {
        id: "4",
        title: "[TEASER] T1 vs GEN | CHUNG KẾT LCK",
        views: "1,8 Tr lượt xem",
        uploadTime: "3 tuần trước",
        likes: "76K",
        description: "[TEASER] T1 vs GEN | CHUNG KẾT LCK",
        videoUrl:
          "https://firebasestorage.googleapis.com/v0/b/appxemphim-b758d.firebasestorage.app/o/%5BTEASER%5D%20T1%20vs%20GEN%20-%20CHUNG%20K%E1%BA%BET%20LCK%20M%C3%99A%20XU%C3%82N%202022.mp4?alt=media&token=7b5de4ce-2c99-4e7d-91a0-b1ce54688d37",
      },
    ],
    []
  );

  const currentVideo = videos[currentVideoIndex];

  useEffect(() => {
    const video = videoRef.current;
    if (!video) return;

    video.currentTime = 0;
    const playPromise = video.play();

    if (playPromise !== undefined) {
      playPromise.catch((error) => {
        console.warn("Không thể phát video:", error.message);
      });
    }

    const timer = setTimeout(() => {
      setCurrentVideoIndex((prev) => (prev + 1) % videos.length);
    }, 20000);

    return () => clearTimeout(timer);
  }, [currentVideoIndex, videos]);

  return (
    <div className="relative w-full bg-black text-white rounded-2xl overflow-hidden shadow-xl">
      <div className="relative w-full  max-w-8xl h-[500px] rounded-xl overflow-hidden shadow-2xl">
        {/* Video background */}
        <video
          ref={videoRef}
          src={currentVideo.videoUrl}
          autoPlay
          muted
          playsInline
          className="absolute inset-0 w-full h-full object-cover object-center z-0 "
        />

        {/* Overlay */}
        <div className="absolute inset-0 bg-gradient-to-r from-black via-black/70 to-transparent z-10" />

        {/* Content */}
        <div className="absolute inset-0 p-6 md:p-10 flex flex-col justify-end text-white z-20">
          <div className="mb-auto">
            <p className="text-sm md:text-base font-medium text-gray-300 drop-shadow-md">
              {currentVideo.views} • {currentVideo.uploadTime}
            </p>
          </div>

          <h2 className="text-3xl font-extrabold mb-2 drop-shadow">
            {currentVideo.title}
          </h2>
          <p className="text-base mb-4 text-white/90">
            {currentVideo.description}
          </p>

          <div className="flex items-center gap-3 mb-4">
            <button className="flex items-center gap-1 px-3 py-2 rounded-full bg-white text-black hover:bg-gray-500 transition">
              <ThumbsUp className="w-4 h-4" />
              <span>{currentVideo.likes}</span>
            </button>
            <button className="p-2 rounded-full bg-white text-black hover:bg-gray-500 transition">
              <ThumbsDown className="w-4 h-4" />
            </button>
            <button className="p-2 rounded-full bg-white text-black hover:bg-gray-500 transition">
              <Share className="w-4 h-4" />
            </button>
            <button className="p-2 rounded-full bg-white text-black hover:bg-gray-500 transition">
              <MoreHorizontal className="w-4 h-4" />
            </button>
          </div>

          {/* Video switch buttons */}
          <div className="flex flex-wrap gap-2">
            {videos.map((video, index) => (
              <button
                key={video.id}
                onClick={() => setCurrentVideoIndex(index)}
                className={`w-20 h-10 rounded-xl border font-semibold text-sm transition-all ${
                  index === currentVideoIndex
                    ? "ring-2 ring-red-500 scale-105"
                    : "opacity-70 hover:opacity-100"
                } bg-neutral-800 text-white`}
              >
                {video.title.slice(0, 2)}
              </button>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};

export default SportsBanner;
