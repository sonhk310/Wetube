import { useContext } from "react";
import { RelatedVideosContext } from "@/contexts/related-videos-context";


export const useVideos = () => {
  const ctx = useContext(RelatedVideosContext);

  if (!ctx) {
    throw new Error("useVideos must be used within a VideosProvider");
  }

  return ctx;
};
