"use client";

import { useControls } from "@/hooks/use-controls";
import { useEffect, useState } from "react";
import ReactPlayer from "react-player";
import { Loader } from "../misc/loader";
import { OnProgressProps } from "react-player/base";
import { useVideoStore } from "@/store/zustand/videoStore";

export default function Player() {
  const [isClient, setIsClient] = useState(false);
  const videoDetail = useVideoStore((s) => s.videoDetail?.detail);
  const videoUrl = videoDetail?.videoUrl;
  const subtitleUrl = videoDetail?.subtitles?.[0]?.subtitleUrl;

  const {
    isPlaying,
    pauseVideo,
    setPercentage,
    setTotalSeek,
    volume,
    setLoaded,
  } = useControls();

  useEffect(() => setIsClient(true), []);

  if (!isClient || !videoUrl) {
    return (
      <div className="absolute top-0 left-0 h-full w-full bg-black flex justify-center items-center">
        <Loader />
      </div>
    );
  }

  return (
    <ReactPlayer
      playing={isPlaying}
      volume={volume / 100}
      controls={true} 
      url={videoUrl}
      height="100%"
      width="100%"
      progressInterval={200}
      onProgress={(state: OnProgressProps) => {
        setPercentage(Math.min(100, state.played * 100));
        setLoaded(Math.min(100, state.loaded * 100));
      }}
      onReady={(player) => setTotalSeek(player.getDuration())}
      onEnded={() => pauseVideo()}
      config={{
        file: {
          attributes: { crossOrigin: "anonymous" }, // cần cho subtitle
          tracks: subtitleUrl
            ? [
                {
                  kind: "subtitles",
                  src: subtitleUrl,
                  srcLang: "vi",
                  label: "Tiếng Việt",
                  default: true,
                },
              ]
            : [],
        },
      }}
    />
  );
}
