"use client";
import { Play, Pause, Users } from "lucide-react";
import { RefObject, useState } from "react";

export default function VideoControls({
  videoRef,
}: {
  videoRef: RefObject<HTMLVideoElement>;
}) {
  const [isPlaying, setPlaying] = useState(false);

  const togglePlay = () => {
    if (!videoRef.current) return;
    if (isPlaying) {
      videoRef.current.pause();
    } else {
      videoRef.current.play();
    }
    setPlaying(!isPlaying);
  };

  return (
    <div className="flex items-center gap-2 bg-neutral-800 p-2 rounded-xl">
      <button onClick={togglePlay}>
        {isPlaying ? <Pause size={20} /> : <Play size={20} />}
      </button>
      <Users size={20} />
    </div>
  );
}
