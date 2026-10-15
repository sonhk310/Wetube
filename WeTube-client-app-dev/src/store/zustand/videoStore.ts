import { VideoDetailResponseDto } from "@/types/video";
import {create} from "zustand";


interface VideoState {
    videoDetail: VideoDetailResponseDto | null;
    setVideoDetail: (video: VideoDetailResponseDto) => void;
    clearVideoDetail: () => void;
}

export const useVideoStore = create<VideoState>((set) => ({
    videoDetail: null,
    setVideoDetail: (video) => set({ videoDetail: video }),
    clearVideoDetail: () => set({ videoDetail: null }),
}));
