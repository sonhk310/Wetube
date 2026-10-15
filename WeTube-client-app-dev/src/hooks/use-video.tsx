import { VideoContext } from "@/contexts/video-context";
import { useContext } from "react";


export const useVideo = () => {
  const ctx = useContext(VideoContext);

  if (!ctx) {
    throw new Error("useVideo must be used within a VideoProvider");
  }

  return ctx;
};
