"use client";

import {
  Play,
  Pause,
  Maximize2,
  Volume2,
  VolumeX,
  Subtitles,
  Rewind,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import React from "react";
import { FastForward } from "@phosphor-icons/react";

interface ControlButtonsProps {
  isPlaying: boolean;
  playVideo: () => void;
  pauseVideo: () => void;
  volume: number;
  setVolume: (v: number) => void;
  isFullscreen: boolean;
  toggleFullscreen: () => void;
  showSubtitles: boolean;
  setShowSubtitles: React.Dispatch<React.SetStateAction<boolean>>;
  seekBy: (secondsDelta: number) => void; // new prop
}

export default function ControlButtons({
  isPlaying,
  playVideo,
  pauseVideo,
  volume,
  setVolume,
  isFullscreen,
  toggleFullscreen,
  showSubtitles,
  setShowSubtitles,
  seekBy,
}: ControlButtonsProps) {
  return (
    <div className="flex items-center justify-center gap-4 bg-black/50 rounded-full px-4 py-2">
      {/* Rewind 10s */}
      <Button
        variant="ghost"
        size="icon"
        onClick={() => seekBy(-10)}
        aria-label="Back 10 seconds"
        title="Back 10s"
        className="text-white hover:text-blue-400"
      >
        <Rewind size={20} />
      </Button>

      {/* Play / Pause */}
      <Button
        variant="ghost"
        size="icon"
        onClick={isPlaying ? pauseVideo : playVideo}
        className="text-white hover:text-blue-400"
      >
        {isPlaying ? <Pause size={22} /> : <Play size={22} />}
      </Button>

            <Button
        variant="ghost"
        size="icon"
        onClick={() => seekBy(10)}
        aria-label="Forward 10 seconds"
        title="Forward 10s"
        className="text-white hover:text-blue-400"
      >
        <FastForward size={22} />
      </Button>

      {/* Volume toggle */}
      <Button
        variant="ghost"
        size="icon"
        onClick={() => setVolume(volume > 0 ? 0 : 100)}
        className="text-white hover:text-blue-400"
        title={volume > 0 ? "Mute" : "Unmute"}
      >
        {volume > 0 ? <Volume2 size={22} /> : <VolumeX size={22} />}
      </Button>

      {/* Subtitles toggle */}
      <Button
        variant="ghost"
        size="icon"
        onClick={() => setShowSubtitles((prev) => !prev)}
        className="hover:text-blue-400"
        title={showSubtitles ? "Hide subtitles" : "Show subtitles"}
      >
        <Subtitles
          size={22}
          className={showSubtitles ? "text-blue-500" : "text-gray-300"}
        />
      </Button>

      {/* Fullscreen toggle */}
      <Button
        variant="ghost"
        size="icon"
        onClick={toggleFullscreen}
        className="text-white hover:text-blue-400"
        title="Fullscreen"
      >
        <Maximize2 size={22} />
      </Button>
    </div>
  );
}
