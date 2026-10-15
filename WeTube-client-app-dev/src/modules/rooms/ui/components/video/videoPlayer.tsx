"use client";

import { useRoomStore } from "@/store/zustand/useRoomStore";
import { useStompStore } from "@/store/zustand/useStompStore";
import { MediaPlayerState } from "@/types/room";
import { VideoOff } from "lucide-react";
import { useEffect, useRef, useState } from "react";

interface VideoPlayerProps {
  onChangeVideo?: (id: string) => void;
}

export default function VideoPlayer({ onChangeVideo }: VideoPlayerProps) {
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const [currentIndex, setCurrentIndex] = useState(0);
  const { room, setMediaState, host } = useRoomStore();
  const { publish, subscribe } = useStompStore();
  const videos = room?.playlist ?? [];
  const currentVideo = room?.playerState.currentSongId || "";
  const mediaState = room?.playerState;
  const currentTimeInSeconds = mediaState!.currentTimeMillis / 1000 || 0;
  const [isPlaying, setIsPlaying] = useState(false);

  useEffect(() => {
    // Kiểm tra nếu videoRef và currentSongId đã có giá trị
    if (videoRef.current && mediaState?.currentSongId) {
      const videoUrl = mediaState.currentSongId;
      const video = videos.find((v) => v.videoUrl === videoUrl);

      // Log để kiểm tra giá trị của video và mediaState
      console.log("mediaState received:", mediaState);
      console.log("currentVideoUrl:", videoUrl);
      console.log("Found video:", video);

      if (video && videoRef.current) {
        // Cập nhật nguồn video khi có video mới
        console.log("Updating video src:", video.videoUrl);
        videoRef.current.src = video.videoUrl;

        // Thiết lập lại thời gian video khi video đổi
        console.log("Setting currentTime to:", currentTimeInSeconds);
        videoRef.current.currentTime = currentTimeInSeconds;

        // Reload video để cập nhật nguồn mới
        videoRef.current.load();
        console.log("Video loaded successfully");
        console.log("mediaState.isPlaying:", mediaState.playing);

        // Kiểm tra trạng thái "playing" để phát hoặc tạm dừng video
        if (mediaState.playing) {
          console.log("Playing video...");
          videoRef.current.play().catch((err) => {
            console.log("Error while playing video:", err);
          });
        } else {
          console.log("Pausing video...");
          videoRef.current.pause();
        }
      } else {
        console.log("No video found with the given URL or videoRef is null.");
      }
    } else {
      console.log("No videoRef or currentSongId available.");
    }
  }, [mediaState]);

  const handlePause = (isPlaying: boolean) => {
    if (videoRef.current) {
      const newState: MediaPlayerState = {
        ...mediaState!,
        playing: isPlaying,
        currentTimeMillis: videoRef.current.currentTime * 1000,
      };

      if (host) {
        publishMediaState(newState); // Gửi lên server
      }
    }
  };

  const publishSeek = () => {
    if (videoRef.current) {
      const newState: MediaPlayerState = {
        ...mediaState!,
        playing: isPlaying, // Giả sử video đang chơi khi seek
        currentTimeMillis: videoRef.current.currentTime * 1000, // Đổi sang milliseconds
      };
      console.log("Is HOst??????????:", host);

      if (host) {
        publishMediaState(newState); // Gửi lên server
      }
    }
  };

  const publishMediaState = (mediaState: MediaPlayerState) => {
    if (!room) return;
    const publishEndpoint = `/app/room/mediaState/${room.roomId}`;
    publish(publishEndpoint, mediaState);
  };

  const handleEnded = () => {
    if (currentIndex < videos.length - 1) {
      setCurrentIndex(currentIndex + 1);
    } else {
      setCurrentIndex(0); // lặp lại từ đầu
    }
  };

  if (videos.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center w-full h-full text-gray-500">
        <VideoOff className="w-12 h-12 mb-4" /> {/* Icon Lucide */}
        <p className="text-lg text-center">
          Search and add a video to start the party 🎉
        </p>
      </div>
    );
  }

  return (
    <video
      ref={videoRef}
      key={currentVideo}
      src={currentVideo}
      controls
      autoPlay={true}
      className="w-full h-full rounded-lg"
      onPlay={() => {
        setIsPlaying(true);
        handlePause(true);
        // console.log("▶️ Video started playing");
        // if (host) {
        //   publish(`/app/media/${room?.roomId}`, {
        //     type: "PLAY",
        //     timestamp: videoRef.current?.currentTime ?? 0,
        //     sender: myUsername,
        //   });
        // }
      }}
      onEnded={handleEnded}
      onPause={() => {
        setIsPlaying(false);
        handlePause(false);
      }}
      onSeeked={host ? publishSeek : undefined}
    />
  );
}
