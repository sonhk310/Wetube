"use client";
import React, { useState, useEffect, useRef, useMemo } from "react";
import { ThumbsUp, ThumbsDown, Share, MoreHorizontal } from "lucide-react";

interface VideoData {
  id: string;
  title: string;
  artist: string;
  views: string;
  uploadTime: string;
  likes: string;
  description: string;
  videoUrl: string;
}

const MusicBanner = () => {
  const [currentVideoIndex, setCurrentVideoIndex] = useState(0);
  const videoRef = useRef<HTMLVideoElement>(null);

  const videos: VideoData[] = useMemo(
    () => [
      {
        id: "1",
        title: "AS IF IT'S YOUR LAST",
        artist: "BlackPink",
        views: "3,3 Tr lượt xem",
        uploadTime: "5 ngày trước",
        likes: "125K",
        description: "BLACKPINK - '마지막처럼 (AS IF IT'S YOUR LAST)' M/V",
        videoUrl:
          "https://firebasestorage.googleapis.com/v0/b/appxemphim-b758d.firebasestorage.app/o/BLACKPINK%20-%20'%EB%A7%88%EC%A7%80%EB%A7%89%EC%B2%98%EB%9F%BC%20(AS%20IF%20IT'S%20YOUR%20LAST)'%20M-V.mp4?alt=media&token=c31b1edf-d50b-40fa-a821-6bdbb3297703",
      },
      {
        id: "2",
        title: "PLAYING WITH FIRE",
        artist: "BlackPink",
        views: "2,1 Tr lượt xem",
        uploadTime: "1 tuần trước",
        likes: "98K",
        description: "BLACKPINK - '불장난 (PLAYING WITH FIRE)' M/V",
        videoUrl:
          "https://firebasestorage.googleapis.com/v0/b/appxemphim-b758d.firebasestorage.app/o/BLACKPINK%20-%20'%EB%B6%88%EC%9E%A5%EB%82%9C%20(PLAYING%20WITH%20FIRE)'%20M-V.mp4?alt=media&token=b0cbbe49-bf2e-413e-83be-077748531dfd",
      },
      {
        id: "3",
        title: "Pink Venom",
        artist: "BlackPink",
        views: "5,2 Tr lượt xem",
        uploadTime: "2 tuần trước",
        likes: "187K",
        description: "BLACKPINK - ‘Pink Venom’ M/V",
        videoUrl:
          "https://firebasestorage.googleapis.com/v0/b/appxemphim-b758d.firebasestorage.app/o/BLACKPINK%20-%20Pink%20Venom%20M-V.mp4?alt=media&token=610d64d2-7545-4410-aa25-c345d6b28d4c",
      },
      {
        id: "4",
        title: "JUMP",
        artist: "BlackPink",
        views: "1,8 Tr lượt xem",
        uploadTime: "3 tuần trước",
        likes: "76K",
        description: "BLACKPINK - ‘뛰어(JUMP)’ M/V",
        videoUrl:
          "https://firebasestorage.googleapis.com/v0/b/appxemphim-b758d.firebasestorage.app/o/BLACKPINK%20-%20%EB%9B%B0%EC%96%B4(JUMP)%20M-V.mp4?alt=media&token=86616595-832c-485a-8c2d-43e1ef36bd1c",
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
    }, 10000);

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
              {currentVideo.artist} • {currentVideo.views} •{" "}
              {currentVideo.uploadTime}
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

export default MusicBanner;
